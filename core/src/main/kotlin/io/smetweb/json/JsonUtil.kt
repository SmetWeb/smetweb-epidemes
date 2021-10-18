package io.smetweb.json

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.core.TreeNode
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectReader
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.treeToValue
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.schedulers.Schedulers
import io.smetweb.log.getLogger
import java.io.IOException
import java.io.InputStream
import java.math.BigDecimal
import java.util.stream.IntStream
import java.util.stream.Stream
import java.util.stream.StreamSupport


val OBJECT_MAPPER: ObjectMapper by lazy {
    ObjectMapper().apply {
        disable( SerializationFeature.FAIL_ON_EMPTY_BEANS );

        // setSerializationInclusion(JsonInclude.Include.NON_NULL)

        QuantityJsonModule.checkRegistered(this)

//        val modules = arrayOf( JodaModule(), UUIDModule(), JavaTimeModule() )
//        registerModules( *modules )

        // Log4j2 may cause recursive call, when initialization is during logging event
//		println( "Using jackson v: " + version() + " with: " + modules.map { it.moduleName } )
    }
}

fun JsonNode.asBigDecimal(): BigDecimal =
    asText().toBigDecimal()

fun <T: Any> TreeNode.readValue(
    type: Class<T>,
    om: ObjectMapper = OBJECT_MAPPER
): T =
    om.treeToValue(this, type)

inline fun <reified T: Any> TreeNode.readValue(
    om: ObjectMapper = OBJECT_MAPPER
): T? =
    om.treeToValue(this)

fun JsonNode.stream(
    property: String? = null,
    parallel: Boolean = false,
    node: JsonNode = property?.let { with(it) } ?: this
): Stream<Map.Entry<String, JsonNode>> =
    Iterable { node.fields() }
        .spliterator()
        .let { StreamSupport.stream(it, parallel) }

fun ObjectNode.forEach(
    property: String? = null,
    parallel: Boolean = false,
    visitor: (String, JsonNode) -> Unit
) =
    stream(property, parallel)
        .forEach { visitor(it.key, it.value) }

fun ArrayNode.stream(
    parallel: Boolean = false
): Stream<Pair<Int, JsonNode>> =
    IntStream.range(0, size())
        .apply {  if (parallel) parallel() }
        .mapToObj { Pair(it, this[it]) }

fun ArrayNode.forEachIndexed(
    parallel: Boolean = false,
    visitor: (Int, JsonNode?) -> Unit,
) =
    stream(parallel)
        .forEach { visitor(it.first, it.second) }


/**
 * parse a (JSON formatted) [String] to a value of specified type
 * @param resultType the type of result [Object]
 * @param om the [ObjectMapper] used to parse/deserialize/unmarshal
 * @return the parsed/deserialized/unmarshalled [Object]
 */
@Throws(JsonProcessingException::class)
fun <T> String?.parseJSON(resultType: Class<T>, om: ObjectMapper = OBJECT_MAPPER): T? =
    this?.let {
        if(it.equals("null", ignoreCase = true))
            null
        else {
            val raw = if (!startsWith("\"") && resultType == String::class.java) "\"$this\"" else this
            om.readValue(raw, resultType)
        }
    }

/**
 * TODO split into common parsing Observable with JsonParser provider
 *
 * @param json the JSON array [InputStream] supplier
 * @param elementType the type of elements to parse
 * @return the parsed/deserialized/unmarshalled [Object]s from first array encountered in the supplied [InputStream]s
 */
@SafeVarargs
fun <T> readArrayAsync(
    elementType: Class<T>,
    om: ObjectMapper = OBJECT_MAPPER,
    scheduler: Scheduler = Schedulers.io(),
    json: () -> InputStream,
): Observable<T> =
    readArrayAsync<T>({ om.factory.createParser(json()) })
        { om.readerFor(om.typeFactory.constructType(elementType)) }
        .observeOn(scheduler)

/**
 * @return the deserialized [Object]s from first array encountered
 */
private fun <T> readArrayAsync(
    jpFactory: () -> JsonParser,
    orFactory: () -> ObjectReader
): Observable<T> {
    val log = getLogger()
    // see http://www.cowtowncoder.com/blog/archives/2009/01/entry_132.html
    return Observable.using(jpFactory, { jsonParser ->
        try {
            jsonParser.readUntil().apply { if (isNotEmpty()) log.warn("Ignoring unexpected preamble: {}", this) }
        } catch (e: Exception) {
            return@using Observable.error(e)
        }
        Observable.create { emitter ->
            val objectReader = orFactory()
            var i = 0
            var last: T? = null
            while (!emitter.isDisposed && jsonParser.nextToken() !== JsonToken.END_ARRAY) {
                objectReader.readValue<T?>(jsonParser)?.also { emitter.onNext(it); last = it }
                i++
            }
            emitter.onComplete()
            log.trace("Parsed {} x {}", i, if (last == null) "?" else last!!::class.java.simpleName)
        }
    }, JsonParser::close)
}

private fun JsonParser.readUntil(token: JsonToken = JsonToken.START_ARRAY): StringBuffer {
    val preamble = StringBuffer()
    // parse whichever array comes first, skip until start-array '['
    while (nextToken() !== token) {
        if (currentToken() == null)
            throw IOException("Missing input")
        preamble.append(text)
    }
    return preamble
}

/**
 * @return the serialized (minimal) JSON-[String] representation of this value of [Any] type
 */
@Throws(JsonProcessingException::class)
fun Any?.stringify(om: ObjectMapper = OBJECT_MAPPER): String =
    om.writer().writeValueAsString(this)

/**
 * @return the serialized (pretty) JSON-[String] representation of this value of [Any] type
 */
@Throws(JsonProcessingException::class)
fun Any?.toJSON(om: ObjectMapper = OBJECT_MAPPER): String =
    om.writer().withDefaultPrettyPrinter().writeValueAsString(this)

/**
 * @return the serialized [JsonNode] tree from a value of [Any] type
 * @see ObjectMapper.valueToTree
 */
fun Any.toTree(om: ObjectMapper = OBJECT_MAPPER): JsonNode  =
    om.valueToTree(this)

/**
 * @return the parsed/deserialized/unmarshalled [JsonNode] tree from an [InputStream] feeding JSON formatted value
 * @see ObjectMapper.readTree
 */
fun InputStream.toTree(om: ObjectMapper = OBJECT_MAPPER): JsonNode =
    om.readTree(this)

/**
 * @return the parsed/deserialized/unmarshalled [JsonNode] tree of a JSON formatted [String]
 * @see ObjectMapper.readTree
 */
@Throws(JsonProcessingException::class)
fun String?.toTree(om: ObjectMapper = OBJECT_MAPPER): JsonNode? =
    if (this == null || isEmpty())
        null
    else
        om.readTree(this)
