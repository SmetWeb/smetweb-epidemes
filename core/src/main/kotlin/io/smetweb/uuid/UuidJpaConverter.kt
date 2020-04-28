package io.smetweb.uuid

import java.nio.ByteBuffer
import java.nio.LongBuffer
import java.util.UUID
import javax.persistence.AttributeConverter
import javax.persistence.Convert
import javax.persistence.Converter
import javax.persistence.Entity

/**
 * A JPA [AttributeConverter] that converts [UUID] to JSON text.
 * Apply to your [@Entity][Entity] attribute (field of type [UUID]).
 * Usage: [@Convert][Convert]`(converter=`[UuidJpaConverter]`.class)`
 */
@Converter(autoApply = true)
class UuidJpaConverter : AttributeConverter<UUID, ByteArray> {

	override fun convertToDatabaseColumn(attribute: UUID?): ByteArray? = attribute
			?.let { uuid -> toBytes(uuid.mostSignificantBits, uuid.leastSignificantBits) }

	override fun convertToEntityAttribute(dbData: ByteArray?): UUID? = dbData
			?.let(Companion::toLongs)
			?.let { longs -> UUID(longs[0], longs[1]) }

	companion object {

		const val SQL_DEFINITION = "BINARY(16)"

		const val SQL_LENGTH = 16

		fun toBytes(vararg elements: Long) = ByteArray(java.lang.Long.BYTES * 2)
				.apply {
					ByteBuffer.wrap(this)
							.asLongBuffer()
							.put(longArrayOf(*elements)) }

		fun toLongs(bytes: ByteArray): LongBuffer = ByteBuffer
				.wrap(bytes)
				.asLongBuffer()
	}
}