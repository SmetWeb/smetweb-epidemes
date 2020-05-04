package io.smetweb.epidemes.data.cbs

import com.fasterxml.jackson.annotation.JsonValue

enum class CBSRegionType(
        @JsonValue
        val prefix: String,
        private val format: String? = null
) {
    /** land  */
    COUNTRY("NL"),

    /** NUTS1  */
    NUTS1("NL", "NL%1d"),

    /** landsdeel  */
    TERRITORY("LD", "LD%02d"),

    /** NUTS2  */
    NUTS2("NL", "NL%02d"),

    /** provincie  */
    PROVINCE("PV", "PV%02d"),

    /** NUTS3  */
    NUTS3("NL", "NL%03d"),

    /** corop (analytical region)  */
    COROP("CR", "CR%02d"),

    /** COROP-subgebied  */
    COROP_SUB("CS"),

    /** COROP-plusgebied  */
    COROP_PLUS("CP"),

    /** gemeente, e.g. 'GM0003' (Appingedam)  */
    MUNICIPAL("GM", "GM%04d"),

    /** Woonplaats  */
    CITY("WP"),

    /** wijk, e.g. 'WK000300'  */
    WARD("WK", "WK%04d%02d"),

    /** buurt, e.g. 'BU00030000'  */
    BOROUGH("BU", "BU%04d%02d%02d"),

    /** 4-number zip code area  */
    ZIP4("PC4", "%s"),

    /** 2-letter zip code street '  */
    ZIP6("PC6", "%s%s"),

    /** arbeidsmarktregio (GEM+UWV)  */
    LABOR_MARKET("AM"),

    /** arrondissement (rechtbank)  */
    DISTRICT("AR"),

    /** GGD-regio  */
    HEALTH_SERVICES("GG"),

    /** Jeugdzorgregio  */
    CHILD_SERVICES("JZ"),

    /** Kamer van Koophandel  */
    COMMERCE("KK"),

    /** Landbouwgebied  */
    AGRICULTURE("LB"),

    /** Landbouwgebied-groep  */
    AGRI_GROUP("LG"),

    /** Politie regionale eenheid  */
    POLICE("RE"),

    /** Ressorten (gerechtshof)  */
    APPEAL_COURT("RT"),

    /** RPA-gebied  */
    LABOR_PLATFORM("RP"),

    /** Toeristengebied  */
    TOURISM("TR"),

    /** Veiligheidsregio  */
    SAFETY("VR"),

    /** WGR-samenwerkingsgebied  */
    MUNICIPAL_COOP("WG"),

    /** Zorgkantoorregio  */
    HEALTH_WELFARE("ZK");

    fun <T> format(vararg args: T): String =
            this.format?.let { String.format(it, *args) }
                    ?: this.prefix

    companion object {
        @JvmStatic
        fun parse(regionId: CharSequence): CBSRegionType {
            val s = regionId.trim { it <= ' ' }
            if (s.isEmpty()) {
                throw IllegalArgumentException("Unknown region type for: $regionId")
            }

            // FIXME replace by regular expressions?
            if (s.substring(0, 2).equals("NL", ignoreCase = true))
                return when (s.length) {
                    3 -> NUTS1
                    4 -> if (s[2] == '0') COUNTRY else NUTS2
                    5 -> NUTS3
                    else -> throw IllegalArgumentException("Unknown region type for: $regionId")
                }
            return values().firstOrNull { s.startsWith(it.prefix, ignoreCase = true) }
                    ?: throw IllegalArgumentException("Unknown region type for: $regionId")
        }
    }
}