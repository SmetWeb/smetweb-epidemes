package io.smetweb.reflect

import java.lang.reflect.GenericArrayType
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.TypeVariable

/**
 * Get the underlying (raw) class for a type, or null if the type is a variable
 * type. See [description](http://www.artima.com/weblogs/viewpost.jsp?thread=208860)
 *
 * @return the underlying class, or `null` if the type is a variable
 */
fun Type.toClass(): Class<*> =
    when(this) {
        is Class<*> -> this
        is ParameterizedType -> this.rawType.toClass()
        is GenericArrayType -> this.genericComponentType.toClass().let {
            java.lang.reflect.Array.newInstance(it, 0)::class.java
        }
        else -> error("Unknown type: ${this::class.java} ($this)")
    }

fun Class<*>.typeArgumentsFor(genericTarget: Class<*>): List<Pair<TypeVariable<*>, Class<*>>> =
    resolveParameterizedType(genericTarget).resolveTypeArguments()

inline fun <reified T: Any, S: T> T.typeArgumentsFor(genericTarget: Class<S>): List<Pair<TypeVariable<*>, Class<*>>> =
    javaClass.resolveParameterizedType(genericTarget).resolveTypeArguments()

fun ParameterizedType.resolveTypeArguments(): List<Pair<TypeVariable<*>, Class<*>>> =
    actualTypeArguments.mapIndexed { i, typeArgument ->
        Pair((rawType as Class<*>).typeParameters[i], typeArgument.toClass())
    }

@Suppress("UNCHECKED_CAST")
fun Type.resolveParameterizedType(genericTarget: Class<*>, originType: Class<*> = toClass()): ParameterizedType {
    if (genericTarget.typeParameters.isEmpty())
        error("Target type '$genericTarget' is not generic (parameterizable)")

    return when(val rawType = toClass()) {
        genericTarget -> this as ParameterizedType
        else -> {
            for(genericInterface in rawType.genericInterfaces)
                if (genericInterface.toClass() == genericTarget)
                    return genericInterface as ParameterizedType

            // not this, not any interface, recurse to array-component-type, and superclass
            rawType.componentType?.resolveParameterizedType(genericTarget, originType)
                ?: rawType.genericSuperclass?.resolveParameterizedType(genericTarget, originType)
                ?: error("Super-type '${genericTarget.name.substringAfter('$')}' " +
                        "not found for '${originType.name.substringAfter('$')}'")
        }
    }
}
