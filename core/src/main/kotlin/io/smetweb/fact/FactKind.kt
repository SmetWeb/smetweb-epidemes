package io.smetweb.fact

import io.smetweb.fact.ActKind.*
import io.smetweb.fact.CoordinationKind.*
import io.smetweb.fact.RoleKind.*

/**
 * [FactKind] defines the types of coordination facts (steps or stages)
 * that can occur in a transaction or cancellation exchange (state machine)
 */
enum class FactKind
(
		val coordinationKind: CoordinationKind,

		/**
		 * the transaction actor role performing the follow-up act (decision),
		 * or `null` for either, i.e. if this is fact type leads to
		 * a terminal state in the transaction/cancellation process for BOTH roles
		 */
		val responderRole: RoleKind?,

		/** the response act kinds possibly ensuing this fact type, counting:
		 * 0 (terminal), 1 (proceed) or 2 (proceed or regress) */
		vararg responseActs: ActKind

) {

	/** the moment that a transaction's order phase is initiated  */
	INITIATED(TRANSACTION, INITIATOR, REQUESTING),

	/** transaction/exchange terminated */
	QUIT(TRANSACTION, null),

	/**
	 * The (intersubjective) "rq" coordination fact (C-fact) that a request has
	 * been made in some transaction by its initiator to its executor. This fact
	 * is an agendum for the executor to either promise or decline to produce
	 * its P-fact.
	 */
	REQUESTED(TRANSACTION, EXECUTOR, PROMISING, DECLINING),

	/**  */
	DECLINED(TRANSACTION, INITIATOR, REQUESTING, QUITTING),

	/**
	 * The (intersubjective) "pm" coordination fact (C-fact) that a promise was
	 * made in some transaction by its executor to its initiator. This fact is
	 * an agendum for the executor to produce the P-Fact of the transaction and
	 * state the result.
	 */
	PROMISED(TRANSACTION, EXECUTOR, EXECUTING),

	/**
	 * The (subjective) production fact (P-fact) that the executor produced the
	 * P-Fact of some transaction. Production is handled subjectively, i.e. its
	 * processes and result are principally not knowable to the initiator.
	 */
	EXECUTED(TRANSACTION, EXECUTOR, STATING),
	
	/**
	 * The (intersubjective) "st" coordination fact (C-fact) that the P-fact of
	 * some transaction was stated by its executor to its initiator. This fact
	 * is an agendum for the initiator to either accept or refuse the stated
	 * result (P-fact) of this transaction process.
	 */
	STATED(TRANSACTION, INITIATOR, ACCEPTING, REJECTING),

	/**
	 * The (intersubjective) "rj" coordination fact (C-fact) that the P-fact
	 * stated by the executor of some transaction was rejected by its initiator.
	 * This fact is an agendum for the executor to either state a new result
	 * (P-fact) or stop this transaction process.
	 */
	REJECTED(TRANSACTION, EXECUTOR, STATING, STOPPING),

	/** terminal state for P-fact transaction for both initiator and executor  */
	STOPPED(TRANSACTION, null),

	/**
	 * The (intersubjective) "ac" coordination fact (C-fact) that the P-fact of
	 * some transaction as stated by its executor was accepted by its initiator.
	 * This fact is a terminal state of this transaction process.
	 */
	ACCEPTED(TRANSACTION, null),

	/*************************
	 * CANCELLATION patterns *
	 *************************/

	/** the moment that a request cancellation phase is initiated  */
	_INITIATED_REQUEST_CANCELLATION(CANCELLATION, INITIATOR, CANCELLING_REQUEST),

	/**  */
	_CANCELLED_REQUEST(CANCELLATION, EXECUTOR, ALLOWING_REQUEST_CANCELLATION,
			REFUSING_REQUEST_CANCELLATION),

	/** terminal state for request cancellation  */
	_ALLOWED_REQUEST_CANCELLATION(CANCELLATION, INITIATOR, QUITTING),

	/** terminal state for request cancellation  */
	_REFUSED_REQUEST_CANCELLATION(CANCELLATION, null),

	/** the moment that a promise cancellation phase is initiated  */
	_INITIATED_PROMISE_CANCELLATION(CANCELLATION, EXECUTOR, CANCELLING_PROMISE),

	/**  */
	_CANCELLED_PROMISE(CANCELLATION, INITIATOR, ALLOWING_PROMISE_CANCELLATION,
			REFUSING_PROMISE_CANCELLATION),

	/** terminal state for promise cancellation  */
	_ALLOWED_PROMISE_CANCELLATION(CANCELLATION, EXECUTOR, DECLINING),

	/**
	 * terminal state for promise cancellation. promised is still the case,
	 * therefore refusal ranks higher
	 */
	_REFUSED_PROMISE_CANCELLATION(CANCELLATION, null),

	/** the moment that a promise cancellation phase is initiated  */
	_INITIATED_STATE_CANCELLATION(CANCELLATION, EXECUTOR, CANCELLING_STATE),

	/**
	 * The executor cancelled a state (e.g. to avoid rejection), causing the
	 * initiator to stop rejecting/accepting and allow or refuse this
	 * cancellation (Dietz, 2006:97)
	 */
	_CANCELLED_STATE(CANCELLATION, INITIATOR, ALLOWING_STATE_CANCELLATION,
			REFUSING_STATE_CANCELLATION),

	/**
	 * terminal state for state cancellation. The initiator allowed the
	 * executor's state cancellation (e.g. due to execution flaws) causing the
	 * executor to re-execute (Dietz, 2006:97)
	 */
	_ALLOWED_STATE_CANCELLATION(CANCELLATION, null),

	/**
	 * Terminal state for state cancellation. When a state cancellation was
	 * refused by the initiator (e.g. the execution flaws accepted), the
	 * transaction remains 'stated' (Dietz, 2006:97)
	 */
	_REFUSED_STATE_CANCELLATION(CANCELLATION, null),

	/** the moment that an accept cancellation phase is initiated  */
	_INITIATED_ACCEPT_CANCELLATION(CANCELLATION, INITIATOR, CANCELLING_ACCEPT),

	/**
	 * agendum for executor to allow or refuse the accept cancelation by the
	 * initiator
	 */
	_CANCELLED_ACCEPT(CANCELLATION, EXECUTOR, ALLOWING_ACCEPT_CANCELLATION,
			REFUSING_ACCEPT_CANCELLATION),

	/**
	 * terminal state for accept cancellation. The executor allowed the
	 * initiator's accept cancellation (e.g. due to payment problems) causing
	 * the initiator to reject (Dietz, 2006:97)
	 */
	_ALLOWED_ACCEPT_CANCELLATION(CANCELLATION, INITIATOR, REJECTING),

	/** terminal state for accept cancellation, P-fact remains 'accepted'  */
	_REFUSED_ACCEPT_CANCELLATION(CANCELLATION, null),

	;

	val creatorRole: RoleKind? = responderRole?.opposite()

	/**
	 * The generic act types possibly triggered by this fact type decider's
	 * response
	 */
	private val responseActs: Array<out ActKind>

	init {
		this.responseActs = arrayOf(*responseActs)
		if (this.responseActs.size > 2)
			throw IllegalStateException("Proceed act undefined for "
					+ name + ", too many options: $responseActs")
	}

	/**
	 * @return true if no more actions need/can be taken by either actor role
	 */
	fun isTerminal() = responseActs.isEmpty()

	/**
	 * @return the generic actor role type sending the fact type,
	 * or `null` for either
	 */
	fun creatorRoleType() = responderRole?.opposite()

	/**
	 * @param roleType the role type to check whether it should respond
	 * @return true if actions need/can be taken by specified actor role type
	 */
	fun isAgendumFor(roleType: RoleKind): Boolean {
		return defaultResponseKind(roleType, true) != null
	}

	/**
	 * @param response the act type to check as legal follow-up stage
	 * @return `true` if specified act type is a legal follow-up stage
	 */
	fun isValidResponseKind(response: ActKind): Boolean {
		return responderRole != response.performer
			// any response is valid for the party that has to wait
				|| responseActs.any { it == response }
	}

	/**
	 * @param roleType the role type who performs the response act
	 * @param proceed whether to continue or stop the process
	 * @return the agenda (default response act type) for specified role
	 * and actor type given specified permission to continue or not,
	 * or `null` if only options are to await reply or to terminate the exchange
	 */
	fun defaultResponseKind(
            roleType: RoleKind,
            proceed: Boolean
	): ActKind? {
		if (isTerminal())
			return null
		if (responderRole != null && responderRole != roleType) {
			// cancellation not allowed at this coordination stage
			if (proceed || coordinationKind == CANCELLATION)
				return null

			// possibly cancel (roll-back) a transaction's last coordination fact
			return when (this) {
				REQUESTED -> CANCELLING_REQUEST
				PROMISED -> CANCELLING_REQUEST
				EXECUTED -> CANCELLING_REQUEST
				STATED -> CANCELLING_STATE
				REJECTED -> CANCELLING_REQUEST
				else -> null
			}
		}
		return when (proceed) {
			true -> responseActs[0]
			else -> if (responseActs.size == 1)
				// can't regress from current stage
				null
			else
				responseActs[1]
		}
	}

	fun isValidDefaultResponse(factType: FactKind, roleKind: RoleKind, proceed: Boolean) =
			isValidResponseKind(factType.defaultResponseKind(roleKind, proceed)!!)

}