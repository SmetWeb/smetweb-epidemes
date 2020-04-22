package io.smetweb.fact

import io.smetweb.ref.NameRef
import io.smetweb.ref.IdRef
import java.lang.IllegalArgumentException

/**
 * [ResultExchange] defines the context of (multiple) [CoordinationFact]s
 * occurring in a transaction pattern with regards to a single
 * identifiable result of some [ResultKind] (e.g. a SALE kind)
 * and a specific [getExecutorRef] and [getInitiatorRef] (e.g. JOE and JANE)
 * each conforming to the [ResultKind]'s respective
 * executor and initiator [ActorKind]s (e.g. the SELLER and BUYER kinds)
 */
interface ResultExchange {

	fun getId(): IdRef<*, *>

	fun getResultKind(): ResultKind

	fun getExecutorRef(): NameRef

	fun getInitiatorRef(): NameRef = getExecutorRef()

	// helper functions

	fun getRootRef(): Comparable<*> = getExecutorRef().getRootRef()

	fun isSelfInitiated() = getInitiatorRef() == getExecutorRef()

	fun participantRef(role: RoleKind): NameRef? =
			when(role) {
				RoleKind.INITIATOR -> getInitiatorRef()
				RoleKind.EXECUTOR -> getExecutorRef()
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
		getInitiatorRef() -> getExecutorRef()
		getExecutorRef() -> getInitiatorRef()
		else -> throw IllegalArgumentException("Not a participant: $name in $this")
	}

}