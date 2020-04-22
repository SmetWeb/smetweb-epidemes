package io.smetweb.fact

import io.smetweb.fact.FactKind.*
import io.smetweb.fact.CoordinationKind.*
import io.smetweb.fact.RoleKind.*

/**
 * [ActKind] represents the coordination act types that may
 * occur in some transaction
 */
enum class ActKind (
		val coordinationKind: CoordinationKind,

		/** the transaction actor role type performing this type of act  */
		val performer: RoleKind,

		/**
		 * the transaction fact types that may trigger this type of act,
		 * or `null` if undefined (e.g. initiation/termination)
		 */
		val condition: FactKind?,

		/** the coordination fact type emerging from this type of act */
		val outcome: FactKind
) {
	/**   */
	INITIATING(TRANSACTION, INITIATOR, null, INITIATED),

	/**   */
	INITIATING_REQUEST_CANCELLATION(CANCELLATION, INITIATOR, REQUESTED, _INITIATED_REQUEST_CANCELLATION),

	/**   */
	INITIATING_PROMISE_CANCELLATION(CANCELLATION, EXECUTOR, PROMISED, _INITIATED_PROMISE_CANCELLATION),

	/**   */
	INITIATING_STATE_CANCELLATION(CANCELLATION, EXECUTOR, STATED, _INITIATED_STATE_CANCELLATION),

	/**   */
	INITIATING_ACCEPT_CANCELLATION(CANCELLATION, INITIATOR, ACCEPTED, _INITIATED_ACCEPT_CANCELLATION),

	/**
	 * The "rq" coordination act (C-Act) that starts the order-phase (O-phase)
	 * of some transaction and may lead to a "rq" coordination fact.
	 */
	REQUESTING(TRANSACTION, INITIATOR, INITIATED, REQUESTED),

	/**   */
	CANCELLING_REQUEST(CANCELLATION, INITIATOR, REQUESTED, _CANCELLED_REQUEST),

	/**   */
	REFUSING_REQUEST_CANCELLATION(CANCELLATION, EXECUTOR, _CANCELLED_REQUEST, _REFUSED_REQUEST_CANCELLATION),

	/**   */
	ALLOWING_REQUEST_CANCELLATION(CANCELLATION, EXECUTOR, _CANCELLED_REQUEST, _ALLOWED_REQUEST_CANCELLATION),

	/**   */
	DECLINING(TRANSACTION, EXECUTOR, REQUESTED, DECLINED),

	/**   */
	QUITTING(TRANSACTION, INITIATOR, DECLINED, QUIT),

	/** The "pm" coordination act that may lead to a "pm" coordination fact.  */
	PROMISING(TRANSACTION, EXECUTOR, REQUESTED, PROMISED),

	/**   */
	CANCELLING_PROMISE(CANCELLATION, EXECUTOR, PROMISED, _CANCELLED_PROMISE),

	/**   */
	REFUSING_PROMISE_CANCELLATION(CANCELLATION, INITIATOR, _CANCELLED_PROMISE, _REFUSED_PROMISE_CANCELLATION),

	/**   */
	ALLOWING_PROMISE_CANCELLATION(CANCELLATION, INITIATOR, _CANCELLED_PROMISE, _ALLOWED_PROMISE_CANCELLATION),

	/**
	 * The "ex" production act that may lead to the production fact of this
	 * transaction, completing the execution phase.
	 */
	EXECUTING(TRANSACTION, EXECUTOR, PROMISED, EXECUTED),

	/** The "st" coordination act that may lead to a "st" coordination fact.  */
	STATING(TRANSACTION, EXECUTOR, EXECUTED, STATED),

	/**   */
	CANCELLING_STATE(CANCELLATION, EXECUTOR, STATED, _CANCELLED_STATE),

	/**   */
	REFUSING_STATE_CANCELLATION(CANCELLATION, INITIATOR, _CANCELLED_STATE, _REFUSED_STATE_CANCELLATION),

	/**   */
	ALLOWING_STATE_CANCELLATION(CANCELLATION, INITIATOR, _CANCELLED_STATE, _ALLOWED_STATE_CANCELLATION),

	/**   */
	REJECTING(TRANSACTION, INITIATOR, STATED, REJECTED),

	/**   */
	STOPPING(TRANSACTION, EXECUTOR, REJECTED, STOPPED),

	/** The "ac" coordination act that may lead to an "ac" coordination fact.  */
	ACCEPTING(TRANSACTION, INITIATOR, STATED, ACCEPTED),

	/**   */
	CANCELLING_ACCEPT(CANCELLATION, INITIATOR, ACCEPTED, _CANCELLED_ACCEPT),

	/**   */
	REFUSING_ACCEPT_CANCELLATION(CANCELLATION, EXECUTOR, _CANCELLED_ACCEPT, _REFUSED_ACCEPT_CANCELLATION),

	/**   */
	ALLOWING_ACCEPT_CANCELLATION(CANCELLATION, EXECUTOR, _CANCELLED_ACCEPT, _ALLOWED_ACCEPT_CANCELLATION);

	/** the transaction actor role type observing this type of act */
	val listener: RoleKind = performer.opposite()

	/**
	 * @param factType the required fact type
	 * @return `true` if factType is the requirement of this type of act
	 */
	fun hasRequirement(factType: FactKind) = condition == factType

	/**
	 * @param factType the resulting fact type to check
	 * @return `true` if factType is the outcome/result of this type of act
	 */
	fun isOutcome(factType: FactKind) = outcome == factType

}
