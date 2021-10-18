package io.smetweb.epidemes.data.cbs

import com.fasterxml.jackson.annotation.JsonAnyGetter
import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import io.reactivex.rxjava3.core.Observable
import io.smetweb.epidemes.data.cbs.CBSRegionType.Companion.parse
import io.smetweb.file.toInputStream
import io.smetweb.json.OBJECT_MAPPER
import io.smetweb.json.forEach
import io.smetweb.json.stringify
import io.smetweb.json.toJSON
import io.smetweb.log.getLogger
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.core.config.ConfigurationFactory
import org.apache.logging.log4j.core.config.ConfigurationSource
import org.apache.logging.log4j.core.config.yaml.YamlConfiguration
import org.slf4j.Logger
import java.io.IOException
import java.util.*
import java.util.stream.Collectors

/**
 * [CbsRegionHierarchy] basic address data:
 *
 * ```
 * {
 * "reg":["GM...", ...],
 * "Arbeidsmarktregio's":["AM..", ...],
 * "Arrondissementen (rechtsgebieden)":["AR..", ...],
 * "COROP-gebieden":["CR..", ...],
 * "COROP-subgebieden":["CS..", ...],
 * "COROP-plusgebieden":["CP..", ...],
 * "GGD-regio's":["GG..", ...],
 * "Jeugdzorgregios":["JZ..", ...],
 * "Kamer van Koophandel":["KK..", ...],
 * "Landbouwgebieden":["LB..", ...],
 * "Landbouwgebieden (groepen)":["LG..", ...],
 * "Landsdelen":["LD", ...],
 * "NUTS1-gebieden":["NL.", ...],
 * "NUTS2-gebieden":["NL..", ...],
 * "NUTS3-gebieden":["NL...", ...],
 * "Politie Regionale eenheden":["RE..", ...],
 * "Provincies":["PV..", ...],
 * "Ressorten (rechtsgebieden)":["RT", ...],
 * "RPA-gebieden":["RP..", ...],
 * "Toeristengebieden":["TR", ...],
 * "Veiligheidsregio's":["VR..", ...],
 * "Wgr-samenwerkingsgebieden":["WG..", ...],
 * "Zorgkantoorregio's":["ZK..", ...]
 * }
 * ```
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class CbsRegionHierarchy {

    @JsonProperty("reg")
    lateinit var gm: Array<String>

    private val values: MutableMap<String, Any> = TreeMap()

    @JsonAnySetter
    operator fun set(key: String, value: Any) {
        values[key] = value
    }

    @JsonAnyGetter
    operator fun get(key: String): Any? =
        values[key]

    override fun toString(): String {
        return stringify()
    }

    @Suppress("UNCHECKED_CAST")
    fun cityRegionsByType(): TreeMap<String, EnumMap<CBSRegionType, String>> =
        Observable.range(0, gm.size)
            .collectInto(TreeMap<String, EnumMap<CBSRegionType, String>>()) { m, i ->
                m[gm[i]] = values.values.stream()
                    .map { v: Any -> (v as List<String?>)[i] }
                    .filter { v: String? -> v != null && v.isNotEmpty() }
                    .collect(
                        Collectors.toMap(
                            { v -> parse(v!!) },
                            { v -> v },
                            { v1, _ -> v1 },
                            { EnumMap(CBSRegionType::class.java) }))
            }
            .blockingGet()

    // landsdeel/nuts1 -> prov/nuts2 (12) -> corop/nuts3 (25) -> corop_sub -> corop_plus -> gm (400)
    // ggd (25x)
    // jeugdzorg (42x)
    // ressort (4) -> district (11) -> safety (25) (!= police reg eenh)
    // politie reg eenh (10x) -> districten (43x)
    // agri_group -> agr
    fun addAdminHierarchy(gmMap: JsonNode?): ObjectNode {
        val gmRegs = cityRegionsByType()
        val result: ObjectNode = OBJECT_MAPPER.createObjectNode()
        if (gmMap != null && gmMap.isObject && gmMap.size() != 0) // only place given keys in a hierarchy
            (gmMap as ObjectNode).forEach { k, v ->
                insertHierarchy(result, gmRegs[k.uppercase(Locale.getDefault())]!!, k, v)
            }
        else if (gmMap == null || gmMap.isNull || gmMap.isContainerNode && gmMap.size() == 0) // create empty nodes for all known keys
            gmRegs.forEach { (k: String, v: EnumMap<CBSRegionType, String>) ->
                insertHierarchy(result, v, k, OBJECT_MAPPER.createObjectNode())
            }
        else
            throw IllegalArgumentException("Illegal filter: $gmMap")
        return result
    }

    private fun insertHierarchy(
        container: ObjectNode,
        v: EnumMap<CBSRegionType, String>,
        gm: String,
        gmNode: JsonNode,
        om: ObjectMapper = OBJECT_MAPPER
    ) {
        val ld = container.with(v[CBSRegionType.TERRITORY])
        ld.replace(REG_TAGS_KEY, om.createArrayNode()
            .add(v[CBSRegionType.TERRITORY])
            .add(v[CBSRegionType.NUTS1]))
        val pv = ld.with(CBSRegionType.PROVINCE.prefix).with(v[CBSRegionType.PROVINCE])
        pv.replace(REG_TAGS_KEY, om.createArrayNode()
                .add(v[CBSRegionType.TERRITORY])
                .add(v[CBSRegionType.NUTS1])
                .add(v[CBSRegionType.PROVINCE])
                .add(v[CBSRegionType.NUTS2]))
        val cr = pv.with(CBSRegionType.COROP.prefix).with(v[CBSRegionType.COROP])
        cr.replace(REG_TAGS_KEY, om.createArrayNode()
            .add(v[CBSRegionType.TERRITORY])
            .add(v[CBSRegionType.NUTS1])
            .add(v[CBSRegionType.PROVINCE])
            .add(v[CBSRegionType.NUTS2])
            .add(v[CBSRegionType.COROP])
            .add(v[CBSRegionType.NUTS3]))
        cr.with(CBSRegionType.MUNICIPAL.prefix)
            .replace(gm, if (gmNode.isObject)
                    (gmNode as ObjectNode).set(REG_TAGS_KEY, om.valueToTree(v.values))
                else
                    gmNode)
    }

    companion object {
        /**  */
        private val LOG: Logger = getLogger()
        private const val FILE_BASE = "../episim-demo/dist/"
        private const val FILE_NAME = "83287NED.json"

        @Throws(IOException::class)
        @JvmStatic
        fun main(args: Array<String>) {
            if (System.getProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY) == null)
                (FILE_BASE + "log4j2.yaml").toInputStream().use {
                    // see https://stackoverflow.com/a/42524443
                    val ctx: LoggerContext = LoggerContext.getContext(false)
                    ctx.start(YamlConfiguration(ctx, ConfigurationSource(it)))
                }
            (FILE_BASE + FILE_NAME).toInputStream().use { it ->
                val om = OBJECT_MAPPER
                val hier = om.readValue(it, CbsRegionHierarchy::class.java)
                LOG.info("GM regions: {}", hier.cityRegionsByType())
                LOG.info("Hierarchy: {}", hier.addAdminHierarchy(null).toJSON(om))
            }
        }

        const val REG_TAGS_KEY = "reg_tags"
    }
}