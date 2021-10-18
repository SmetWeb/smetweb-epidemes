package io.smetweb.refer

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import io.smetweb.uuid.UuidRef
import java.util.UUID

/**
 * [IdRef] is a [Ref.Ordinal] that wraps some ordinal or [Comparable] identifier
 */
interface IdRef<C, T: Comparable<C>>: Ref.Ordinal<C, T> {

	/**
	 * [IntRef] wraps an [Int] and as data class automatically
	 * integrates the wrapped value's [hashCode], [equals]
	 */
	data class IntRef(
			@JsonValue
			override val value: Int
	): IdRef<Int, Int> {

		constructor(): this(0)

		@JsonCreator
		constructor(json: Number): this(json.toInt())

		fun next() = IntRef(get() + 1)

		override fun toString() = get().toString()
	}

	companion object {

		@JvmStatic
		fun of(value: Int) = IntRef(value)

		@JvmStatic
		fun of(value: UUID) = UuidRef(value)

	}
}