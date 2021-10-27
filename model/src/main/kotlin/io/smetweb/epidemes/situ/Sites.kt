package io.smetweb.epidemes.situ

import io.smetweb.epidemes.data.duo.DuoPedagogy
import io.smetweb.math.Table

internal interface Sites {

    class SiteName(value: String): Table.Property<String>(value)

    class RegionRef(value: Comparable<*>): Table.Property<Comparable<*>>(value)

    class Latitude(value: Double): Table.Property<Double>(value)

    class Longitude(value: Double): Table.Property<Double>(value)

    class SiteFunction(value: BuiltFunction): Table.Property<BuiltFunction>(value)

    class EduCulture(value: DuoPedagogy): Table.Property<DuoPedagogy>(value)

    class Capacity(value: Int): Table.Property<Int>(value)

    class Occupancy(value: Int): Table.Property<Int>(value)

    class Pressure(value: Double): Table.Property<Double>(value)

    companion object {

        @JvmStatic
        val PROPERTIES: List<Class<out Table.Property<*>>> = listOf(
            Pressure::class.java,
            Occupancy::class.java,
            SiteName::class.java,
            SiteFunction::class.java,
            EduCulture::class.java,
            RegionRef::class.java,
            Latitude::class.java,
            Longitude::class.java,
            Capacity::class.java
        )
    }
}