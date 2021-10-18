package io.smetweb.epidemes.data.cbs

import io.smetweb.epidemes.deme.ConnectionType
import io.smetweb.epidemes.deme.HouseholdComposition

/**
 * [CBSHouseholdComposition] is a JSON encoding of CBS` [HouseholdComposition]
 */
enum class CBSHouseholdComposition(
    val jsonKey: String,
    override val isAggregate: Boolean,
    override val adultCount: Int,
    override val registered: Boolean,
    override val childCount: Int,
    override val orMoreKids: Boolean,
    /** the referent relation type */
    val relationType: ConnectionType?,
    /** @return the (minimum) household size */
    val size: Int = adultCount + childCount,
) : HouseholdComposition {
    /**
     * total households == `hh_l` + `hh_p_0` + `hh_p_1+` == `hh_l` + `hh_b` +
     * `hh_m` + `hh_s` + `hh_o`
     */
    TOTAL("hh", true, 0, false, 0, true, null),

    /** total multiple adults with no kids == `hh_b_0` + `hh_m_0` + `hh_o`  */
    POLY_NOKIDS("hh_p_0", true, 2, false, 0, false, CBSConnectionType.PARTNER),

    /**
     * total multiple adults with 1+ kids ==
     * `hh_b_1`+`hh_b_2`+`hh_b_3+`+`hh_m_1`+`hh_m_2`+`hh_m_3+`+`hh_s_1`+`hh_s_2`+`hh_s_3+`
     */
    POLY_1PLUSKIDS("hh_p_1p", true, 2, false, 1, true, CBSConnectionType.PARTNER),

    /** single adult, no kids  */
    SOLO_NOKIDS("hh_l", false, 1, false, 0, false, CBSConnectionType.SINGLE),

    /** total single parent with 1+ [step/adopted] kids  */
    SOLO_1PLUSKIDS("hh_s", true, 1, false, 1, true, CBSConnectionType.SINGLE),

    /** single parent with 1 [step/adopted] kid  */
    SOLO_1KID("hh_s_1", false, 1, false, 1, false, CBSConnectionType.SINGLE),

    /** single parent with 2 [step/adopted] kids  */
    SOLO_2KIDS("hh_s_2", false, 1, false, 2, false, CBSConnectionType.SINGLE),

    /** single parent with >=3 [step/adopted] kids  */
    SOLO_3PLUSKIDS("hh_s_3p", false, 1, false, 3, true, CBSConnectionType.SINGLE),

    /** total unregistered couples == `hh_b_0`+`hh_b_1`+`hh_b_2`+`hh_b_3+`  */
    DUO_0PLUSKIDS("hh_b", true, 2, false, 0, true, CBSConnectionType.PARTNER),

    /** unregistered couple with no kids  */
    DUO_NOKIDS("hh_b_0", false, 2, false, 0, false, CBSConnectionType.PARTNER),

    /** unregistered couple with one [step/adopted] kid  */
    DUO_1KID("hh_b_1", false, 2, false, 1, false, CBSConnectionType.PARTNER),

    /** unregistered couple with 2 [step/adopted] kids  */
    DUO_2KIDS("hh_b_2", false, 2, false, 2, false, CBSConnectionType.PARTNER),

    /** unregistered couple with >=3 [step/adopted] kids  */
    DUO_3PLUSKIDS("hh_b_3p", false, 2, false, 3, true, CBSConnectionType.PARTNER),

    /**
     * total registered (married/cohabiting) couples ==
     * `hh_m_0`+`hh_m_1`+`hh_m_2`+`hh_m_3+`
     */
    REGDUO_0PLUS("hh_m", true, 2, true, 0, true, CBSConnectionType.PARTNER),

    /** registered (married/cohabiting) couple with no kids  */
    REGDUO_NOKIDS("hh_m_0", false, 2, true, 0, false, CBSConnectionType.PARTNER),

    /** registered (married/cohabiting) couple with 1 [step/adopted] kid  */
    REGDUO_1KID("hh_m_1", false, 2, true, 1, false, CBSConnectionType.PARTNER),

    /** registered (married/cohabiting) couple with 2 [step/adopted] kids  */
    REGDUO_2KIDS("hh_m_2", false, 2, true, 2, false, CBSConnectionType.PARTNER),

    /** registered (married/cohabiting) couple and 3+ [step/adopted] kids  */
    REGDUO_3PLUSKIDS("hh_m_3p", false, 2, true, 3, true, CBSConnectionType.PARTNER),

    /** total other private compositions (siblings, boarder, fosters, ...)  */
    OTHER("hh_o", true, 0, false, 0, true, CBSConnectionType.WARD);

    //	public static CBSHousehold of( final Domestic<?> household )
    //	{
    //		final long adultCount = household.getMembers().values().stream()
    //				.filter( m -> m.relationType().isInformative() ).count(),
    //				childCount = household.getMembers().size() - adultCount;
    //		// check if registered couple is still together
    //		final boolean registered = household.getComposition().registered()
    //				&& adultCount == 2;
    //		for( CBSHousehold hhType : values() )
    //			if(
    //			// skip aggregates
    //			!hhType.aggregate()
    //					// match registered
    //					&& hhType.registered() == registered
    //					// match adult count
    //					&& hhType.adultCount() == adultCount
    //					// match child count exactly or range
    //					&& (hhType.childCount() == childCount || (hhType.more()
    //							&& hhType.childCount() < childCount)) )
    //				return hhType;
    //		return Thrower.throwNew( IllegalArgumentException::new,
    //				() -> "Unknown composition for " + household.getMembers()
    //						+ ", adults: " + adultCount + ", children: "
    //						+ childCount + ", registered: " + registered );
    //	}

    override fun plusAdult(): CBSHouseholdComposition =
        when (this) {
            SOLO_NOKIDS -> DUO_NOKIDS
            SOLO_1KID -> DUO_1KID
            SOLO_2KIDS -> DUO_2KIDS
            SOLO_3PLUSKIDS -> DUO_3PLUSKIDS
            else -> error("Can't add adult to $name")
        }

    override fun minusAdult(): CBSHouseholdComposition =
        when (this) {
            REGDUO_NOKIDS, DUO_NOKIDS -> SOLO_NOKIDS
            REGDUO_1KID, DUO_1KID -> SOLO_1KID
            REGDUO_2KIDS, DUO_2KIDS -> SOLO_2KIDS
            REGDUO_3PLUSKIDS, DUO_3PLUSKIDS -> SOLO_3PLUSKIDS
            else -> error("Can't remove adult from $name")
        }

    override fun plusChild(): CBSHouseholdComposition =
        when (this) {
            SOLO_NOKIDS -> SOLO_1KID
            SOLO_1KID -> SOLO_2KIDS
            SOLO_2KIDS, SOLO_3PLUSKIDS -> SOLO_3PLUSKIDS
            DUO_NOKIDS -> DUO_1KID
            DUO_1KID -> DUO_2KIDS
            DUO_2KIDS, DUO_3PLUSKIDS -> DUO_3PLUSKIDS
            REGDUO_NOKIDS -> REGDUO_1KID
            REGDUO_1KID -> REGDUO_2KIDS
            REGDUO_2KIDS, REGDUO_3PLUSKIDS -> REGDUO_3PLUSKIDS
            POLY_NOKIDS -> POLY_1PLUSKIDS
            else -> error("Can't add child to $name")
        }

    override fun minusChild(): CBSHouseholdComposition =
        when (this) {
            SOLO_1KID -> SOLO_NOKIDS
            SOLO_2KIDS -> SOLO_1KID
            SOLO_3PLUSKIDS -> SOLO_2KIDS
            DUO_1KID -> DUO_NOKIDS
            DUO_2KIDS -> DUO_1KID
            DUO_3PLUSKIDS -> DUO_2KIDS
            REGDUO_1KID -> REGDUO_NOKIDS
            REGDUO_2KIDS -> REGDUO_1KID
            REGDUO_3PLUSKIDS -> REGDUO_2KIDS

            SOLO_NOKIDS, DUO_NOKIDS, REGDUO_NOKIDS, POLY_NOKIDS ->
                error("No child to remove from $name")

            else -> error("Undefined composition in $name")
        }
}