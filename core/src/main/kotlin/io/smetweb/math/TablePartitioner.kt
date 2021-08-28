package io.smetweb.math

import io.smetweb.log.getLogger
import java.util.*
import java.util.function.*
import java.util.stream.IntStream
import java.util.stream.Stream


/**
 * [TablePartitioner] maintains an ordered index partition of strata
 * based on (a [hierarchy] of) one [Table.Property] or more (iff its value type is [Comparable]),
 * having at least one [Stratum] per [Stratification] (outer bounds are infinite)
 * plus one [Stratum] for each of its intermediate split values,
 * for enabling *[reproducible](https://www.wikiwand.com/en/Reproducibility)*
 * [stratified sampling](https://www.wikiwand.com/en/Stratified_sampling)
 * of [Table.Tuple]s (i.e. same random seed yields same sampling order).
 *
 * TODO: replace tree (duplicate end/begin indices) by flat split point array?
 * may reduce tree-search speed across (unbalanced) siblings compared to
 * Arrays#binarysearch
 */
class TablePartitioner<PK: Any>(
    private val table: Table<PK>,
    private val validation: Boolean = false,
    private val onError: (Throwable) -> Unit = Throwable::printStackTrace,
) {
    private val log = getLogger()
    private val root: Stratum<PK>
    private val hierarchy: MutableList<Stratification<*>> = mutableListOf()
    private val keys: MutableList<PK> = Collections.synchronizedList(table.keys.toMutableList())

    init {
        root = Stratum(indexRange = intArrayOf(0, keys.size))
        // subscribe as last subscriber?
        table.changes.subscribe({ ch: Table.Change<PK> ->
            try {
                onChange(ch)
            } catch (e: Throwable) {
                // internal error
                onError(e)
            }
        }, onError)
    }

    override fun toString(): String =
        try {
            val n = keys.size
            buildString {
                if (n < 8)
                    append(keys)
                else
                    append("[", keys[0], ", ", keys[1], ", ", keys[2], ", ..., ", keys[n - 3], ", ",
                        keys[n - 2], ", ", keys[n - 1], "]")
                append(" <-", root.bounds.contentToString(), "- ", root)
            }
        } catch (e: Exception) {
            onError(IllegalStateException("Unable to render string of partition: " + e.message, e))
            ""
        }

    fun index() = this.root.indexKeyStream()

    fun <P: Table.Property<V>, V: Comparable<*>> stratify(
        property: Class<P>,
        boundaries: Iterable<V> = emptyList(),
        valueComparator: Comparator<V> = Comparator.naturalOrder(),
    ): TablePartitioner<PK> {
        val stratification = Stratification(property, boundaries, valueComparator)
        hierarchy.add(stratification)
        root.split(stratification, ::stratifier, { evaluator(stratification, it) }, ::nodeSplitter)
        return this
    }

    private fun findStratum(vararg sampleValues: Comparable<*>): Stratum<PK> {
        if (root.isEmpty || sampleValues.isEmpty())
            return root

        var result: Stratum<PK> = this.root
        for (value: Comparable<*> in sampleValues) {
            if (result.stratification == null)
                error("Filter values ${sampleValues.contentToString()} exceed available groupings: $hierarchy")
            val range = if (value is Range<*>) value else Range(value)
            result = if (result.stratification!!.splitOrder.isEmpty()) // walk (value) branch
                result.valueNode(range, ::nodeSplitter)
            else if (result.children != null) // walk (sub-range) branch
                result.children!!.floorEntry(range).value
            else
                error("Unexpected, no match for filters: " + sampleValues.contentToString())
        }
        return result
    }

    fun keys(vararg valueFilter: Comparable<*>): List<Any> =
        if (root.isEmpty)
            emptyList()
        else
            stratifier(findStratum(*valueFilter).bounds).toList()

    fun nearestKeys(
        deviationConfirmer: (Class<*>, Range<*>) -> Boolean,
        vararg valueFilter: Comparable<*>
    ): List<Any?> {
        if (root.isEmpty)
            return Collections.emptyList()

        if (valueFilter.isEmpty())
            return Collections.unmodifiableList(keys)

        // walk the hierarchy
        var stratum: Stratum<PK> = this.root
        var childEntry: Map.Entry<Range<*>, Stratum<PK>>?
        for (value: Comparable<*>? in valueFilter) {
            val valueRange: Range<*>

            if (value is Range<*>) {
                valueRange = value
                // TODO merge node-bins if value-range contains multiple nodes/range
                val low: Stratum<PK>? = (
                        if (valueRange.lowerFinite())
                            stratum.children!!.ceilingEntry(Range(valueRange.lowerBound()))
                        else
                            stratum.children!!.firstEntry()
                        )?.value
                val high: Stratum<PK>? = (
                        if (valueRange.upperFinite())
                            stratum.children!!.floorEntry(Range(valueRange.upperBound()))
                        else
                            stratum.children!!.lastEntry()
                        )?.value
                val start = low?.bounds?.get(0) ?: stratum.bounds[0]
                val end = high?.bounds?.get(1) ?: stratum.bounds[1]
                if (start != end) // FIXME merge results from remaining filter values/ranges
                    return keys.subList(start, end)
            } else
                valueRange = Range(value)

            childEntry = stratum.children?.floorEntry(valueRange)
            if (childEntry == null || childEntry.value.isEmpty) {
                var prev = childEntry
                var next = childEntry
                while (prev != null && prev.value.isEmpty)
                    prev = stratum.children!!.lowerEntry(prev.key)
                while (next != null && next.value.isEmpty)
                    next = stratum.children!!.higherEntry(next.key)
                // upper category undefined/empty, expand by 1 within bounds
                val range = Range(prev?.key?.lowerBound(), next?.key?.upperBound())
                if (!deviationConfirmer(stratum.stratification!!.propertyType, range))
                    return emptyList()
                val start = prev?.value?.bounds?.get(0) ?: stratum.bounds[0]
                val end = next?.value?.bounds?.get(1) ?: stratum.bounds[1]
                return keys.subList(start, end)
            }
            stratum = childEntry.value
        }
        return stratifier(stratum.bounds).toList()
    }

    private fun onChange(d: Table.Change<PK>) {
        when (d.operation) {
            Table.Operation.CREATE -> {
                add(table.select(d.keyRef)!!)
                if (validation) {
                    validate()
                    if (keys.indexOf(d.keyRef) < 0)
                        onError(IllegalStateException("failed insert of ${d.keyRef}, not indexed: $this"))
                }
            }
            Table.Operation.DELETE -> {
                remove(table.select(d.keyRef)!!)
                if (validation) {
                    validate()
                    val i = keys.indexOf(d.keyRef)
                    if (i >= 0)
                        onError(IllegalStateException("failed delete of ${d.keyRef}, still indexed at: $i: $this"))
                }
            }
            Table.Operation.UPDATE -> {
                hierarchy.firstOrNull { it.propertyType == d.valueType }?.let {
                    val t: Table.Tuple<PK> = table.select(d.keyRef)!!
                    remove(t.withOverride(it.propertyType, d.update.first))
                    add(t)
                    if (validation) {
                        validate()
                        if (keys.indexOf(d.keyRef) < 0)
                            onError(IllegalStateException("failed update of ${d.keyRef}, not indexed: $this"))
                    }
                }
            }
        }
    }

    private fun validate() {
        val invalid = this.root
            .invalidChildren { i -> table.select(keys[i])!! }
            .toArray()
        if (invalid.isNotEmpty())
            onError(IllegalStateException("Invalid: $invalid of $keys"))
    }

    private fun add(t: Table.Tuple<PK>) {
        this.root.resize(t, +1,  { _, leaf ->
            // insert halfway
            keys.add((leaf.bounds[0] + leaf.bounds[1]) / 2, t.key)
            true
        }, ::nodeSplitter)
    }

    private fun remove(tuple: Table.Tuple<PK>) {
        this.root.resize(tuple, -1, { _, leaf ->
            val index: Int = stratifier(leaf.bounds).indexOf(tuple.key)
            if (index < 0) {
                val j: Int = keys.indexOf(tuple.key)
                if (j < 0) {
                    return@resize false
                }
                val leafRanges: MutableList<Range<*>?> = ArrayList()
                val indexRanges: MutableList<Range<*>?> = ArrayList()
                val values: MutableList<Range<*>> = ArrayList()
                // visit ancestors
                var stratum1: Stratum<PK> = leaf
                while (stratum1.parent != null) {
                    leafRanges.add(0, stratum1.parentRange)
                    stratum1 = stratum1.parent!!
                }
                // visit offspring
                var stratum: Stratum<PK>? = this.root
                while (stratum != null) {
                    if (stratum.parentRange != null) {
                        indexRanges.add(stratum.parentRange)
                        val key = stratum.parent!!.stratification!!.propertyType
                        val value = tuple[key] as Comparable<*>
                        values.add(Range(value))
                    }
                    stratum = if (stratum.children == null)
                        null
                    else
                        stratum.children!!.values.stream()
                            .filter { it.bounds[0] <= j && j < it.bounds[1] }
                            .findAny()
                            .orElse(null)
                }
                log.warn(("Remove failed, {} {} not in #{} {}, but found at #{} {}: {}"),
                    tuple.key, values, leaf.bounds, leafRanges, j, indexRanges, this.root)
                return@resize false
            }
            // log.trace( "removing #{}={} in #{}", i + leaf.bounds[0], key, leaf.bounds );
            keys.removeAt(index + leaf.bounds[0])
            true
        }, ::nodeSplitter)
    }

    private fun stratifier(bounds: IntArray): MutableList<PK> =
        keys.subList(bounds[0], bounds[1])

    @Suppress("UNCHECKED_CAST")
    fun <V: Comparable<*>> evaluator(dim: Stratification<V>, key: PK): V? =
        table.select(key)?.get(dim.propertyType) as V?

    private fun <V: Comparable<*>> split(stratum: Stratum<PK>, dim: Stratification<V>) {
        stratum.split(dim, ::stratifier, { key -> evaluator(dim, key) }, ::nodeSplitter)
        if (validation)
            validate()
    }

    private fun nodeSplitter(stratum: Stratum<PK>) {
        var passed = false
        for (i in 0 until hierarchy.size - 1) {
            if (!passed && hierarchy[i] === stratum.parent!!.stratification)
                passed = true
            if (passed)
                split(stratum, hierarchy[i + 1])
        }
    }

    data class Stratification<V: Comparable<*>> internal constructor(
        val propertyType: Class<out Table.Property<V>>,
        val valueComparator: Comparator<V>,
        val splitOrder: List<V>,
    ) {
        constructor(
            propertyType: Class<out Table.Property<V>>,
            boundaries: Iterable<V>,
            valueComparator: Comparator<V> = Comparator.naturalOrder(),
        ): this(
            propertyType = propertyType,
            splitOrder = boundaries.distinct().sortedWith(valueComparator),
            valueComparator = valueComparator)

        override fun toString(): String =
            ":" + propertyType.simpleName
    }

    /**
     * [Stratum] helper class to build the partition-tree
     */
    class Stratum<PK> internal constructor(
        val parent: Stratum<PK>? = null, // null == root
        val parentRange: Range<*>? = null,  // null == root
        indexRange: IntArray,
        val bounds: IntArray = indexRange.copyOf()
    ) {
        private val log = getLogger()
        var stratification: Stratification<*>? = null // null == leaf
        var children: NavigableMap<Range<*>, Stratum<PK>>? = null // null = leaf

        override fun toString(): String {
            val n = bounds[1] - bounds[0]
            return children?.let {
                // branch
                val branchStr = it.entries.stream()
                    .map { (bin, node) ->
//                        if (node.isEmpty) // category key
//                            ""
//                        else
                            ((if (stratification!!.splitOrder.isEmpty()) // intermediate range
                                " '${bin.lower.value}':"
                            else if (bin.lowerFinite()) // lowest range
                                (" < ${bin.lower.value} =<")
                            else
                                "")
                                    + " " + node) // value(s): leaf or branch
                    }
                    .reduce { l, r -> l + r }
                    .orElse("")
                val name = stratification!!.toString()
                buildString {
                    append("{", name.substring(0, 7.coerceAtMost(name.length)), branchStr, "}")
                }
            }
            // leaf
                ?: buildString {
                    if (n > 2)
                        append(n, "x")
                    append("[")
                    if(!isEmpty) {
                        append(bounds[0])
                        if (n > 1)
                            append(when (n)
                            {
                                2 -> ","
                                else -> ".."
                            }, bounds[1] - 1)
                    }
                    append("]")
                }
        }

        val isEmpty: Boolean
            get() = bounds[0] == bounds[1]

        fun indexKeyStream(): IntStream =
            children
                // recurse
                ?.values?.stream()?.flatMapToInt(Stratum<PK>::indexKeyStream)
                // stop
                ?: IntStream.range(bounds[0], bounds[1])

        fun resize(
            tuple: Table.Tuple<*>,
            delta: Int,
            leafHandler: (Range<*>, Stratum<PK>) -> Boolean,
            nodeSplitter: (Stratum<PK>) -> Unit
        ) {
            if(stratification == null)
                error("Missing dimension?")

            val value = (tuple[stratification!!.propertyType]
                ?: error("$tuple has no value for $stratification")) as Comparable<*>
            if (stratification!!.splitOrder.isEmpty()) {
                // no split points: each value is a bin
                val bin: Range<*> = Range(value)
                val child = valueNode(bin, nodeSplitter)
                if (child.children != null) {
                    // resize children/sub-ranges recursively
                    child.resize(tuple, delta, leafHandler, nodeSplitter)
                    bounds[1] += delta // resize parent after child
                    shift(children!!.tailMap(bin, false).values.stream(), delta)
                } else if (leafHandler(bin, child)) {
                    // resize matching leaf child (end recursion)
                    child.bounds[1] += delta // resize leaf
                    bounds[1] += delta // resize parent after child
                    shift(children!!.tailMap(bin, false).values.stream(), delta)
                } else
                    log.warn("leaf not adjusted: {}", bin)
            } else {
                // find appropriate range between provided split points
                val bin: Range<*> = Range(value)
                val entry = children?.floorEntry(bin)
                    ?: error("Unexpected, $stratification: $value -> $bin < " + children!!.firstKey())
                val child = entry.value
                if (child!!.children != null) {
                    // resize children/sub-ranges recursively
                    child.resize(tuple, delta, leafHandler, nodeSplitter)
                    bounds[1] += delta // resize parent
                    shift(children!!.tailMap(bin, false).values.stream(), delta)
                } else if (leafHandler(entry.key, child)) {
                    // resize matching leaf child (end recursion)
                    child.bounds[1] += delta
                    bounds[1] += delta // resize parent
                    shift(children!!.tailMap(bin, false).values.stream(), delta)
                } else
                    log.warn("leaf not adjusted: {}", bin);
            }
        }

        private fun shift(nodes: Stream<Stratum<PK>>, delta: Int) {
            nodes.forEach { node ->
                node.bounds[0] += delta
                node.bounds[1] += delta
                // recurse
                if (node.children != null)
                    shift(node.children!!.values.stream(), delta)
            }
        }

        /** helper function to avoid reification */
        @Suppress("UNCHECKED_CAST")
        private fun <V: Comparable<*>> Any.isIn(range: Range<V>): Boolean =
            range.contains(this as V)

        fun invalidChildren(tupleFetcher: (Int) -> Table.Tuple<*>): IntStream =
                children?.entries?.stream()
                    // recurse on children
                    ?.flatMapToInt { (bin: Range<*>, stratum: Stratum<PK>) ->
                        // sort once
                        val invalid: IntArray = stratum.invalidChildren(tupleFetcher).sorted().toArray()
                        IntStream.range(stratum.bounds[0], stratum.bounds[1])
                            // invalid: child(ren) invalid
                            .filter { Arrays.binarySearch(invalid, it) >= 0 }
                            // invalid: value out of range
                            .filter {
                                val tuple = tupleFetcher(it)
                                val value = tuple[stratification!!.propertyType] ?: error("No value for $stratification in $tuple")
                                !value.isIn(bin)
                            }
                    }
                    ?: IntStream.empty()

        @Suppress("UNCHECKED_CAST")
        fun <V: Comparable<*>> split(
            strat: Stratification<V>,
            partitioner: (IntArray) -> MutableList<PK>,
            valueSupplier: (PK) -> V?,
            nodeSplitter: (Stratum<PK>) -> Unit
        ) {
            if (children != null) // reached leaf node
            {
                children!!.values.forEach { it.split(strat, partitioner, valueSupplier, nodeSplitter ) }
                return
            }
            // provide the dimension info to each affected leaf
            this.stratification = strat
            // sort node key-partition using given property value comparator
            val partition = partitioner(bounds)
            try {
                partition.sortWith { k1, k2 -> strat.valueComparator.compare(valueSupplier(k1), valueSupplier(k2)) }
            } catch (e: NullPointerException) {
                error("Missing value(s) for $strat")
            }
            if (strat.splitOrder.isEmpty()) {
                // split points empty? add all distinct values as split point
                children = TreeMap() // infinity placeholder range: Range.infinite() to new PartitionNode( this, this.bounds )
                if (partition.isEmpty())
                    return
                var v: V = valueSupplier(partition[0])!!
                var vNode = valueNode(Range(v), nodeSplitter)
                var offset = bounds[0]
                for (i in 1 until partition.size) {
                    val v2: V = valueSupplier(partition[i])!!
                    if (strat.valueComparator.compare(v2, v) != 0) {
                        v = v2
                        vNode.bounds[0] = offset
                        vNode.bounds[1] = bounds[0] + i
                        offset = vNode.bounds[1] // intermediate
                        vNode = valueNode(Range(v2), nodeSplitter)
                    }
                }
                vNode.bounds[0] = offset
                vNode.bounds[1] = bounds[1]
                return
            }
            val splitKeys = IntArray(strat.splitOrder.size)
            var i = 0
            var k = 0
            while (i != strat.splitOrder.size) {
                while (k < partition.size && strat.valueComparator.compare(valueSupplier(partition[k]), strat.splitOrder[i]) < 0)
                    k++ // value[key] > point[i] : put key in next range
                splitKeys[i] = bounds[0] + k
                i++
            }

            // map split points to respective sub-partition bounds
            val map = IntStream.range(0, strat.splitOrder.size + 1)
                .collect(
                    // create new partition's range-bounds mapping
                    { TreeMap { r1, r2 -> Range.compare(r1 as Range<V>, r2 as Range<V>, strat.valueComparator) } },
                    // add split node (value range and key bounds)
                    { map, j: Int ->
                        val range: Range<*> = toRange(strat.splitOrder, j)
                        //log.trace( "Splitting {} -> {}", i, range );
                        val next = Stratum(this, range, toBounds(bounds, splitKeys, j))
                        val old: Stratum<PK>? = map.put(range, next)
                        if (old != null)
                            log.warn("Not mutually exclusive? {} vs {}", old.parentRange, next.parentRange)
                    },
                    NavigableMap<Range<*>, Stratum<PK>>::putAll)
            children = map
        }

        fun valueNode(bin: Range<*>, nodeSplitter: (Stratum<PK>) -> Unit): Stratum<PK> =
            children!!.computeIfAbsent(bin) {
                val prev: Range<*>? = children!!.lowerKey(bin)
                val i: Int = if (prev == null)
                    bounds[0]
                else
                    children!![prev]!!.bounds[1]
                val result = Stratum(this, bin, intArrayOf(i, i)) // initialize empty
                nodeSplitter(result)
                result
            }

        companion object {

            private fun toBounds(
                bounds: IntArray,
                splitKeys: IntArray, i: Int
            ): IntArray =
                intArrayOf(
                    if (i == 0) bounds[0] else splitKeys[i - 1],
                    if (i == splitKeys.size) bounds[1] else splitKeys[i]
                )

            private fun <V: Comparable<*>> toRange(points: List<V>, i: Int): Range<V> {
                val min = if (i == 0) null else points[i - 1]
                val max = if (i == points.size) null else points[i]
                return Range(min, i != 0, max, false)
            }
        }
    }
}