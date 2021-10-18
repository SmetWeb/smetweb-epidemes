package io.smetweb.epidemes.data.bag

import com.fasterxml.jackson.annotation.JsonAnyGetter
import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.smetweb.file.toInputStream
import io.smetweb.json.readArrayAsync
import io.smetweb.json.stringify
import io.smetweb.log.getLogger

/**
 * [BagZipcode4RegionPolygons] basic address data:
 *
 * ```
 * {
 * "pc4":1023,
 * "vr_code":"VR13",
 * "gg_code":"GG3406",
 * "shape":{"type":"Polygon",
 * "coordinates":[[[4.968445637092872, 52.38812065054079], ... [..., ...]]]},
 * "ggd":"GGD Amsterdam",
 * "pc4_naam":"Nieuwendammerdijk/Buiksloterdijk",
 * "provincie":"Noord-Holland",
 * "veiligheidsregio":"Amsterdam-Amstelland",
 * "woonplaats_2":"AMSTERDAM",
 * "gem_nr":363,
 * "bev_2015":4610,
 * "bev_2016":4630,
 * "pv_code":"PV27",
 * "woonplaats_nen":"AMSTERDAM",
 * "gemeente":"Amsterdam",
 * "objectid":12
 * }
 * ```
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class BagZipcode4RegionPolygons(
    @JsonProperty("pc4")
    var zip: Int = 0,

    @JsonProperty("gem_nr")
    var gm: Int = 0

) {
    private val ignoredValues: MutableMap<String, Any> = HashMap()

    @JsonAnySetter
    /* operator */ fun set(key: String, value: Any) {
        ignoredValues[key] = value
    }

    @JsonAnyGetter
    operator fun get(key: String): Any? =
        ignoredValues[key]

    override fun toString(): String =
        stringify()

    companion object {

        private val LOG = getLogger()
        private const val FILE_NAME = "dist/adm_pc4_2016_basis.json"

        @JvmStatic
        fun main(args: Array<String>) {
            val array = readArrayAsync(BagZipcode4RegionPolygons::class.java) { FILE_NAME.toInputStream() }
            LOG.info("Row: {}", array.blockingFirst())
        }
    }
}