package io.smetweb.sim.event

import io.smetweb.ref.IdRef
import io.smetweb.uuid.UuidNameRef
import io.smetweb.time.TimeRef
import io.smetweb.ref.IdRef.IntRef
import io.smetweb.ref.Ref
import io.smetweb.fact.*
import io.smetweb.uuid.UuidRef
import java.util.*

data class UuidFact(
		override val id: UuidRef = UuidRef(),
		override val kind: FactKind = FactKind.REQUESTED,
		override val exchange: UuidResultExchange,
		override val ordinal: IntRef = IntRef(),
		override val occur: TimeRef,
		override val links: Collection<FactLink> = emptySet(),
		override val details: Map<in IdRef<*, *>, Ref<*>> = emptyMap()
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
			exchange: UuidResultExchange = UuidResultExchange(
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

	override fun rootRef(): UUID = super.rootRef() as UUID
}
