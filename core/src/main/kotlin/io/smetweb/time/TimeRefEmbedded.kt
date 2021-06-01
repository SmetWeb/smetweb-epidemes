package io.smetweb.time

import io.smetweb.math.parseQuantity
import java.math.BigDecimal
import java.time.Instant
import java.util.Date
import javax.measure.quantity.Time
import javax.persistence.*

/**
 * [TimeRefEmbedded] is a data access object for persisting [TimeRef] objects, with respective
 * JPA attributes specified in the generated [TimeRefEmbedded_] meta-model.
 */
@Embeddable
data class TimeRefEmbedded(

		/** derived numeric relative time, converted to common base time units */
		@Basic
		@Column(name = "TIME", updatable = false)
		var ordinal: BigDecimal? = null,

		/** exact relative time amount description with JSR-310 precision, scale and unit specification */
		@Basic
		@Column(name = "TIME_EXACT", updatable = false)
		var exact: String? = null,

		/** derived absolute POSIX time from UTC epoch,; [Temporal] incompatible with more precise [Instant] */
		@Temporal(TemporalType.TIMESTAMP)
		@Column(name = "TIME_STAMP", updatable = false)
		var posix: Date? = null

		// TODO replace by (or extend with) java.time.Instant
		// for nano-precision (if supported by DB)?
) {

	constructor(
			time: TimeRef = TimeRef.T_ZERO,
			epoch: Instant = Instant.EPOCH
	): this(
			ordinal = time.decimalUnits(),
			exact = time.toString(),
			posix = time.toDate(Date.from(epoch)))

	fun toTimeRef(): TimeRef =
			TimeRef.of(this.exact?.parseQuantity(Time::class.java) ?: error("Attribute not loaded?"))
}