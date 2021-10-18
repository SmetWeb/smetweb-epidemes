package io.smetweb.uuid

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import io.smetweb.refer.IdRef
import java.util.UUID

data class UuidRef(
		override val value: UUID
): IdRef<UUID, UUID> {

	constructor(): this(generateUUID())

	@JsonCreator
	constructor(json: String): this(UUID.fromString(json))

	@JsonValue
	override fun toString(): String = get().toString()

}