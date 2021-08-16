package io.smetweb.math

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Observer
import java.util.function.Supplier

/**
 * an observable collection, mapping keys to tuples of property-value pairs and emitting [Change] operations
 *
 * TODO compare with [Apache Kafka Stream API](https://kafka.apache.org/28/documentation/streams/)
 */
interface Table<T: Table.Tuple> {

    /** return an [Observable] stream of [Change]s  */
    fun changes(): Observable<Change<*>>

    enum class Operation {
        CREATE, READ, UPDATE, DELETE
    }

    /**
     * [Property] wraps a value of a [Tuple] in some [Table]
     *
     * @param <T> the value return type
    </T> */
    interface Property<T>: Supplier<T?> {

        override fun get(): T?

        fun set(newValue: T)

        @Suppress("UNCHECKED_CAST")
        fun <THIS: Property<T>> with(newValue: T): THIS {
            set(newValue)
            return this as THIS
        }
    }

    /**
     * [Change] notifies modifications made to e.g. a [Property], [Tuple], [Table] or their sub-types
     */
    data class Change<T>(
            val operation: Operation,
            val sourceRef: Any?,
            val valueType: Class<*>,
            val update: Pair<T?, T?>
    ) {
        private val string: String by lazy {
            "$operation ${sourceRef?.let{"@it "}}${valueType.simpleName}: $update"
        }

        override fun toString(): String = this.string
    }

    /**
     * [Tuple] represents a [Property] set from a row in a
     * [Table]
     */
    open class Tuple(
            val key: Any,
            val properties: List<Property<*>>,
            private val getter: (Class<out Property<*>>) -> Any?,
            private val setter: (Class<out Property<*>>, Any?) -> Unit,
            private val stringifier: () -> String,
            private var observer: Observer<Change<*>>? = null
    ) {

        override fun toString(): String =
                this.stringifier()

        @Suppress("UNCHECKED_CAST")
        operator fun <V> get(key: Class<out Property<V>>): V? =
                this.getter(key) as V?

        operator fun <V> set(property: Class<out Property<V>>, value: V?) =
                this.setter(property, value)

        fun <V> set(property: Property<V>) =
                set(property.javaClass, property.get())

        fun set(vararg properties: Property<*>) {
            if (properties.isNotEmpty()) {
                for (element in properties) {
                    set(element)
                }
            }
        }

        fun toMap(vararg properties: Class<out Property<*>>): Map<Class<out Property<*>>, Any?> =
                if (properties.isEmpty()) emptyMap() else properties.associateWith { this.getter(it) }

        fun toMap(properties: Iterable<Class<out Property<*>>>): Map<Class<out Property<*>>, Any?> =
            properties.associateWith { this.getter(it) }

        fun <V> put(property: Property<V>): V? =
                getAndUpdate(property.javaClass) { property.get() }

        private fun <V> update(propertyType: Class<out Property<V>>, op: (V?) -> V?): Pair<V?, V?> {
            val oldValue: V? = get(propertyType)
            val newValue: V? = op(oldValue)
            val result = Pair(oldValue, newValue)
            if (newValue !== oldValue) {
                set(propertyType, newValue)
                this.observer?.onNext(
                        Change(Operation.UPDATE, this.key, propertyType, result))
            }
            return result
        }

        fun <V> getAndUpdate(propertyType: Class<Property<V>>, op: (V?) -> V?): V? =
                update(propertyType, op).first

        fun <V> updateAndGet(propertyType: Class<out Property<V>>, op: (V?) -> V?): V? =
                update(propertyType, op).second

//        fun <P : Property<W>, W> override(property: Class<P>, override: W?): Tuple? {
//            val self = this
//            return object : Tuple() {
//                override fun <K : Property<V>, V> get(key: Class<K>): V? {
//                    return if (key == property) override as V? else super.get(key)
//                }
//
//                override fun properties(): MutableList<Class<out Property>>? {
//                    return self.properties()
//                }
//            }.reset(key, emitter, getter, setter,
//                    stringifier)
//        }

        companion object {
            private const val start = '['
            private const val end = ']'
            private const val eq = ':'
            private const val delim: String = "; "
        }
    }

}