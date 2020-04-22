package io.smetweb.fact

import io.smetweb.ref.NameRef

/**
 * [ResultKind] identifies the type of production fact is brought about
 * by the [CoordinationFact]s in the [ResultExchange]
 */
interface ResultKind {

	fun nameRef(): NameRef

	fun getExecutorKind(): Class<out ActorKind>

	fun getInitiatorKinds(): Collection<Class<out ActorKind>> = listOf(ActorKind::class.java)

	// helper functions

	fun getRootRef(): Comparable<*> = nameRef().getRootRef()

	companion object {

		@JvmStatic
		fun of(name: NameRef,
               executorKind: Class<out ActorKind> = ActorKind::class.java,
               initiatorKinds: Collection<Class<out ActorKind>> = listOf(executorKind)
		) = object: ResultKind {
			override fun nameRef() = name
			override fun getExecutorKind() = executorKind
			override fun getInitiatorKinds() = initiatorKinds
			override fun toString(): String = name.toString()
		}

		@JvmStatic
		fun of(name: NameRef,
               executorKind: Class<out ActorKind> = ActorKind::class.java,
               vararg initiatorKinds: Class<out ActorKind> = arrayOf(executorKind)
		) = of(name, executorKind, listOf(*initiatorKinds))
	}
}