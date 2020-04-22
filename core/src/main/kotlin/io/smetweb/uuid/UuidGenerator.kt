package io.smetweb.uuid

import org.springframework.stereotype.Component
import org.springframework.util.IdGenerator

@Component
class UuidGenerator : IdGenerator {

	override fun generateId() = generateUUID()

}