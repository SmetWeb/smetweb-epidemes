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
 * [BagZipcode4RegionPolygons] or Basic Address Givens (data) maps each Dutch PC4-level zipcode (4 digits):
 * - zipcode digits (`pc4`) and area name (`pc4_naam`);
 * - city name (`woonplaats_nen`) and (`woonplaats_2`);
 * - municipality code (`gem_nr`) + name (`gemeente`);
 * - health region code (`gg_code`) + name (`ggd`);
 * - security region code (`vr_code`) + name (`veiligheidsregio`);
 * - province code (`pv_code`) + name (`provincie`); and
 * - geographic polygon (`coordinates`)
 *
 * See also [CBS](https://www.cbs.nl/nl-nl/dossier/nederland-regionaal/geografische-data/gegevens-per-postcode)
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
data class BagZipcode4RegionPolygons(
    @JsonProperty("pc4")
    var zip: Int = 0,

    @JsonProperty("gem_nr")
    var gm: Int = 0

) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    constructor(): this(zip = 0, gm = 0)

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