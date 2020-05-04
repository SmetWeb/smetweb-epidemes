package io.smetweb.math

import org.apfloat.Apfloat
import org.apfloat.ApfloatMath.*
import si.uom.NonSI
import tec.uom.se.ComparableQuantity
import tec.uom.se.unit.Units
import javax.measure.Quantity
import javax.measure.Unit
import javax.measure.quantity.Angle

data class LatLong(
        val latitude: Quantity<Angle>,
        val longitude: Quantity<Angle>,
        val unit: Unit<Angle> = latitude.unit
) {
    constructor(latitude: Number, longitude: Number, unit: Unit<Angle> = NonSI.DEGREE_ANGLE): this(
            latitude = latitude.toQuantity(unit), longitude = longitude.toQuantity(unit), unit = unit)

    private val latitudeRadians: Apfloat by lazy { this.latitude.to(Units.RADIAN).value.toApfloat() }

    private val longitudeRadians: Apfloat by lazy { this.longitude.to(Units.RADIAN).value.toApfloat() }

    fun angularDistance(that: LatLong, unit: Unit<Angle> = this.unit): ComparableQuantity<Angle> {
        val lat1: Apfloat = this.latitudeRadians
        val lon1: Apfloat = this.longitudeRadians
        val lat2: Apfloat = that.latitudeRadians
        val lon2: Apfloat = that.longitudeRadians
        val result: Apfloat = TWO.multiply(asin(sqrt(
                pow(sin(abs(lat1.subtract(lat2)).divide(TWO)), TWO).add(
                        cos(lat1).multiply(cos(lat2)).multiply(
                                pow(sin(abs(lon1.subtract(lon2)).divide(TWO)), TWO))))))
                .precision(minOf(lat1.precision(), lon1.precision(),
                        lat2.precision(), lon2.precision()))
        // NOTE avoid using (standard) ComparableQuantity#to(Unit) !!
        return result.toDecimal().toQuantity(Units.RADIAN).toUnit(unit)
    }
}