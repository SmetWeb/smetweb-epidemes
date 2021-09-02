package io.smetweb.math

class HashMapTable<PK: Any>(
    override val properties: List<Class<out Property<*>>>,
    keyGen: () -> PK,
    map: MutableMap<PK, Tuple<PK>> = mutableMapOf()
): Table<PK>(
    properties = properties,
    counter = { map.size.toLong() },
    printer = map::toString,
    cleaner = map::clear,
    indexer = { map.keys },
    retriever = map::get,
    remover = { map.remove(it) },
    inserter = { HashMapTuple(key = keyGen(), properties = properties).apply { map[this.key] = this } }
) {

    private class HashMapTuple<PK: Any>(
        private val map: MutableMap<Class<out Property<*>>, Any?> = mutableMapOf(),
        override val key: PK,
        override val properties: List<Class<out Property<*>>>,
    ): Table.Tuple<PK>(
        key = key,
        properties = properties,
        getter = map::get, // TODO add property validity checks
        setter = map::put // TODO add property validity checks
    )
}
