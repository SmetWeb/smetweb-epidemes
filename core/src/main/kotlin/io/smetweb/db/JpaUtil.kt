package io.smetweb.db

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import jakarta.persistence.*
import jakarta.persistence.criteria.CriteriaDelete
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Root
import jakarta.persistence.metamodel.SingularAttribute
import kotlin.math.max

/**
 * see http://stackoverflow.com/a/35587856, but with half the calls to `finder`
 *
 * @param retriever
 * @param factory
 * @return the current or created entity
 */
@Throws(PersistenceException::class)
fun <DAO> EntityManager.findOrCreate(retriever: () -> DAO?, factory: () -> DAO): DAO =
        retriever() ?:
            try {
                this.merge(factory())
            } catch (ex: PersistenceException) {
                retriever() ?: throw ex // other issue -> fail
            }

@Suppress("UNCHECKED_CAST")
@Throws(PersistenceException::class)
fun <T, R> EntityManager.query(
        entityType: Class<T>,
        resultType: Class<R>,
        matcher: (CriteriaQuery<R>, Root<T>) -> CriteriaQuery<R> =
                { query, root -> query.select(root as Root<R>) } // default: select all
): TypedQuery<R> {
    val query = this.criteriaBuilder.createQuery(resultType)
    val restriction = matcher(query, query.from(entityType))
    return this.createQuery(restriction)
}

/**
 * find any <ATT> values
 */
@Throws(PersistenceException::class)
fun <T, ATT> EntityManager.existAttributeValues(attr: SingularAttribute<T, ATT>, vararg values: ATT): List<ATT> =
        this.query(attr.declaringType.javaType, attr.bindableJavaType) { query, root ->
            val path = root.get(attr)
            query.select(path).where(path.`in`(*values))
        }.resultList

/**
 * get all key values <T> for matching records
 */
@Throws(PersistenceException::class)
fun <T, ATT> EntityManager.findAttributeValues(
        attr: SingularAttribute<T, ATT>,
        matcher: (CriteriaQuery<ATT>, Root<T>) -> CriteriaQuery<ATT> =
                { select, _ -> select } // default: select all
): List<ATT> =
        this.query(attr.declaringType.javaType, attr.bindableJavaType) { query, root ->
            matcher(query.select(root.get(attr)), root)
        }.resultList

@Throws(PersistenceException::class)
fun <T, ATT> EntityManager.findByAttribute(attr: SingularAttribute<T, ATT>, vararg values: ATT) =
        this.findByCriteria(attr.declaringType.javaType) { query, root ->
            query.select(root).where(root.get(attr).`in`(*values))
        }

@Throws(PersistenceException::class)
fun <T, ATT> EntityManager.findByAttribute(attr: SingularAttribute<T, ATT>, values: Collection<ATT>) =
        this.findByCriteria(attr.declaringType.javaType) { query, root ->
            query.select(root).where(root.get(attr).`in`(values))
        }

@Suppress("UNCHECKED_CAST")
@Throws(PersistenceException::class)
fun <T> EntityManager.findByCriteria(
        entityType: Class<T>,
        matcher: (CriteriaQuery<T>, Root<T>) -> CriteriaQuery<T> =
                { query, root -> query.select(root) } // default: select all
): List<T> =
        this.query(entityType, entityType, matcher).resultList

fun <T, ATT> EntityManager.deleteByAttribute(attr: SingularAttribute<T, ATT>, vararg values: ATT) =
        this.deleteByCriteria(attr.declaringType.javaType) { query, root ->
            query.where(root.get(attr).`in`(*values))
        }

fun <T, ATT> EntityManager.deleteByAttribute(attr: SingularAttribute<T, ATT>, values: Collection<ATT>) =
        this.deleteByCriteria(attr.declaringType.javaType) { query, root ->
            query.where(root.get(attr).`in`(values))
        }

fun <T> EntityManager.deleteByCriteria(
        entityType: Class<T>,
        matcher: (CriteriaDelete<T>, Root<T>) -> CriteriaDelete<T>
): Int {
    val query = this.criteriaBuilder.createCriteriaDelete(entityType)
    val restriction = matcher(query, query.from(entityType))
    return this.createQuery(restriction).executeUpdate()
}

/**
 * utility method
 *
 * @param pageSize the buffer size (small: more SQL, large: more heap)
 * @param keyAttr the unique (primary) key attribute/field-mapping to select and paginate
 * @param matcher the JPQL match query to execute, or `null` for all
 * @return a buffered [Observable] stream of paginated match result lists, if any
 */
fun <T, PK> EntityManager.findAsync(
        pageSize: Int,
        keyAttr: SingularAttribute<T, PK>,
        matcher: (CriteriaQuery<PK>, Root<T>) -> CriteriaQuery<PK> =
                { select, _ -> select } // default: select all
): Observable<List<T>> = Observable.using(
        { this.findAttributeValues(keyAttr, matcher) },
        { allKeys ->
            Observable.fromIterable(allKeys)
                    .buffer(max(1, pageSize))
                    .map { pageKeys -> this.findByAttribute(keyAttr, pageKeys) }
        },
        { /* no clean up required */ })

/**
 * [tx] does not work well within Spring Framework's @Transactional methods, which
 * are instrumented (via aspect-oriented programming) with an [EntityManager] and
 * [EntityManagerFactory] that do not seem to generate fully independent inner contexts
 */
fun EntityManagerFactory.tx(): Single<EntityManager> {
    val em = this.createEntityManager()
    em.transaction.begin()
    return Single.just(em)
            .doAfterSuccess {
                if (em.transaction.isActive) {
                    try {
                        em.transaction.commit()
                    } catch (ex: RollbackException) {
                    }
                }
            }
            .doOnError {
                if (em.transaction.isActive) {
                    em.transaction.rollback()
                }
            }
}
