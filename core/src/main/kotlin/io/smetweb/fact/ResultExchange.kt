package io.smetweb.fact

import io.smetweb.refer.NameRef
import io.smetweb.refer.IdRef
import java.lang.IllegalArgumentException

/**
 * [ResultExchange] defines the context of (multiple) [CoordinationFact]s
 * occurring in a transaction pattern with regards to a single
 * identifiable result of some [ResultKind] (e.g. a SALE kind)
 * and a specific [executorRef] and [initiatorRef] (e.g. JOE and JANE)
 * each conforming to the [ResultKind]'s respective
 * executor and initiator [ActorKind]s (e.g. the SELLER and BUYER kinds)
 */
interface ResultExchange {

	val id: IdRef<*, *>

	val resultKind: ResultKind

	val executorRef: NameRef

	val initiatorRef: NameRef
		get() = this.executorRef

	// helper functions

	fun rootRef(): Comparable<*> = this.executorRef.rootRef()

	fun selfInitiated(): Boolean = this.initiatorRef == this.executorRef

	fun participantRef(role: RoleKind): NameRef? =
			when(role) {
				RoleKind.INITIATOR -> initiatorRef
				RoleKind.EXECUTOR -> executorRef
			}

	/**
	 * @param factKind a fact kind or exchange step
	 * @return the [NameRef] referencing the actor that would create given fact kind in this [ResultExchange] exchange,
	 * or `null` for either
	 */
	fun creatorRef(factKind: FactKind) = factKind.creatorRole?.let(this::participantRef)

	/**
	 * @param factKind a fact kind or exchange step
	 * @return the [NameRef] referencing the actor that would respond to given fact kind in this [ResultExchange] exchange,
	 * or `null` for either
	 */
	fun responderRef(factKind: FactKind) = factKind.responderRole?.let(this::participantRef)

	fun oppositeRef(name: NameRef): NameRef = when(name) {
		this.initiatorRef -> this.executorRef
		this.executorRef -> this.initiatorRef
		else -> throw IllegalArgumentException("Not a participant: $name in $this")
	}

}