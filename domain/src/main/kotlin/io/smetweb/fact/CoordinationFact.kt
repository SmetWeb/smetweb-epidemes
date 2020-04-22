package io.smetweb.fact

import io.smetweb.ref.NameRef
import io.smetweb.ref.IdRef
import io.smetweb.ref.Ref
import io.smetweb.time.TimeRef

interface CoordinationFact {

	fun getId(): IdRef<*, *>

	fun getKind(): FactKind

	fun getExchange(): ResultExchange

	/** monotonous within the exchange */
	fun getOrdinal(): IdRef<*, *>

	fun getOccur(): TimeRef

	fun getLinks(): Collection<FactLink>

	fun getDetails(): Map<in IdRef<*, *>, Ref<*>>

	// helper functions

	fun getRootRef(): Comparable<*> = getExchange().getRootRef()

	/**
	 * @return the [NameRef] referencing the actor that produced this [CoordinationFact]
	 */
	fun creatorRef(): NameRef? = getKind().creatorRole?.let(getExchange()::participantRef)

	/**
	 * @return the [NameRef] referencing the actor that would respond
	 * to this kind of [CoordinationFact] in this [ResultExchange] exchange, or `null` for either
	 */
	fun responderRef(): NameRef? = getKind().responderRole?.let(getExchange()::participantRef)

}