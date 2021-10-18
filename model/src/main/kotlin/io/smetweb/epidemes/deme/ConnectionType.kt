package io.smetweb.epidemes.deme

/** [ConnectionType] determines behavioral/physical contagion */
interface ConnectionType {

    /** affects attitude  */ // e.g. peer, media, authority, ...
    val isInformative: Boolean

    /** affects infections  */ // e.g. fellow, (class)mate, colleague, ...
    val isContiguous: Boolean

    /** affects STD infections  */ // e.g. lover, spouse, ...
    val isSexual: Boolean

    //	/** affects attitude */
    //	boolean isRepresentative: Boolean // e.g. (foster/step) parent, guardian, ...

}