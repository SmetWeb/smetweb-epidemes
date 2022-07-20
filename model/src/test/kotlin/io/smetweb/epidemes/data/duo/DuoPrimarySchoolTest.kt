package io.smetweb.epidemes.data.duo

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import io.smetweb.epidemes.data.bag.BagZipcode4RegionPolygons
import io.smetweb.epidemes.data.bag.BagZipcode6Locations
import io.smetweb.epidemes.data.duo.DuoPrimarySchool.Col.*
import io.smetweb.epidemes.data.duo.DuoPrimarySchool.EduCol.*
import io.smetweb.epidemes.data.cbs.CBSRegionType
import io.smetweb.epidemes.data.cbs.CbsRegionHierarchy
import io.smetweb.file.toInputStream
import io.smetweb.file.toOutputStream
import io.smetweb.json.OBJECT_MAPPER
import io.smetweb.json.readArrayAsync
import io.smetweb.log.getLogger
import io.smetweb.random.ConditionalDistribution
import io.smetweb.random.ProbabilityDistribution
import io.smetweb.random.PseudoRandomEcj
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import java.util.stream.Stream
import kotlin.collections.HashMap
import kotlin.collections.HashSet

class DuoPrimarySchoolTest {


    companion object {
        val log: Logger = getLogger()

        private const val pc4GemFile = "../dist/adm_pc4_2016_basis.json"
        private const val pc6GeoFile = "dist/bag_pc6_2016_01.json"
        private const val fileName = "03.-leerlingen-po-totaaloverzicht-2015-2016.csv"
        const val outFileName = "dist/region-primary-school-students.json"

        @BeforeAll
        @JvmStatic
        internal fun `generate output file`() {
            val pc4Gem = readArrayAsync(BagZipcode4RegionPolygons::class.java) { pc4GemFile.toInputStream() }
                .collect({ TreeMap<String, String>() }) { m, v ->
                    m["%04d".format(v.zip)] = CBSRegionType.MUNICIPAL.format(v.gm)
                }
                .blockingGet()
            log.info("Reading geographic positions of zipcodes from {}", pc6GeoFile)
            val pc6stat = readArrayAsync(BagZipcode6Locations::class.java) { pc6GeoFile.toInputStream() }
                .collectInto(TreeMap<String, BagZipcode6Locations>()) { m, v -> m[v.zip!!] = v }
                .blockingGet()
            val gmCultAgeSchoolPos: ObjectNode = OBJECT_MAPPER.createObjectNode()
            val missing: MutableSet<String> = HashSet()
            Files.lines(
                Paths.get("dist", fileName),
                Charset.forName("ISO-8859-1")
            ).use { stream ->
                stream.skip(1)
                    .map { line -> line.split(";") }
                    .forEach { v ->
                        val pc4Gm = pc4Gem[v.get(POSTCODE_LEERLING.ordinal)] ?: return@forEach
                        // ANDERS/BUITENLAND
                        val stat: BagZipcode6Locations? = pc6stat[v[POSTCODE_VESTIGING.ordinal]]
                        if (stat == null) {
                            missing.add(v[POSTCODE_VESTIGING.ordinal])
                            return@forEach
                        }
                        val id: String = v[BRIN_NUMMER.ordinal] + "_" + v[VESTIGINGSNUMMER.ordinal]

                        gmCultAgeSchoolPos //
                            .with(DuoPrimarySchool.ZIPDIST_KEY).with(pc4Gm)
                            .with(v[POSTCODE_LEERLING.ordinal])
                            .put(id, Integer.valueOf(v[TOTAAL.ordinal]))

                        val coords = stat.geo!!.coords
                        gmCultAgeSchoolPos.with(DuoPrimarySchool.SCHOOLS_KEY)
                            .putArray(id) // following order of ExportCol enumeration
                            .add(CBSRegionType.MUNICIPAL.format(v[GEMEENTENUMMER.ordinal].toInt()))
                            .add(v[SOORT_PO.ordinal])
                            .add(v[DENOMINATIE_VESTIGING.ordinal])
                            .add(v[BRIN_NUMMER.ordinal])
                            .add(v[VESTIGINGSNUMMER.ordinal])
                            .add(v[POSTCODE_VESTIGING.ordinal])
                            .add(coords[1])
                            .add(coords[0])
                    }
            }

            // put in administrative hierarchy...
            val gmRegionFile = "dist/83287NED.json"
            gmRegionFile.toInputStream().use { `is` ->
                outFileName.toOutputStream(false).use { os ->
                    val gmRegs = OBJECT_MAPPER.readValue(`is`, CbsRegionHierarchy::class.java)
                    gmCultAgeSchoolPos.replace(
                        DuoPrimarySchool.ZIPDIST_KEY, gmRegs.addAdminHierarchy(
                            gmCultAgeSchoolPos.with(
                                DuoPrimarySchool.ZIPDIST_KEY
                            )
                        )
                    )
                    OBJECT_MAPPER.writer().withDefaultPrettyPrinter().writeValue(os, gmCultAgeSchoolPos)
                }
            }
            log.warn("Missing stats for PC6 addresses: {}", missing)
        }
    }

    @Test
    fun `test dist`() {
        outFileName.toInputStream().use { inputStream ->
            log.debug("Creating RNG...")
            val rng = PseudoRandomEcj()
            log.debug("Aggregating into dist...")
            val poLUTHER = "po_luther"
            val poSTEINER = "po_steiner"
            val poSPECIAL = "po_special"
            val poALL = "po_all"
            val schoolCache: MutableMap<String, Map<DuoPrimarySchool.EduCol, JsonNode>> = HashMap()
            val zipCultDists: MutableMap<String, MutableMap<String, ProbabilityDistribution<String>>> =
                DuoPrimarySchool.parse(inputStream, rng) { id, arr ->
                    schoolCache.computeIfAbsent(id) { arr }
                    val denom: String = arr[DENOMINATIE]!!.asText()
                    if (denom.startsWith("Prot") // 23.1%
                        || denom.startsWith("Geref") // 1.2%
                        || denom.startsWith("Evan") // 0.1%
                    )
                        return@parse Stream.of(poLUTHER, poALL)
                    if (denom.startsWith("Antro")) // 0.9%
                        return@parse Stream.of(poSTEINER, poALL)
                    val type: String = arr[PO_SOORT]!!.asText()
                    if (type.startsWith("S") || type.contains("s"))
                        return@parse Stream.of(poSPECIAL, poALL)
                    Stream.of(poALL)
                }
            val pc4SchoolDist =
                ConditionalDistribution.of { zipCult: Array<Any> ->
                    zipCultDists
                        .computeIfAbsent(zipCult[0].toString()) { mutableMapOf() }
                        .computeIfAbsent(zipCult[1].toString()) { zipCultDists[zipCult[0]]!![poALL]!! }
                }
            log.debug("Testing dist fallback...")
            zipCultDists.keys.stream()
                .sorted()
                .limit(10)
                .forEach { pc4: String ->
                    listOf(poLUTHER, poSTEINER, poSPECIAL, poALL).stream()
                        .forEach { cat ->
                            log.debug("...draw for {} x {}: {}", pc4, cat, schoolCache[pc4SchoolDist.draw(arrayOf(pc4, cat))])
                        }
                }
        }
    }
}