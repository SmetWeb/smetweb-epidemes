package io.smetweb.fact

/**
 * [CoordinationKind] defines the two types of coordination patterns
 * occurring in [UuidResultExchange]s of coordination [UuidFact]s
 */
enum class CoordinationKind {
	TRANSACTION,
	CANCELLATION;
}
