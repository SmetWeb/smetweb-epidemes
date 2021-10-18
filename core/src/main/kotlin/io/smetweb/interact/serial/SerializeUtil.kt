package io.smetweb.interact.serial

import java.io.*
import java.util.*

/**
 * @param toType the type of value to deserialize
 * @return the deserialized [Object]
 * @throws IOException
 * @throws ClassNotFoundException
 */
@Throws(IOException::class, ClassNotFoundException::class)
fun <T: Serializable> String.deserialize(toType: Class<T>): T =
    ObjectInputStream(
        ByteArrayInputStream(
            Base64.getDecoder().decode(this) as ByteArray
        )
    ).use { `in` -> toType.cast(`in`.readObject()) }

/**
 * Write the object to a Base64 string
 *
 * @return the [String] representation of the [Serializable] object
 * @throws IOException
 */
@Throws(IOException::class)
fun Serializable.serialize(): String =
    ByteArrayOutputStream().use { baos ->
        ObjectOutputStream(baos).use { oos ->
            oos.writeObject(this)
            String(Base64.getEncoder().encode(baos.toByteArray()))
        }
    }
