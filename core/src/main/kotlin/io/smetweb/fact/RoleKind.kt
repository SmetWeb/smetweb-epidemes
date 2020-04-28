package io.smetweb.fact

/**
 * [RoleKind] defines the two types of role a participant [ActorKind]
 * of some [ResultExchange] can have when coordinating the production of some [ResultKind]
 */
enum class RoleKind {
	INITIATOR,
	EXECUTOR;

	fun opposite() =
			if (this == EXECUTOR)
                INITIATOR
			else
                EXECUTOR
}
