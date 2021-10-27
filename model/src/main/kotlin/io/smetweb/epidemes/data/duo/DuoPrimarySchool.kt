package io.smetweb.epidemes.data.duo

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import io.smetweb.epidemes.data.cbs.CBSRegionType
import io.smetweb.epidemes.data.cbs.CbsRegionHierarchy
import io.smetweb.json.OBJECT_MAPPER
import io.smetweb.json.asBigDecimal
import io.smetweb.json.stream
import io.smetweb.random.*
import java.io.IOException
import java.io.InputStream
import java.util.EnumMap
import java.util.TreeMap
import java.util.stream.Collectors
import java.util.stream.Stream
import kotlin.streams.toList

@Suppress("UNUSED")
object DuoPrimarySchool {

    const val ZIPDIST_KEY = "zip_dist"
    const val SCHOOLS_KEY = "schools"

    private fun toEnumMap(node: ArrayNode): EnumMap<EduCol, JsonNode> =
        node.stream().collect(Collectors.toMap(
            { EduCol.values()[it.first] },
            { it.second },
            { _, v -> v },
            { EnumMap<EduCol, JsonNode>(EduCol::class.java) }))

    @Throws(IOException::class)
    fun <T> parse(
        inputStream: InputStream,
        rng: PseudoRandom = PseudoRandomEcj(),
        classifier: (String, Map<EduCol, JsonNode>) -> Stream<T>
    ): TreeMap<String, MutableMap<T, ProbabilityDistribution<String>>> {
        val root: JsonNode = OBJECT_MAPPER.readTree(inputStream)
        val schools: ObjectNode = root.with(SCHOOLS_KEY) as ObjectNode
        return root.stream(ZIPDIST_KEY)
            .flatMap { it.value.stream(CBSRegionType.PROVINCE.prefix) }
            .flatMap { it.value.stream(CBSRegionType.COROP.prefix) }
            .flatMap { it.value.stream(CBSRegionType.MUNICIPAL.prefix) }
            .flatMap { it.value.stream() }
            .filter { it.key != CbsRegionHierarchy.REG_TAGS_KEY }
            .collect(Collectors.toMap(
                { it.key },
                { zipWvs ->
                    zipWvs.value.stream()
                        .flatMap { wv ->
                            val zipSchools = toEnumMap(schools[wv.key] as ArrayNode)
                            classifier(wv.key, zipSchools).map { cat -> Pair(cat, wv) }
                        }
                        // reclassify
                        .collect(Collectors.groupingBy { catSchool -> catSchool.first })
                        .entries
                        .stream()
                        .collect(Collectors.toMap( // key2: cat
                            { it.key },
                            { rng.categorical(
                                it.value.stream().map { wv -> Pair(wv.second.key, wv.second.value.asBigDecimal()) }.toList())
                            },
                            { _, v2 -> v2 },
                            { HashMap() }))
                },
                { _, v2 -> v2 },
                { TreeMap() }))
    }

    enum class Col {
        PEILDATUM,
        GEMEENTENUMMER,
        GEMEENTENAAM,
        POSTCODE_LEERLING,
        GEMEENTENAAM_LEERLING,
        BRIN_NUMMER,
        VESTIGINGSNUMMER,
        NSTELLINGSNAAM_VESTIGING,
        POSTCODE_VESTIGING,
        PLAATSNAAM,
        PROVINCIE,
        SOORT_PO,
        DENOMINATIE_VESTIGING,
        BEVOEGD_GEZAG_NUMMER,
        L3_MIN, L4, L5, L6, L7, L8, L9, L10, L11, L12, L13, L14, L15, L16, L17, L18, L19, L20, L21, L22, L23, L24, L25_PLUS,
        TOTAAL
    }

    enum class EduCol {
        GEMEENTE,
        PO_SOORT,
        DENOMINATIE,
        BRIN,
        VESTIGING,
        POSTCODE,
        LATITUDE,
        LONGITUDE
    }
}