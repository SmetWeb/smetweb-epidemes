package io.smetweb.refer

/**
 * A [NameRef] is a [Ref] wrapping any ([Comparable]) value
 * and a ([Comparable]) [parentRef] (or `null`) in some ordinal or sorted
 * name space tree, such that [rootRef] recursively resolves the tree's root name value
 */
@FunctionalInterface
interface NameRef : Ref<Comparable<*>>, Comparable<Comparable<*>> {

	val parentRef: NameRef?
		get() = null

	fun rootRef(): Comparable<*> {
		var i: NameRef = this
		while (i.parentRef != null) {
			i = i.parentRef as NameRef
		}
		return i.get()
	}

	override fun compareTo(other: Comparable<*>): Int {
		if (other !is NameRef)
			return if (this.parentRef == null)
			// context/root value (no parent), i.e. T: Comparable<T>
				uncheckedCompare(this, other)
			else
				throw IllegalArgumentException(
						"Undefined comparison: '$this' <> '$other'")
		val parentCompare = parentRef?.let {
			uncheckedCompare(it as Comparable<*>, other.parentRef as Comparable<*>)
		} ?: 0
		return if (parentCompare != 0)
			parentCompare
		else
			uncheckedCompare(this, other)
	}

	companion object {

		@JvmStatic
		fun of(value: Comparable<*>, parentRef: NameRef? = null) = object: NameRef {
			override val value = value
			override val parentRef = parentRef
			override fun toString(): String = toHashString(this)
		}

		@JvmStatic
		@Suppress("UNCHECKED_CAST")
		fun <S> uncheckedCompare(self: Comparable<S>, other: Comparable<*>): Int {
			if (self !is NameRef)
				return if (other is NameRef)
					uncheckedCompare(self, other.get())
				else
					self.compareTo(other as S)
			return if (other !is NameRef)
				uncheckedCompare(self.get(), other)
			else
				uncheckedCompare(self.get(), other.get())
		}

		/** the (configured?) id context separator constant */
		@JvmStatic
		val ROOT_SEPARATOR = '@'

		/** the (configured?) id separator constant */
		@JvmStatic
		val VALUE_SEPARATOR = "-"

		/** the (configured?) id separator constant */
		@JvmStatic
		val VALUE_SEPARATOR_REGEX = VALUE_SEPARATOR.toRegex()

		/** utility method for generating an easily parsable @JsonValue of some [nameRef] */
		@JvmStatic
		fun toHashString(nameRef: NameRef): String =
				toString(nameRef) { Integer.toHexString(it.hashCode()) }

		/** utility method for generating an easily parsable @JsonValue of some [nameRef] */
		@JvmStatic
		fun toString(nameRef: NameRef, rootWriter: (Any) -> String = Any::toString): String {
			var result = nameRef.get().toString()
			var id = nameRef.parentRef
			while (id != null) {
				val value = id.get()
				if (id.parentRef == null)
					result += ROOT_SEPARATOR + rootWriter(value)
				else
					result = value.toString() + VALUE_SEPARATOR + result
				id = id.parentRef
			}
			return result
		}
	}
}