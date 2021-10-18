package io.smetweb.epidemes.deme

interface HouseholdComposition {

    /** @return `true` for aggregates */
    val isAggregate: Boolean

    /** @return the number of adults incl. (step/adoptive) parents */
    val adultCount: Int

    /** @return `true` for a registered (married/cohabiting) couple */
    fun isCouple(): Boolean =
        adultCount == 2

    /** @return `true` for a registered (married/cohabiting) couple */
    val registered: Boolean

    /** @return the number of children, incl. step/adopted, excl. foster */
    val childCount: Int

    /** @return `true` if more than [childCount] kids are allowed */
    val orMoreKids: Boolean

    /** @return the new [HouseholdComposition] */
    fun plusAdult(): HouseholdComposition

    /** @return the new [HouseholdComposition] */
    fun minusAdult(): HouseholdComposition

    /** @return the new [HouseholdComposition] */
    fun plusChild(): HouseholdComposition

    /** @return the new [HouseholdComposition] */
    fun minusChild(): HouseholdComposition
}