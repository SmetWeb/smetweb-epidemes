package io.smetweb.epidemes.deme

import io.smetweb.epidemes.data.cbs.CBSBirthRank
import io.smetweb.epidemes.data.cbs.CBSHouseholdComposition
import io.smetweb.epidemes.data.duo.DuoPedagogy
import io.smetweb.math.Table
import java.math.BigDecimal

internal interface Households {

    /** [HouseholdSeq] tracks households through time (index key is data source dependent) */
    class HouseholdSeq(value: Long) : Table.Property<Long>(value)
    class EduCulture(value: DuoPedagogy) : Table.Property<DuoPedagogy>(value)
    class Complacency(value: BigDecimal) : Table.Property<BigDecimal>(value)
    class Confidence(value: BigDecimal) : Table.Property<BigDecimal>(value)
    class HomeRegionRef(value: Comparable<*>) : Table.Property<Comparable<*>>(value)
    class HomeSiteRef(value: Comparable<*>) : Table.Property<Comparable<*>>(value)
    class Composition(value: CBSHouseholdComposition) : Table.Property<CBSHouseholdComposition>(value)
    class KidRank(value: CBSBirthRank) : Table.Property<CBSBirthRank>(value)
    class ReferentBirth(value: BigDecimal) : Table.Property<BigDecimal>(value)
    class MomBirth(value: BigDecimal) : Table.Property<BigDecimal>(value)

    companion object {
        val NO_MOM = BigDecimal.TEN.pow(6)
        val PROPERTIES: List<Class<out Table.Property<*>>> = listOf(
            HomeRegionRef::class.java,
            HomeSiteRef::class.java,
            Composition::class.java,
            KidRank::class.java,
            ReferentBirth::class.java,
            MomBirth::class.java,
            EduCulture::class.java,
            HouseholdSeq::class.java,
            Complacency::class.java,
            Confidence::class.java
        )
    }
}