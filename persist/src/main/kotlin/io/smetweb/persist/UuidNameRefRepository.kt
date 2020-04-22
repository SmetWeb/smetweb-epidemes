package io.smetweb.persist

import io.smetweb.jpa.PersistentDao
import io.smetweb.log.getLogger
import io.smetweb.uuid.UuidNameRef
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.lang.IllegalStateException
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext
import javax.persistence.criteria.CriteriaBuilder
import javax.persistence.criteria.Predicate
import javax.persistence.criteria.Root

@Repository
@Transactional
open class UuidNameRefRepository: PersistentDao<UuidNameRef, UuidNameRef> {

	private val log = getLogger()

	@PersistenceContext
	private lateinit var em: EntityManager

	/**
	 * [matchCompositeKey] generates a predicate that matches EITHER by primary key (if any) OR the composite (join) values
	 */
	private fun matchCompositeKey(
			entity: UuidNameRefEntity,
			root: Root<UuidNameRefEntity>,
			cb: CriteriaBuilder = this.em.criteriaBuilder
	): Predicate {
		// compare by value, root/context ref, and immediate parent (if any)
		val parentPredicate = entity.parentRef?.let { parentRefDao ->
			cb.equal(root.get(UuidNameRefEntity_.parentRef), parentRefDao)
		} ?: cb.isNull(root.get(UuidNameRefEntity_.parentRef))

		val valueConjunction = cb.and(
				cb.equal(root.get(UuidNameRefEntity_.contextRef), entity.contextRef),
				cb.equal(root.get(UuidNameRefEntity_.value), entity.value),
				parentPredicate)

		// match by primary key first (if any), and then (or just) by value conjunction
		return entity.pk?.let { pk ->
			cb.or(cb.equal(root.get(UuidNameRefEntity_.pk), pk), valueConjunction)
		} ?: valueConjunction
	}

	/**
	 * [existsKeyOrValues] selects the COUNT of entries that [matchCompositeKey]
	 */
	private fun existsKeyOrValues(entity: UuidNameRefEntity): Boolean =
			PersistentDao.query(em, UuidNameRefEntity::class.java, Long::class.java)
			{ query, root ->
				query.select(this.em.criteriaBuilder.count(root.get(UuidNameRefEntity_.pk)))
						.where(matchCompositeKey(entity, root))
			}.singleResult > 0

	/**
	 * [existsKeyOrValues] selects the COUNT of entries that [matchCompositeKey]
	 */
	private fun findByCompositeKey(entity: UuidNameRefEntity): UuidNameRefEntity? =
			PersistentDao.findByCriteria(this.em, UuidNameRefEntity::class.java)
			{ query, root ->
				log.trace("Finding ${entity.contextRef} ${entity.value} ${entity.parentRef?.pk}")
				query.select(root).where(matchCompositeKey(entity, root))
			}.firstOrNull()

	/**
	 * [toEntity] recursively resolves the persisted parent refs, or returns NULL if parents are not persisted (yet)
	 */
	private fun UuidNameRef.toEntity(): UuidNameRefEntity? =
			this.getParentRef()?.let { parentRef ->
				// parent exists: this is the (grand)child of the root/context UUID
				log.trace("Finding parent for $this, recursively...")
				UuidNameRefEntity(
					nameRef = this,
					parentRefEntity = parentRef.getParentRef()?.let { grandParentRef ->
						// grand-parent exists: this is the child of another (grand)child of the root/context UUID
						log.trace("Grand-parent for $this found ($grandParentRef): recurse again...")
						parentRef.toEntity() ?.let { parentDao ->
							findByCompositeKey(parentDao) ?.let { mergedParentDao ->
								mergedParentDao
							} ?: let {
								log.trace("Parent ($parentRef) is not (yet) persisted either: recursion ends")
								return null
							}
						} ?: let {
							throw IllegalStateException("Grand-parent ($parentRef) is root/contextID only")
						}
					} ?: let {
						log.trace("No grand-parent, $this is direct child of root ($parentRef): recursion ends")
						null
					})
			} ?: let {
				throw IllegalStateException("$this is a root/contextID (orphan) and is not persisted by itself")
			}

	/**
	 * [findOrCreate] tries to [findByCompositeKey], or creates it, assuming its parentRefDao was merged already
	 */
	private fun findOrCreate(entity: UuidNameRefEntity): UuidNameRefEntity =
			PersistentDao.findOrCreate(this.em,
					{ findByCompositeKey(entity) },
					{ this.em.merge(entity) })

	/**
	 * recursively finds/merges the parentRefs first, than itself, using helper method [findOrCreate]
	 */
	open fun findOrCreate(entry: UuidNameRef): UuidNameRefEntity =
			findOrCreate(UuidNameRefEntity(
					nameRef = entry,
					parentRefEntity = entry.getParentRef()?.let { parentRef ->
						parentRef.getParentRef()?.let { _ ->
							// first, recursively persist the new dao encoding the parentRef, if non-null
							findOrCreate(UuidNameRefEntity(parentRef))
						}
					})
					// then, persist the new dao encoding this child
			)

	override fun save(entry: UuidNameRef) {
		findOrCreate(entry)
	}

	override fun findByKey(key: UuidNameRef): UuidNameRef? =
			key.toEntity()?.let { findByCompositeKey(it)?.toUuidNameRef() }

	override fun delete(entry: UuidNameRef): Int =
			entry.toEntity()?.let { dao ->
				PersistentDao.deleteByCriteria(this.em, UuidNameRefEntity::class.java) { query, root ->
					query.where(matchCompositeKey(dao, root))
				}
			} ?: 0

}