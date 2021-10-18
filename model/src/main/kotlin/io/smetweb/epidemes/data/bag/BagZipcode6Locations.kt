package io.smetweb.epidemes.data.bag

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.smetweb.file.toInputStream
import io.smetweb.json.readArrayAsync
import io.smetweb.json.stringify
import io.smetweb.json.toJSON
import io.smetweb.log.getLogger
import io.smetweb.log.lazyString
import org.slf4j.Logger
import java.util.*

/**
 * [BagZipcode6Locations] basic address data:
 *
 * ```
 * {wptot=110,
 * bagcnt=47,
 * wpfull=96,
 * openbareruimtenaam=Spuistraat,
 * postcode=1012SV,
 * avgy=487549,
 * bedrcnt=22,
 * avgx=121206,
 * gmpwont=0,
 * woonplaatsnaam=Amsterdam,
 * pc6pers=42,
 * meandist=78,
 * geometrie={type=Point, coordinates=[4.890940341920423, 52.374772925483896]},
 * objectid=156,
 * pc6won=40}
 * ```
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class BagZipcode6Locations {

    @JsonProperty("postcode")
    var zip: String? = null

    @JsonProperty("woonplaatsnaam")
    var city: String? = null

    @JsonProperty("openbareruimtenaam")
    var street: String? = null

    @JsonProperty("pc6won")
    var hh = 0

    @JsonProperty("pc6pers")
    var ppl = 0

    @JsonProperty("bedrcnt")
    var org = 0

    @JsonProperty("wpfull")
    var hr = 0

    @JsonProperty("geometrie")
    var geo: GeoJson? = null

    override fun toString(): String =
        stringify()

    companion object {
        private val LOG: Logger = getLogger()
        private const val FILE_NAME = "dist/bag_pc6_2016_01.json"

        @Throws(Exception::class)
        @JvmStatic
        fun main(args: Array<String>) {
            val result = readArrayAsync(BagZipcode6Locations::class.java) { FILE_NAME.toInputStream() }
                .take(100)
                .collectInto(TreeMap<String, BagZipcode6Locations>()) { m, v -> m[v.zip!!] = v }
                .blockingGet()
            LOG.info("Mapping: {}", lazyString { result.toJSON() })
        }
    }
}