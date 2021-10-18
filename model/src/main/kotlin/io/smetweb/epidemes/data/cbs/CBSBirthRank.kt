package io.smetweb.epidemes.data.cbs

enum class CBSBirthRank(
    private val jsonKey: String
) {

    /**  */
    FIRST("sib_0"),

    /**  */
    SECOND("sib_1"),

    /**  */
    THIRD("sib_2"),

    /**  */
    FOURPLUS("sib_3plus");

    fun jsonKey(): String =
        jsonKey

    fun rank(): Int =
        ordinal

    fun minusOne(): CBSBirthRank =
        values()[(ordinal - 1).coerceAtLeast(0)]

    fun plusOne(): CBSBirthRank =
        values()[(ordinal + 1).coerceAtMost(values().size - 1)]
}