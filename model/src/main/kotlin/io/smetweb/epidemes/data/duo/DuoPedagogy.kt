package io.smetweb.epidemes.data.duo

import com.fasterxml.jackson.databind.JsonNode
import io.smetweb.epidemes.data.duo.DuoPrimarySchool.EduCol
import java.util.*

enum class DuoPedagogy {

    /** Protestant (Luther), Calvinistisch (Calvin) Gereformeerd (Reformed)/Vrijgemaakt (Liberated)/Evangelistisch (Orthodox) */
    REFORMED,

    /** Steiner/Waldorf (anthroposofical)  */
    ALTERNATIVE,

    /** special needs (speciaal onderwijs)  */
    SPECIAL,

    /** public, catholic, islamic, mixed  */
    OTHERS,

    /** unknown/all  */
    ALL;

    companion object {

        private val DUO_CACHE: MutableMap<String, DuoPedagogy> = HashMap()

        fun resolveDuo(school: Map<EduCol, JsonNode>): DuoPedagogy {
            val type: String = school[EduCol.PO_SOORT]!!.asText()
            val denom: String = school[EduCol.DENOMINATIE]!!.asText()
            val key = type + denom
            //				System.err.println( type + " :: " + denom + " -> " + result );
            return DUO_CACHE.computeIfAbsent(key) {
                if (
                //denom.startsWith( "Prot" ) || // 23.1%
                    denom.startsWith("Geref") // 1.2%
                    || denom.startsWith("Evan") // 0.1%
                )
                    REFORMED
                else if (denom.startsWith("Antro")) // 0.9%
                    ALTERNATIVE
                else if (type.startsWith("S") || type.contains("s"))
                    SPECIAL
                else
                    OTHERS
            }
        }
    }
}