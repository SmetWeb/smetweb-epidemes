package io.smetweb.fact

import io.smetweb.refer.NameRef
import io.smetweb.refer.IdRef
import io.smetweb.refer.Ref
import io.smetweb.time.TimeRef

interface CoordinationFact {

	val id: IdRef<*, *>

	val kind: FactKind

	val exchange: ResultExchange

	/** monotonous within the exchange */
	val ordinal: IdRef<*, *>

	val occur: TimeRef

	val links: Collection<FactLink>

	val details: Map<in IdRef<*, *>, Ref<*>>

	// helper functions

	fun rootRef(): Comparable<*> = exchange.rootRef()

	/**
	 * @return the [NameRef] referencing the actor that produced this [CoordinationFact]
	 */
	fun creatorRef(): NameRef? = kind.creatorRole?.let(exchange::participantRef)

	/**
	 * @return the [NameRef] referencing the actor that would respond
	 * to this kind of [CoordinationFact] in this [ResultExchange] exchange, or `null` for either
	 */
	fun responderRef(): NameRef? = kind.responderRole?.let(exchange::participantRef)

}