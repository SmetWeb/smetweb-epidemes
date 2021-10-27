package io.smetweb.epidemes.medi.infect

import io.smetweb.epidemes.medi.ClinicalPhase

/**
 * [EpiClinicalPhase] represents [ClinicalPhase] relevant in epidemiological contexts
 */
enum class EpiClinicalPhase(
		override val apparent: Boolean,
		override val clinical: Boolean
): ClinicalPhase {

	/**
	 * no disease, pathogen, signs or symptoms (not if never
	 * exposed/invaded)
	 */
	ABSENT(false, false),

	/**
	 * affected/exposed but non-apparent, possibly 1. dormant (inactive, non
	 * growing); 2. asymptomatic/subclinical (not clinically observable); or
	 * 3. latent/occult (no visual signs or symptoms)
	 */
	LATENT(false, false),

	/**
	 * (sub)clinical signs or symptoms, possibly still obscure/non-apparent
	 */
	SYMPTOMATIC(true, false),

	/**
	 * visually apparent signs or symptoms, possibly still subclinical
	 */
	SYMPTOMATIC_APPARENT(true, false),

	/**
	 * apparent signs or symptoms of early onset, e.g. lack of appetite,
	 * fever/hyperthermia (measles), rhinorrhea (measles, flu, cold),
	 * conjuctivitis (measles, flu, cold).
	 */
	SYMPTOMATIC_PRODROMAL(true, true),

	/**
	 * apparent signs or symptoms throughout body, e.g. sepsis, cold, flu,
	 * mononucleosis (Pfeiffer due to the Epstein-Barr herpes virus),
	 * Streptococcal pharyngitis
	 */
	SYMPTOMATIC_SYSTEMIC(true, true),

	/**
	 * apparent signs or symptoms near recovery, e.g.
	 * [Reye's syndrome](https://en.wikipedia.org/wiki/Reye_syndrome) following influenza recovery
	 */
	SYMPTOMATIC_POSTDROMAL(true, true);

	override val value = name

}