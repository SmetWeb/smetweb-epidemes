package io.smetweb.epidemes.situ

import io.smetweb.math.Table

internal interface Regions {

    class RegionName (value: String): Table.Property<String>(value)

    class ParentRef(value: Comparable<*>): Table.Property<Comparable<*>>(value)

    class Population(value: Long): Table.Property<Long>(value)

    companion object {

        @JvmStatic
        val PROPERTIES: List<Class<out Table.Property<*>>> = listOf(
            RegionName::class.java,
            ParentRef::class.java,
            Population::class.java,
        )
    }
}