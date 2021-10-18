package io.smetweb.json

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.TreeNode
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import io.smetweb.math.parseQuantity
import io.smetweb.math.toPlainString
import io.smetweb.math.toQuantity
import tech.units.indriya.ComparableQuantity
import java.io.IOException
import javax.measure.Quantity

class QuantityJsonModule : SimpleModule() {

    init {
        addSerializer(JSON_SERIALIZER)
        addKeyDeserializer(Quantity::class.java, KEY_DESERIALIZER)
        addDeserializer(Quantity::class.java, JSON_DESERIALIZER)
        addKeyDeserializer(ComparableQuantity::class.java, KEY_DESERIALIZER)
        addDeserializer(ComparableQuantity::class.java, JSON_DESERIALIZER)
    }

    companion object {

        private val JSON_REGISTERED: MutableSet<ObjectMapper> = HashSet()

        @Synchronized
        fun checkRegistered(om: ObjectMapper) {
            if (!JSON_REGISTERED.contains(om))
                JSON_REGISTERED.add(om.registerModule(QuantityJsonModule()))
        }

        private val KEY_DESERIALIZER: KeyDeserializer =
            object : KeyDeserializer() {
                override fun deserializeKey(key: String, ctxt: DeserializationContext): ComparableQuantity<*> =
                    key.parseQuantity()
            }

        private val JSON_DESERIALIZER: JsonDeserializer<ComparableQuantity<*>> =
            object : JsonDeserializer<ComparableQuantity<*>>() {
                @Throws(IOException::class)
                override fun deserialize(p: JsonParser, ctxt: DeserializationContext): ComparableQuantity<*>? {
                    return if (p.currentToken.isNumeric)
                        p.numberValue.toQuantity()
                    else if (p.currentToken.isScalarValue)
                        p.valueAsString.parseQuantity()
                    else {
                        val tree: TreeNode = p.readValueAsTree()
                        if (tree.size() == 0)
                            null
                        else
                            throw IOException("Problem parsing " + Quantity::class.java.simpleName + " from " + tree)
                    }
                }
            }

        private val JSON_SERIALIZER: JsonSerializer<Quantity<*>> =
            object : StdSerializer<Quantity<*>>(Quantity::class.java) {
                @Throws(IOException::class)
                override fun serialize(value: Quantity<*>, gen: JsonGenerator, serializers: SerializerProvider) {
                    gen.writeString(value.toPlainString())
                }
            }
    }
}