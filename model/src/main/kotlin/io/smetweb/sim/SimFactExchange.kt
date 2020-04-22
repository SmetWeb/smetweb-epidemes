package io.smetweb.sim

import io.smetweb.ref.NameRef
import io.smetweb.fact.ResultExchange
import io.smetweb.fact.ResultKind
import io.smetweb.uuid.UuidNameRef
import io.smetweb.uuid.UuidRef
import java.util.*

/**
 * [SimFactExchange] helps to [id] or track and trace
 * all coordination [SimFactEvent]s in some exchange between one or two [participantRef]s,
 * i.e. the transaction's [executorRef], and its [initiatorRef],
 * (possibly identical when [isSelfInitiated])
 * for producing one specific [resultKind]
 */
data class SimFactExchange (
		private val id: UuidRef = UuidRef(),
		private val resultKind: ResultKind,
		private val executorRef: UuidNameRef,
		private val initiatorRef: UuidNameRef = executorRef
): ResultExchange {

	override fun getId(): UuidRef = this.id

	override fun getResultKind(): ResultKind = this.resultKind

	override fun getExecutorRef(): NameRef = this.executorRef

	override fun getInitiatorRef(): NameRef = this.initiatorRef

	override fun getRootRef(): UUID = super.getRootRef() as UUID

	override fun toString(): String = "${this.resultKind.nameRef().get()}" +
			"#${Integer.toHexString(this.id.get().hashCode())}" +
			"[${this.initiatorRef.get()}->${this.executorRef.get()}]" +
			"@${Integer.toHexString(this.getRootRef().hashCode())}"
}
