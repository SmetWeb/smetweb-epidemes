package io.smetweb.random

import io.smetweb.math.TablePartitioner
import io.smetweb.math.Range
import io.smetweb.math.Table
import kotlin.Comparator
import kotlin.collections.ArrayList

/**
 * [StratifiedSampler] (see [wikipedia](https://www.wikiwand.com/en/Stratified_sampling)) is
 * a [ProbabilityDistribution] that samples [Table.Tuple]s from a [Table] stratified using a [TablePartitioner]
 * based on strata provided via [of] and the fluent builder APIs of [Root] and [Branch]
 */
interface StratifiedSampler<PK: Any> : ProbabilityDistribution<Table.Tuple<PK>> {

    val parent: StratifiedSampler<PK>

    val filter: MutableList<Comparable<*>?>
        get() = parent.filter

    val partitioner: TablePartitioner<PK>
        get() = parent.partitioner

    @Suppress("UNCHECKED_CAST")
    override fun draw(): Table.Tuple<PK> =
        parent.draw()

    @Suppress("UNCHECKED_CAST")
    fun draw(defaultOnDeviation:
                 (deviant: Class<out Table.Property<*>>, default: Range<*>, strata: Array<Comparable<*>?>) -> Boolean =
                 { _, _, _ -> true }
    ): Table.Tuple<PK> =
        parent.draw(defaultOnDeviation)

    interface Root<PK: Any>: StratifiedSampler<PK> {

        override val parent: Root<PK>
            get() = this

        fun <K: Table.Property<V>, V: Comparable<*>> splitBy(
            property: Class<K>,
            vararg boundaries: V,
            valueComparator: Comparator<V> = Comparator.naturalOrder()
        ): Branch<PK, K, V> =
            splitBy(property, boundaries.toList(), valueComparator)

        @Suppress("UNCHECKED_CAST")
        fun <K: Table.Property<V>, V: Comparable<*>> splitBy(
            property: Class<K>,
            boundaries: Iterable<V>,
            valueComparator: Comparator<V> = Comparator.naturalOrder()
        ): Branch<PK, K, V> {
            partitioner.stratify(property, boundaries, valueComparator)
            return Branch.of(this)
        }
    }

    interface Branch<PK: Any, P: Table.Property<T>, T: Comparable<*>>: StratifiedSampler<PK> {

        fun <K: Table.Property<V>, V: Comparable<*>> thenBy(
            property: Class<K>,
            vararg boundaries: V,
            valueComparator: Comparator<V> = Comparator.naturalOrder()
        ): Branch<PK, K, V> =
            thenBy(property, boundaries.toList(), valueComparator)

        fun <K: Table.Property<V>, V: Comparable<*>> thenBy(
            property: Class<K>,
            boundaries: Iterable<V>,
            valueComparator: Comparator<V> = Comparator.naturalOrder()
        ): Branch<PK, K, V> {
            partitioner.stratify(property, boundaries, valueComparator)
            return of(this)
        }

        fun any(): StratifiedSampler<PK> {
            filter.add(0, null)
            return parent
        }

        fun match(valueFilter: T): StratifiedSampler<PK> {
            filter.add(0, valueFilter)
            return parent
        }

        fun match(rangeFilter: Range<T>): StratifiedSampler<PK> {
            filter.add(0, rangeFilter)
            return parent
        }

        companion object {
            fun <PK: Any, K: Table.Property<V>, V: Comparable<*>> of(parent: StratifiedSampler<PK>): Branch<PK, K, V> =
                object : Branch<PK, K, V> {
                    override val parent: StratifiedSampler<PK> = parent
                }
        }
    }

    companion object {

        fun <PK: Any> of(
            source: Table<PK>,
            rng: PseudoRandom,
            defaultOnDeviation:
                (deviant: Class<out Table.Property<*>>, default: Range<*>, strata: Array<Comparable<*>?>) -> Boolean =
                { _, _, _, -> true },
            validation: Boolean = false,
            onError: (Throwable) -> Unit = Throwable::printStackTrace
        ): Root<PK> =
            object : Root<PK> {

                override val partitioner = TablePartitioner(source, validation, onError)

                override val filter: MutableList<Comparable<*>?> = ArrayList()

                override fun draw(): Table.Tuple<PK> =
                    draw(defaultOnDeviation)

                override fun draw(
                    defaultOnDeviation:
                        (deviant: Class<out Table.Property<*>>, default: Range<*>, strata: Array<Comparable<*>?>) -> Boolean
                ): Table.Tuple<PK> {
                    val strata = filter.toTypedArray()
                    filter.clear() // prevents accumulation before each draw
                    val keys = partitioner.nearestKeys(*strata) { deviant, bin -> defaultOnDeviation(deviant, bin, strata) }
                    return source.select(rng.nextElement(keys))!!
                }
            }
    }
}