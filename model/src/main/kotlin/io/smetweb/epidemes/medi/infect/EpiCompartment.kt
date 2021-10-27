package io.smetweb.epidemes.medi.infect

import io.smetweb.refer.Ref


/**
 * [EpiCompartment] is a [Ref] for epidemiologic compartments as used in
 * [compartmental models](https://en.wikipedia.org/wiki/Compartmental_models_in_epidemiology)
 * (e.g. SI/SIS/SIR/SIRS/SEIS/SEIR/MSIR/MSEIR/MSEIRS) following terminology used in
 * [epidemic models](https://en.wikipedia.org/wiki/Epidemic_model)
 * and approaches for [mathematical modeling of infectious
 * disease](https://en.wikipedia.org/wiki/Mathematical_modelling_of_infectious_disease).
 *
 * @see [some examples (Dutch)](https://www.volkskrant.nl/kijkverder/2016/vaccinatie/)
 */
interface EpiCompartment: Ref<String> {

	/**
	 * `true` iff this compartment represents a primary or `INFECTIVE` status
	 */
	val infective: Boolean

	/**
	 * `true` iff this compartment represents a secondary or `SUSCEPTIBLE` status
	 */
	val susceptible: Boolean

}