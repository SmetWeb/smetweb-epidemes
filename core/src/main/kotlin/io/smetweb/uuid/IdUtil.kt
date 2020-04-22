package io.smetweb.uuid

import com.fasterxml.uuid.Generators
import com.fasterxml.uuid.NoArgGenerator
import com.fasterxml.uuid.UUIDTimer
import java.math.BigDecimal
import java.time.Instant
import java.util.Random
import java.util.UUID

private val UUID_TIMER = UUIDTimer(Random(System.currentTimeMillis()), null)

private val TIME_BASED_UUID_GENERATOR: NoArgGenerator = Generators.timeBasedGenerator(null, UUID_TIMER)

private const val UUID_EPOCH_TO_UTC_EPOCH_100NS = 0x01b21dd213814000L // see https://stackoverflow.com/a/15179513

@Throws(UnsupportedOperationException::class, ArithmeticException::class)
fun UUID.created(): Instant {
	val secondsDecimal = BigDecimal.valueOf(this.timestamp() - UUID_EPOCH_TO_UTC_EPOCH_100NS)
			.scaleByPowerOfTen(-7)
	val secondsTruncated = secondsDecimal.toLong()
	val nanoAdjustment = secondsDecimal
			.subtract(BigDecimal(secondsTruncated))
			.scaleByPowerOfTen(9)
			.longValueExact()

	return Instant.ofEpochSecond(secondsTruncated, nanoAdjustment)
}

fun generateUUID(): UUID = TIME_BASED_UUID_GENERATOR.generate()
