package io.smetweb.random

import tech.units.indriya.ComparableQuantity
import javax.measure.Quantity

interface QuantityDistribution<Q: Quantity<Q>>: ProbabilityDistribution<ComparableQuantity<Q>>