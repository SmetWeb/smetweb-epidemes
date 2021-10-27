package io.smetweb.random

import java.util.function.Supplier

interface ProbabilityDistribution<T>: Supplier<T> {

	fun draw(): T

	override fun get(): T = draw()

}