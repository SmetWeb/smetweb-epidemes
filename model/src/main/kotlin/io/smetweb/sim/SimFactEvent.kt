package io.smetweb.sim

import io.smetweb.ref.IdRef
import io.smetweb.uuid.UuidNameRef
import io.smetweb.time.TimeRef
import io.smetweb.ref.IdRef.IntRef
import io.smetweb.ref.Ref
import io.smetweb.fact.*
import io.smetweb.uuid.UuidRef
import java.util.*

data class SimFactEvent(
		private val id: UuidRef = UuidRef(),
		private val kind: FactKind = FactKind.REQUESTED,
		private val exchange: SimFactExchange,
		private val ordinal: IntRef = IntRef(),
		private val occur: TimeRef,
		private val links: Collection<FactLink> = emptySet(),
		private val details: Map<in IdRef<*, *>, Ref<*>> = emptyMap()
): CoordinationFact {

	constructor(
			id: UuidRef = UuidRef(),
			kind: FactKind = FactKind.REQUESTED,
			occur: TimeRef,
			resultKind: ResultKind,
			executorRef: UuidNameRef,
			initiatorRef: UuidNameRef = executorRef,
			ordinal: IntRef = IntRef(),
			links: Collection<FactLink> = emptySet(),
			details: Map<in IdRef<*, *>, Ref<*>> = emptyMap(),
			exchange: SimFactExchange = SimFactExchange(
					resultKind = resultKind,
					initiatorRef = initiatorRef,
					executorRef = executorRef)
	): this(
			id = id,
			kind = kind,
			exchange = exchange,
			ordinal = ordinal,
			occur = occur,
			links = links,
			details = details)

	override fun getId(): UuidRef = this.id

	override fun getKind(): FactKind = this.kind

	override fun getExchange(): SimFactExchange = this.exchange

	override fun getOrdinal(): IntRef = this.ordinal

	override fun getOccur(): TimeRef = this.occur

	override fun getLinks(): Collection<FactLink> = this.links

	override fun getDetails(): Map<in IdRef<*, *>, Ref<*>> = this.details

	override fun getRootRef(): UUID = super.getRootRef() as UUID
}
