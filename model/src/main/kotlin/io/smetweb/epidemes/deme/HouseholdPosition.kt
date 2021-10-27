package io.smetweb.epidemes.deme

/**
 * [HouseholdPosition] represents a person's rank within a (monogamous) household
 */
enum class HouseholdPosition(val adult: Boolean = false) {

	REFERENT(true),
	PARTNER(true),
	CHILD1,
	CHILD2,
	CHILD3,
	CHILDMORE;

	/** @return the new [HouseholdPosition] for this one with given [removedMember] */
	fun shift(removedMember: HouseholdPosition): HouseholdPosition
	{
		return if (removedMember == REFERENT) {
			if (this == PARTNER)
				REFERENT
			else
				this // kids position stays the same
		} else if (!adult && ordinal > removedMember.ordinal)
			values()[ordinal - 1] // shift left
		else
			this // position stays the same for referent or children with lower rank values than the [removedMember]
	}

	companion object {

		/** @return the [HouseholdPosition] for child of given [rank] >= 0, where: 0 = first, 1 = second, etc. */
		@JvmStatic
		fun ofChildIndex(rank: Int): HouseholdPosition =
			if (rank < 0)
				error("Child rank must be 0 or greater, but was: $rank")
			else if (rank < values().size - CHILD1.ordinal)
				values()[CHILD1.ordinal + rank]
			else
				CHILDMORE
	}
}