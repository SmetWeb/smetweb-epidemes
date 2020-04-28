package io.smetweb.db

/**
 * [PersistentDao] tags data access objects (e.g. repositories) for values of type [V]
 * which are mapped to some Persistence API (e.g. JPA Entity or MyBatis Mapper);
 * and searchable (e.g. indexed) by some key mapped to type [K], supporting
 * [save], [delete], and [findByKey] operations.
 */
interface PersistentDao<V, K> {

	fun save(entry: V)

	fun delete(entry: V): Int

	fun findByKey(key: K): V?

}