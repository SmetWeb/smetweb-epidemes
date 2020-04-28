package io.smetweb.uuid

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import io.smetweb.ref.NameRef
import java.util.*

/**
 * [UuidNameRef] is a special [NameRef] structure which ensures
 * that the root or [rootRef] value is of type [UUID]
 */
@Suppress("UNCHECKED_CAST")
data class UuidNameRef(
		override val parentRef: UuidNameRef? = null,
		override val value: Comparable<*>
): NameRef {

	/**
	 * a new unique global context (root) identifier
	 */
	constructor(): this(generateUUID())

	/**
	 * a known global context (root) identifier
	 */
	constructor(rootRef: UUID): this(value = rootRef)

	/**
	 * a contextualized (child/local) identifier
	 */
	constructor(value: Comparable<*>, rootRef: UUID): this(value, UuidNameRef(rootRef))

	/**
	 * a contextualized (child/local) identifier
	 */
	constructor(value: Class<*>, rootRef: UUID): this(value.javaClass.name, UuidNameRef(rootRef))

	/**
	 * a contextualized (child/local) identifier
	 */
	constructor(value: Comparable<*>, parent: UuidNameRef): this(parent, value)

	/**
	 * @param value the [String] representation to parse/deserialize
	 */
	@JsonCreator
	constructor(value: String): this(value.trim().split(NameRef.ROOT_SEPARATOR))

	override fun toString(): String = NameRef.toHashString(this)

	@JsonValue
	fun toJson(): String = NameRef.toString(this)

	// utility constructor for parsing the name space tree structure
	private constructor(pair: List<String>): this(
			context = when(pair.size) {
				2 -> UUID.fromString(pair[1])
				else -> throw IllegalArgumentException("Unable to parse local id from $pair")
			},
			values = pair[0].split(NameRef.VALUE_SEPARATOR_REGEX, 2))

	// utility constructor for parsing the name space tree structure
	private constructor(context: UUID, values: List<String>): this(
			parent = when(values.size) {
				1 -> UuidNameRef(context)
				else -> UuidNameRef(context, values[1].split(NameRef.VALUE_SEPARATOR_REGEX, 2))
			},
			value = values[0])

	override fun rootRef(): UUID = super.rootRef() as UUID

}