package io.smetweb.epidemes.deme

import io.smetweb.math.LatLong
import io.smetweb.refer.Named
import javax.measure.Quantity
import javax.measure.quantity.Area

interface Region: Named {

    val type: RegionType

    val localName: String?
        get() = this.nameRef.get().toString()

    val parent: Region?
        get() = null

    val children: Collection<Region>
        get() = emptySet()

    val centroid: LatLong?
        get() = null

    val surfaceArea: Quantity<Area>?
        get() = null

    interface Habitat<T: Any>: Region {
        val inhabitants: Collection<T>
    }
}