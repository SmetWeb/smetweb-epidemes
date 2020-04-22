package io.smetweb.epidemes.disease.infection

import com.fasterxml.jackson.annotation.JsonValue

/**
 * [MSEIRSCompartment] enumerates several common [EpiCompartment]s as used in
 * [compartmental models](https://en.wikipedia.org/wiki/Compartmental_models_in_epidemiology)
 * (e.g. SI/SIS/SIR/SIRS/SEIS/SEIR/MSIR/MSEIR/MSEIRS)
 *
 * <p>
 * from Zhang (2016)?: typical influenza life cycle (compartment transitions): - passively
 * immune - susceptible - vaccine-infected : latent (incubation-period),
 * infectious, recovered (1-2y), susceptible - contact-infected : latent
 * (incubation-period) - asymptomatic, infectious, recovering, removed (2-7y),
 * susceptible - symptomatic - mobile, recovering, removed (2-7y), susceptible -
 * immobilize - convalescent, removed (2-7y), susceptible - hospitalize -
 * convalescent, removed (2-7y), susceptible - death, removed
 */
enum class MSEIRSCompartment(
		override val infective: Boolean,
		override val susceptible: Boolean
) : EpiCompartment {

	/**
	 * [EpiCompartment] for MATERNALLY DERIVED or PASSIVELY IMMUNE
	 * infants, e.g. naturally (due to maternal antibodies in placenta and
	 * colostrum) or artificially (induced via antibody-transfer). See
	 * https://www.wikiwand.com/en/Passive_immunity
	 */
	MATERNAL_IMMUNE(false, false),

	/**
	 * [EpiCompartment] for SUSCEPTIBLE individuals (post-vaccination)
	 */
	SUSCEPTIBLE(false, true),

	/**
	 * [EpiCompartment] for EXPOSED individuals, i.e. LATENT INFECTED or
	 * PRE-INFECTIVE carriers
	 */
	EXPOSED(false, false),

	/**
	 * [EpiCompartment] for primary INFECTIVE individuals, currently able
	 * to transmit disease by causing secondary infections
	 */
	INFECTIVE(true, false),

	/**
	 * [EpiCompartment] for individuals RECOVERED from the disease, and
	 * naturally IMMUNE (vis-a-vis [VACCINATED] or acquired immune),
	 * possibly waning again into [SUSCEPTIBLE]
	 */
	RECOVERED(false, false),

	/**
	 * [EpiCompartment] for susceptible NEWBORN individuals, after
	 * maternal immunity waned and before a (parental) vaccination decision
	 * is made
	 */
	NEWBORN(false, true),

	/**
	 * [EpiCompartment] for VACCINATED individuals having acquired
	 * immunity, possibly waning again into [SUSCEPTIBLE]
	 */
	VACCINATED(false, false),

	/**
	 * [EpiCompartment] for susceptible but DORMANT individuals having
	 * recovered from prior exposure, but who will not become
	 * [INFECTIVE] before being re-[EXPOSED]
	 */
	DORMANT(false, true);

	@JsonValue
	fun json(): String = this.name.substring(0, 1)

	override val value = this.name

}