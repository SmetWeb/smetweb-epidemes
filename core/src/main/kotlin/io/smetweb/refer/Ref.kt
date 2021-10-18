package io.smetweb.refer

import java.util.function.Supplier

/**
 * [Ref] is a [Supplier] wrapping some reference value of type [T], which as supplied by (or unwrapped with) [get]
 */
interface Ref<T>: Supplier<T> {

	val value: T

	override fun get(): T = value

	/**
	 * [Ordinal] is a [Ref] wrapping some ordinal reference value of type [T] and is also
	 * [Comparable] with objects of type [C] (typically the same as [T], but not necessarily)
	 */
	interface Ordinal<C, T: Comparable<C>>: Ref<T>, Comparable<Comparable<C>> {

		@Suppress("UNCHECKED_CAST")
		override fun compareTo(other: Comparable<C>): Int =
				get().compareTo((other as Supplier<*>).get() as C)

	}

}