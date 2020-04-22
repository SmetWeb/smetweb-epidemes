package io.smetweb.jpa

import io.reactivex.Observable
import io.reactivex.Single
import io.smetweb.log.getLogger
import javax.persistence.*
import javax.persistence.metamodel.SingularAttribute
import javax.persistence.criteria.*
import kotlin.math.max

/**
 * [PersistentDao] tags data access objects (e.g. repositories) for values of type [V]
 * which are mapped to some Persistence API (e.g. JPA Entity or MyBatis Mapper);
 * and searchable (e.g. indexed) by some key mapped to type [K], supporting
 * [save], [delete], and [findByKey] operations.
 * <p>
 * JPA utility methods (using e.g. generated meta-model mappings) include:
 * [tx], [query], [findOrCreate], [findByCriteria], [findAttributeValues],
 * [findAsync], [deleteByCriteria] and [deleteByAttribute]
 */
interface PersistentDao<V, K> {

	fun save(entry: V)

	fun delete(entry: V): Int

	fun findByKey(key: K): V?

	companion object {

		private val LOG = getLogger()

		/**
		 * [tx] does not work well within Spring Framework's @Transactional methods, which
		 * are instrumented (via aspect-oriented programming) with an [EntityManager] and
		 * [EntityManagerFactory] that do not seem to generate fully independent inner contexts
		 */
		@JvmStatic
		fun tx(emf: EntityManagerFactory): Single<EntityManager> {
			val em = emf.createEntityManager()
			LOG.debug("Created inner context, open: ${em.isOpen}")
			em.transaction.begin()
			LOG.debug("Started inner transaction, open: ${em.isOpen}")
			return Single.just(em)
					.doAfterSuccess {
						if (em.transaction.isActive) {
							try {
								em.transaction.commit()
								LOG.debug("Inner transaction committed")
							} catch (ex: RollbackException) {
								LOG.debug("Inner transaction rolled back: ${ex.message}")
							}
						}
					}
					.doOnError {
						if (em.transaction.isActive) {
							LOG.debug("Rolling back and closing inner context due to error: ${it.message}")
							em.transaction.rollback()
						} else {
							LOG.debug("Some error occurred after commit: ${it.message}")
						}
					}
		}

		/**
		 * see http://stackoverflow.com/a/35587856, but with half the calls to `finder`
		 *
		 * @param em
		 * @param retriever
		 * @param factory
		 * @return the current or created entity
		 */
		@JvmStatic
		@Throws(PersistenceException::class)
		fun <DAO> findOrCreate(
				em: EntityManager,
				retriever: () -> DAO?,
				factory: () -> DAO
		): DAO = retriever() ?: let {
			LOG.trace("Unable to retrieve, attempting to create and insert...")
			return try {
				em.merge(factory())
			} catch (ex: PersistenceException) {
				LOG.trace("Unable to create and/or insert, retrying retrieve...")
				retriever() ?: let {
					throw ex // other issue -> fail
				}
			}
		}

		@JvmStatic
		fun <T, ATT> deleteByAttribute(
				em: EntityManager,
				attr: SingularAttribute<T, ATT>,
				vararg values: ATT
		) = deleteByCriteria(em, attr.declaringType.javaType) { query, root ->
			query.where(root.get(attr).`in`(*values))
		}

		@JvmStatic
		fun <T, ATT> deleteByAttribute(
				em: EntityManager,
				attr: SingularAttribute<T, ATT>,
				values: Collection<ATT>
		) = deleteByCriteria(em, attr.declaringType.javaType) { query, root ->
			query.where(root.get(attr).`in`(values))
		}

		@JvmStatic
		fun <T> deleteByCriteria(
				em: EntityManager,
				entityType: Class<T>,
				matcher: (CriteriaDelete<T>, Root<T>) -> CriteriaDelete<T>
		): Int {
			val query = em.criteriaBuilder.createCriteriaDelete(entityType)
			val restriction = matcher(query, query.from(entityType))
			return em.createQuery(restriction).executeUpdate()
		}

		/**
		 * get all key values for matching records
		 */
		@JvmStatic
		@Throws(PersistenceException::class)
		fun <T, ATT> existAttributeValues(
				em: EntityManager,
				attr: SingularAttribute<T, ATT>,
				vararg values: ATT
		): List<ATT> = query(em, attr.declaringType.javaType, attr.bindableJavaType) { query, root ->
			val path = root.get(attr)
			query.select(path).where(path.`in`(*values))
		}.resultList

		/**
		 * get all key values for matching records
		 */
		@JvmStatic
		@Throws(PersistenceException::class)
		fun <T, ATT> findAttributeValues(
				em: EntityManager,
				attr: SingularAttribute<T, ATT>,
				matcher: (CriteriaQuery<ATT>, Root<T>) -> CriteriaQuery<ATT> =
						{ select, _ -> select } // default: select all
		): List<ATT> = query(em, attr.declaringType.javaType, attr.bindableJavaType) { query, root ->
			matcher(query.select(root.get(attr)), root)
		}.resultList

		@JvmStatic
		@Throws(PersistenceException::class)
		fun <T, ATT> findByAttribute(
				em: EntityManager,
				attr: SingularAttribute<T, ATT>,
				vararg values: ATT
		) = findByCriteria(em, attr.declaringType.javaType) { query, root ->
			query.select(root).where(root.get(attr).`in`(*values))
		}

		@JvmStatic
		@Throws(PersistenceException::class)
		fun <T, ATT> findByAttribute(
				em: EntityManager,
				attr: SingularAttribute<T, ATT>,
				values: Collection<ATT>
		) = findByCriteria(em, attr.declaringType.javaType) { query, root ->
			query.select(root).where(root.get(attr).`in`(values))
		}

		@JvmStatic
		@Suppress("UNCHECKED_CAST")
		@Throws(PersistenceException::class)
		fun <T> findByCriteria(
				em: EntityManager,
				entityType: Class<T>,
				matcher: (CriteriaQuery<T>, Root<T>) -> CriteriaQuery<T> =
						{ query, root -> query.select(root) } // default: select all
		): List<T> = query(em, entityType, entityType, matcher).resultList

		@JvmStatic
		@Suppress("UNCHECKED_CAST")
		@Throws(PersistenceException::class)
		fun <T, R> query(
				em: EntityManager,
				entityType: Class<T>,
				resultType: Class<R>,
				matcher: (CriteriaQuery<R>, Root<T>) -> CriteriaQuery<R> =
						{ query, root -> query.select(root as Root<R>) } // default: select all
		): TypedQuery<R> {
			val query = em.criteriaBuilder.createQuery(resultType)
			val restriction = matcher(query, query.from(entityType))
			return em.createQuery(restriction)
		}

		/**
		 * utility method
		 *
		 * @param em the session or [EntityManager]
		 * @param pageSize the buffer size (small: more SQL, large: more heap)
		 * @param keyAttr the unique (primary) key attribute/field-mapping to select and paginate
		 * @param matcher the JPQL match query to execute, or `null` for all
		 * @return a buffered [Observable] stream of paginated match result lists, if any
		 */
		@JvmStatic
		fun <T, PK> findAsync(
				em: EntityManager,
				pageSize: Int,
				keyAttr: SingularAttribute<T, PK>,
				matcher: (CriteriaQuery<PK>, Root<T>) -> CriteriaQuery<PK> =
						{ select, _ -> select } // default: select all
		): Observable<List<T>> = Observable.using(
				{ findAttributeValues(em, keyAttr, matcher) },
				{ allKeys ->
					Observable.fromIterable(allKeys)
						.buffer(max(1, pageSize))
						.map { pageKeys -> findByAttribute(em, keyAttr, pageKeys) }
				},
				{ /* no clean up required */ })
	}
}