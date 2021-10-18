package io.smetweb.log

import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun lazyString(provider: () -> Any?) = object: Any() {
	override fun toString(): String = provider()?.toString() ?: "null"
}

fun getLogger(name: String = StackWalker.getInstance().walk { frames -> frames.skip(1).findFirst().map { it.className } }.get()): Logger =
	LoggerFactory.getLogger(name)

inline fun <reified T: Any> T.getLogger(): Logger =
	getLogger(this::class.java.name.substringBefore("\$Companion"))
