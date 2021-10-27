package io.smetweb.epidemes.deme

import io.smetweb.epidemes.medi.infect.MSEIRSCompartment
import io.smetweb.math.Table
import java.math.BigDecimal

internal interface Persons {

    /** [PersonSeq] tracks a person instance through time (likely different from database index key, if any) */
    class PersonSeq(value: Long): Table.Property<Long>(value)

    class CultureRef(value: Comparable<*>): Table.Property<Comparable<*>>(value)

    class HouseholdRef(value: Comparable<*>): Table.Property<Comparable<*>>(value)

    class HouseholdRank(value: HouseholdPosition): Table.Property<HouseholdPosition>(value)

    class Male(value: Boolean): Table.Property<Boolean>(value)

    class Birth(value: BigDecimal): Table.Property<BigDecimal>(value)

    class HomeRegionRef(value: Comparable<*>): Table.Property<Comparable<*>>(value)

    class HomeSiteRef(value: Comparable<*>): Table.Property<Comparable<*>>(value)

    class PathogenCompartment(value: MSEIRSCompartment): Table.Property<MSEIRSCompartment>(value)

    class PathogenResistance(value: Double): Table.Property<Double>(value)

    class VaxCompliance(value: Int): Table.Property<Int>(value)

    companion object {

        @JvmStatic
        val PROPERTIES: List<Class<out Table.Property<*>>> = listOf(
            PathogenCompartment::class.java,
            HouseholdRef::class.java,
            HouseholdRank::class.java,
            Birth::class.java,
            HomeRegionRef::class.java,
            HomeSiteRef::class.java,
            PathogenResistance::class.java,
            Male::class.java,
            CultureRef::class.java,
            VaxCompliance::class.java,
            PersonSeq::class.java
        )
    }
}