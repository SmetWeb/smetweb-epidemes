package io.smetweb.math

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.PublishSubject
import io.reactivex.rxjava3.subjects.Subject
import io.smetweb.math.Table.Change
import java.util.*
import java.util.concurrent.atomic.AtomicReference

/**
 * an observable collection, mapping keys to tuples of property-value pairs and emitting [Change] operations
 *
 * TODO compare with [Apache Kafka Stream API](https://kafka.apache.org/28/documentation/streams/)
 */
open class Table<PK: Any>(
    private val inserter: () -> Tuple<PK>,
    private val remover: (PK) -> Tuple<PK>?,
    private val indexer: () -> Iterable<PK>,
    private val retriever: (PK) -> Tuple<PK>?,
    private val counter: () -> Long,
    private val printer: () -> String,
    private val cleaner: () -> Unit,
    private val emitter: Subject<Change<PK>> = PublishSubject.create()
) {

    val keys: Iterable<PK>
        get() = indexer()

    val size: Long
        get() = counter()

    fun clear() =
        cleaner()

    /** return an [Observable] stream of [Change]s  */
    val changes: Observable<Change<PK>>
        get() = emitter

    override fun toString(): String =
        this.printer()

    fun select(key: PK): Tuple<PK>? =
        this.retriever(key)

    fun insert(values: Map<Class<out Property<*>>, Any?>): PK {
        val tuple: Tuple<PK> = inserter()
        emitter.onNext(Change(Operation.CREATE, tuple.key, tuple::class.java, Pair(null, tuple)))
        tuple.set(values)
            .map { Change(Operation.UPDATE, tuple.key, it.first, it.second) }
            .subscribe(emitter::onNext, emitter::onError)
        return tuple.key
    }

    fun insert(values: Iterable<Property<*>>): PK =
        insert(values.associate { Pair(it::class.java, it.get()) })

    fun insert(vararg values: Property<*>): PK =
        insert(values.associate { Pair(it::class.java, it.get()) })

    fun update(key: PK, values: Map<Class<out Property<*>>, Any?>) =
        this.retriever(key)!!.set(values)
            .map { Change(Operation.UPDATE, key, it.first, it.second) }
            .subscribe(emitter::onNext, emitter::onError)

    fun update(key: PK, values: Iterable<Property<*>>) =
        update(key, values.associate { Pair(it::class.java, it.get()) })

    fun update(key: PK, vararg values: Property<*>) =
        update(key, values.associate { Pair(it::class.java, it.get()) })

    fun upsert(key: PK, values: Map<Class<out Property<*>>, Any?>): PK =
        this.retriever(key)?.let { tuple ->
            tuple.set(values)
                .map { Change(Operation.UPDATE, tuple.key, it.first, it.second) }
                .subscribe(emitter::onNext, emitter::onError)
            tuple.key
        } ?: insert(values)

    fun upsert(key: PK, values: Iterable<Property<*>>): PK =
        upsert(key, values.associate { Pair(it::class.java, it.get()) })

    fun upsert(key: PK, vararg values: Property<*>): PK =
        upsert(key, values.associate { Pair(it::class.java, it.get()) })

    fun delete(key: PK): Boolean =
        select(key)?.let { old ->
            emitter.onNext(Change(Operation.DELETE, key, old::class.java, Pair(old, null)))
            remover(key)
        } != null

    enum class Operation {
        CREATE,
//        READ, // TODO use me?
        UPDATE,
        DELETE
    }

    /**
     * [Property] wraps a value of a [Tuple] in some [Table]
     *
     * @param T the value return type
     */
    open class Property<T>(value: T? = null): AtomicReference<T>(value)

    /**
     * [Change] notifies creation/deletion of [Tuple], or updates to a [Property]
     */
    data class Change<PK: Any>(
        val operation: Operation,
        val keyRef: PK,
        val valueType: Class<*>,
        val update: Pair<Any?, Any?>
    ) {
        private val string: String by lazy {
            "$operation ${keyRef.let{"@$it "} ?: ""}${valueType.simpleName}: $update"
        }

        override fun toString(): String = string
    }

    /**
     * [Tuple] represents a [Property] set from a row in a [Table]
     */
    open class Tuple<PK: Any>(
        open val key: PK,
        open val properties: List<Class<out Property<*>>>,
        private val getter: (Class<out Property<*>>) -> Any?,
        private val setter: (Class<out Property<*>>, Any?) -> Unit,
        private val stringifier: () -> String = {
            buildString {
                append('@')
                append(key)
                append(stringifyProperties(properties, getter))
            }
        }
    ) {

        override fun toString(): String =
            stringifier()

        fun <V> set(property: Property<V>) =
            set(property::class.java, property.get())

        fun set(vararg properties: Property<*>) =
            properties.forEach { set(it) }

        fun set(newValues: Map<Class<out Property<*>>, Any?>): Observable<Pair<Class<out Property<*>>, Pair<Any?, Any?>>> =
            Observable.fromIterable(newValues.entries)
                .mapOptional { (propertyType, newValue) ->
                    set(propertyType, newValue)?.let {
                        Optional.of(Pair(propertyType, it))
                    } ?: Optional.empty()
                }

        @Suppress("UNCHECKED_CAST")
        operator fun set(propertyType: Class<out Property<*>>, newValue: Any?): Pair<Any?, Any?>? =
            propertyType.let {
                val oldValue = getter(it)
                if(oldValue !== newValue) {
                    setter(it, newValue)
                    Pair(oldValue, newValue)
                } else {
                    null
                }
            }

        operator fun get(key: Class<out Property<*>>): Any? =
            getter(key)

        fun toMap(vararg property: Class<out Property<*>>): Map<Class<out Property<*>>, Any?> =
                if (property.isEmpty())
                    emptyMap()
                else
                    property.associateWith { this.getter(it) }

        fun toMap(propertyFilter: Iterable<Class<out Property<*>>> = this.properties): Map<Class<out Property<*>>, Any?> =
            propertyFilter.associateWith { getter(it) }

        fun <V> put(property: Property<V>): V? =
                getAndUpdate(property::class.java) { property.get() }

        @Suppress("UNCHECKED_CAST")
        private fun <V> update(propertyType: Class<out Property<*>>, op: (V?) -> V?): Change<PK>? {
            val oldValue: V? = get(propertyType) as V?
            val newValue: V? = op(oldValue)
            return set(propertyType, newValue)?.let {
                Change(Operation.UPDATE, this.key, propertyType, it)
            }
        }

        @Suppress("UNCHECKED_CAST")
        fun <V> getAndUpdate(propertyType: Class<out Property<*>>, op: (V?) -> V?): V? =
                update(propertyType, op)?.update?.first as V?

        @Suppress("UNCHECKED_CAST")
        fun <V> updateAndGet(propertyType: Class<out Property<V>>, op: (V?) -> V?): V? =
                update(propertyType, op)?.update?.second as V?

        fun withOverride(property: Class<*>, newValue: Any?): Tuple<PK> =
            Tuple(
                key = key,
                properties = properties,
                getter = { key: Class<out Property<*>> -> if (key == property) newValue else getter(key) },
                setter = setter,
                stringifier = stringifier)

        companion object {

            const val start = '['
            const val end = ']'
            const val eq = ':'
            const val delim: String = "; "

            @JvmStatic
            private fun stringifyProperty(
                i: Int,
                properties: List<Class<out Property<*>>>,
                getter: (Class<out Property<*>>) -> Any?
            ): String = buildString {
                append(properties[i].simpleName, eq, getter(properties[i]) ?: "")
            }

            @JvmStatic
            private fun stringifyProperties(
                properties: List<Class<out Property<*>>>,
                getter: (Class<out Property<*>>) -> Any?
            ): String = buildString {
                append(start)
                if(properties.isNotEmpty()) {
                    append(stringifyProperty(0, properties, getter))
                    (1 until properties.size).forEach { i ->
                        append(delim, stringifyProperty(i, properties, getter))
                    }
                }
                append(end)
            }
        }
    }

}