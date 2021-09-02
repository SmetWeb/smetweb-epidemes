package io.smetweb.math

import io.smetweb.log.getLogger
import java.util.*
import java.util.function.*
import java.util.stream.IntStream
import java.util.stream.Stream

/**
 * [TablePartitioner] maintains an ordered index partition of strata
 * based on (a [hierarchy] of) one [Table.Property] or more (iff its value type is [Comparable]),
 * having at least one [Stratum] per [Delimiters] (outer bounds are infinite)
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
    val root: Stratum<PK>
    private val hierarchy: MutableList<Delimiters<*>> = mutableListOf()
    private val keys: MutableList<PK> = Collections.synchronizedList(table.keys.toMutableList())
    private val log = getLogger()

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
            val nameStr = root.limits!!.toString().let { it.substring(0, 7.coerceAtMost(it.length)) }
            val keyStr = (if (n < 6)
                keys.toList()
            else
                listOf(keys[0], keys[1], null, keys[n - 2], keys[n - 1]))
                .joinToString(", ") {
                    if (it == null) ".." else "$it=${evaluator(root.limits!!, it)}"
                }
            buildString {
                append(nameStr, "{", keyStr, "} ", root.toString())
            }
        } catch (e: Exception) {
            onError(IllegalStateException("Unable to render string of partition: " + e.message, e))
            ""
        }

//    fun index() = this.root.indexKeyStream()

    fun <P: Table.Property<V>, V: Comparable<*>> stratify(
        property: Class<P>,
        boundaries: Iterable<V> = emptyList(),
        valueComparator: Comparator<V> = Comparator.naturalOrder(),
    ): TablePartitioner<PK> {
        val delimiters = Delimiters(property, boundaries, valueComparator)
        hierarchy.add(delimiters)
        root.split(delimiters, ::stratifier, { evaluator(delimiters, it) }, ::nodeSplitter)
        return this
    }

//    private fun findStratum(vararg sampleValues: Comparable<*>): Stratum<PK> {
//        if (root.isEmpty || sampleValues.isEmpty())
//            return root
//
//        var result: Stratum<PK> = this.root
//        for (value: Comparable<*> in sampleValues) {
//            if (result.stratification == null)
//                error("Filter values ${sampleValues.contentToString()} exceed available groupings: $hierarchy")
//            val range = if (value is Range<*>) value else Range(value)
//            result = if (result.stratification!!.splitOrder.isEmpty()) // walk (value) branch
//                result.valueNode(range, ::nodeSplitter)
//            else if (result.children != null) // walk (sub-range) branch
//                result.children!!.floorEntry(range).value
//            else
//                error("Unexpected, no match for filters: " + sampleValues.contentToString())
//        }
//        return result
//    }

//    fun keys(vararg valueFilter: Comparable<*>): List<Any> =
//        if (root.isEmpty)
//            emptyList()
//        else
//            stratifier(findStratum(*valueFilter).bounds).toList()

    fun nearestKeys(
        defaultOnDeviation: (Class<out Table.Property<*>>, Range<*>) -> Boolean,
        vararg strata: Comparable<*>?
    ): List<PK> {
        if (root.isEmpty)
            return Collections.emptyList()

        if (strata.isEmpty())
            return Collections.unmodifiableList(keys)

        // walk the hierarchy
        var stratum: Stratum<PK> = this.root
        var childEntry: Map.Entry<Range<*>, Stratum<PK>>?
        for (value: Comparable<*>? in strata) {
            val valueRange: Range<*>

            if (value is Range<*>) {
                valueRange = value
                // TODO merge node-bins if value-range contains multiple strata
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
                if (!defaultOnDeviation(stratum.limits!!.propertyType, range))
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
                        val key = stratum.parent!!.limits!!.propertyType
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
    fun <V: Comparable<*>> evaluator(strat: Delimiters<V>, key: PK): V? =
        table.select(key)?.get(strat.propertyType) as V?

    private fun <V: Comparable<*>> split(stratum: Stratum<PK>, strat: Delimiters<V>) {
        stratum.split(strat, ::stratifier, { key -> evaluator(strat, key) }, ::nodeSplitter)
        if (validation)
            validate()
    }

    private fun nodeSplitter(stratum: Stratum<PK>) {
        var passed = false
        for (i in 0 until hierarchy.size - 1) {
            if (!passed && hierarchy[i] === stratum.parent!!.limits)
                passed = true
            if (passed)
                split(stratum, hierarchy[i + 1])
        }
    }

    fun strataFor(prop: Class<out Table.Property<*>>, value: Comparable<*>): List<Pair<Range<*>, Stratum<PK>>> =
        this.root.strataFor(prop, value).toList()

    data class Delimiters<V: Comparable<*>> internal constructor(
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
            propertyType.simpleName
    }

    /**
     * [Stratum] helper class to build the partition-tree
     */
    class Stratum<PK> internal constructor(
        val parent: Stratum<PK>? = null, // null == root
        val parentRange: Range<*>? = null,  // null == root
        indexRange: IntArray,
        var limits: Delimiters<*>? = null // null == leaf
    ) {
        val bounds: IntArray = indexRange.copyOf()
        var children: NavigableMap<Range<*>, Stratum<PK>>? = null // null = leaf
        private val log = getLogger()

        override fun toString(): String {
            val n = bounds[1] - bounds[0]
            return children?.let {
                // branch
                val branchStr = it.entries.stream()
                    .map { (bin, node) ->
//                        if (node.isEmpty) // category key
//                            ""
//                        else
                            ((if (limits!!.splitOrder.isEmpty()) // intermediate range
                                " '${bin.lower.value}':"
                            else if (bin.lowerFinite()) // lowest range
                                (" < '${bin.lower.value}' =<")
                            else
                                "")
                                    + " " + node) // value(s): leaf or branch
                    }
                    .reduce { l, r -> l + r }
                    .orElse("")
                buildString {
                    append("{", branchStr, "}")
                }
            }
            // leaf
                ?: buildString {
                    if (n > 2)
                        append(n, "x")
                    append("#[")
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

//        fun indexKeyStream(): IntStream =
//            children
//                // recurse
//                ?.values?.stream()?.flatMapToInt(Stratum<PK>::indexKeyStream)
//                // stop
//                ?: IntStream.range(bounds[0], bounds[1])

        fun resize(
            tuple: Table.Tuple<*>,
            delta: Int,
            leafHandler: (Range<*>, Stratum<PK>) -> Boolean,
            nodeSplitter: (Stratum<PK>) -> Unit
        ) {
            if(limits == null)
                error("Missing dimension?")

            val value = (tuple[limits!!.propertyType]
                ?: error("$tuple has no value for $limits")) as Comparable<*>
            if (limits!!.splitOrder.isEmpty()) {
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
                    ?: error("Unexpected, $limits: $value -> $bin < " + children!!.firstKey())
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
                    log.warn("leaf not adjusted: {}", bin)
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

        fun <V: Comparable<*>> strataFor(prop: Class<out Table.Property<*>>, value: V): MutableList<Pair<Range<*>, Stratum<PK>>> =
            children?.entries
                ?.flatMapTo(mutableListOf()) { (bin: Range<*>, child: Stratum<PK>) ->
                    // recurse on children
                    val childStrata = child.strataFor(prop, value)
                    @Suppress("UNCHECKED_CAST")
                    return@flatMapTo if (childStrata.isNotEmpty())
                        // some child stratum's range contains given property/value: return these first
                        childStrata
                    else if (this.limits!!.propertyType == prop && (bin as Range<V>).contains(value))
                        // current child stratum's range contains given property/value: return new path from this node
                        listOf(Pair(bin, child))
                    else
                        listOf()
                }
                ?: mutableListOf()

        fun invalidChildren(tupleFetcher: (Int) -> Table.Tuple<*>): IntStream =
                children?.entries?.stream()
                    // recurse on children
                    ?.flatMapToInt { (bin: Range<*>, child: Stratum<PK>) ->
                        // sort once
                        val invalid: IntArray = child.invalidChildren(tupleFetcher).sorted().toArray()
                        IntStream.range(child.bounds[0], child.bounds[1])
                            // invalid: child(ren) invalid
                            .filter { Arrays.binarySearch(invalid, it) >= 0 }
                            // invalid: value out of range
                            .filter {
                                val tuple = tupleFetcher(it)
                                val value = tuple[limits!!.propertyType] ?: error("No value for $limits in $tuple")
                                !value.isIn(bin)
                            }
                    }
                    ?: IntStream.empty()

        @Suppress("UNCHECKED_CAST")
        fun <V: Comparable<*>> split(
            delimiters: Delimiters<V>,
            partitioner: (IntArray) -> MutableList<PK>,
            valueSupplier: (PK) -> V?,
            nodeSplitter: (Stratum<PK>) -> Unit
        ) {
            if (children != null) // reached leaf node
            {
                children!!.values.forEach { it.split(delimiters, partitioner, valueSupplier, nodeSplitter ) }
                return
            }
            // provide the dimension info to each affected leaf
            this.limits = delimiters
            // sort node key-partition using given property value comparator
            val partition = partitioner(bounds)
            try {
                partition.sortWith { k1, k2 -> delimiters.valueComparator.compare(valueSupplier(k1), valueSupplier(k2)) }
            } catch (e: NullPointerException) {
                error("Missing value(s) for $delimiters")
            }
            if (delimiters.splitOrder.isEmpty()) {
                // split points empty? add all distinct values as split point
                children = TreeMap() // infinity placeholder range: Range.infinite() to new PartitionNode( this, this.bounds )
                if (partition.isEmpty())
                    return
                var v: V = valueSupplier(partition[0])!!
                var vNode = valueNode(Range(v), nodeSplitter)
                var offset = bounds[0]
                for (i in 1 until partition.size) {
                    val v2: V = valueSupplier(partition[i])!!
                    if (delimiters.valueComparator.compare(v2, v) != 0) {
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
            val splitKeys = IntArray(delimiters.splitOrder.size)
            var splitIndex = 0
            for(i in 0 until delimiters.splitOrder.size) {
                val splitValue = delimiters.splitOrder[i]
                while (splitIndex < partition.size && delimiters.valueComparator.compare(valueSupplier(partition[splitIndex]), splitValue) < 0)
                    splitIndex++
                splitKeys[i] = bounds[0] + splitIndex
            }

            // map split points to respective sub-partition bounds
            val map = IntStream.range(0, delimiters.splitOrder.size + 1)
                .collect(
                    // create new partition's range-bounds mapping
                    { TreeMap { r1, r2 -> Range.compare(r1 as Range<V>, r2 as Range<V>, delimiters.valueComparator) } },
                    // add split node (value range and key bounds)
                    { map, j: Int ->
                        val range: Range<*> = toRange(delimiters.splitOrder, j)
                        val next = Stratum(parent = this, parentRange = range,
                            indexRange = toBounds(bounds, splitKeys, j), limits = delimiters)
                        val old: Stratum<PK>? = map.put(range, next)
                        if (old != null)
                            log.warn("Not mutually exclusive? {} vs {}", old.parentRange, next.parentRange)
                    },
                    NavigableMap<Range<*>, Stratum<PK>>::putAll)
            children = map
        }

        private fun valueNode(bin: Range<*>, nodeSplitter: (Stratum<PK>) -> Unit): Stratum<PK> =
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