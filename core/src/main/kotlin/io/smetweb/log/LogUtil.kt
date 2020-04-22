package io.smetweb.log

import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun lazyString(provider: () -> Any?) = object: Any() {
	override fun toString(): String = provider()?.toString() ?: "null"
}

inline fun <reified T: Any> T.getLogger(): Logger =
		LoggerFactory.getLogger(this::class.java.name.substringBefore("\$Companion"))
