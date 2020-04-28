package io.smetweb.sim.event

import io.smetweb.fact.ResultExchange
import io.smetweb.fact.ResultKind
import io.smetweb.uuid.UuidNameRef
import io.smetweb.uuid.UuidRef
import java.util.*

/**
 * [UuidResultExchange] helps to [id] or track and trace
 * all coordination [UuidFact]s in some exchange between one or two [participantRef]s,
 * i.e. the transaction's [executorRef], and its [initiatorRef],
 * (possibly identical when [selfInitiated])
 * for producing one specific [resultKind]
 */
data class UuidResultExchange (
		override val id: UuidRef = UuidRef(),
		override val resultKind: ResultKind,
		override val executorRef: UuidNameRef,
		override val initiatorRef: UuidNameRef = executorRef
): ResultExchange {

	override fun rootRef(): UUID = super.rootRef() as UUID

	override fun toString(): String = "${this.resultKind.nameRef().get()}" +
			"#${Integer.toHexString(this.id.get().hashCode())}" +
			"[${this.initiatorRef.get()}->${this.executorRef.get()}]" +
			"@${Integer.toHexString(this.rootRef().hashCode())}"
}
