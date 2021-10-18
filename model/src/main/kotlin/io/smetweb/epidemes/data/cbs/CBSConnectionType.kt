package io.smetweb.epidemes.data.cbs

import io.smetweb.epidemes.deme.ConnectionType

/**
 * [CBS definitions](https://www.cbs.nl/nl-nl/onze-diensten/methoden/begrippen?tab=p#id=positie-in-het-huishouden)
 */
enum class CBSConnectionType(
    override val isInformative: Boolean,
    override val isContiguous: Boolean,
    override val isSexual: Boolean
) : ConnectionType {

    /** e.g. online forum, media, authority, peer  */
    INFORM(true, false, false /* , false */),

    /** e.g. parent/child, mate, colleague, fellow, cohabiting  */
    SOCIAL(true, true, false /* , false */),

    /** e.g. casual, marital  */
    PARTNER(true, true, true /* , false */),

    /** e.g. bachelor, single parent  */
    SINGLE(true, true, false /* , false */),

    /** represented by guardian, e.g. biological/foster/step/co-parent  */
    WARD(false, true, false /* , true */);
}