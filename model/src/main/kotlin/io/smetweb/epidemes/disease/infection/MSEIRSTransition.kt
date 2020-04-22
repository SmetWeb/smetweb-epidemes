package io.smetweb.epidemes.disease.infection

import com.fasterxml.jackson.annotation.JsonValue


/**
 * [MSEIRSTransition] see [compartmental models](https://en.wikipedia.org/wiki/Compartmental_models_in_epidemiology)
 */
enum class MSEIRSTransition(
		override val outcome: EpiCompartment
) : EpiTransition {
	/**
	 * M  S or M  N: passive/maternal immunity waning (frequency
	 * = , mean period = *<sup>-1</sup>*)
	 */
	PASSIVE(MSEIRSCompartment.NEWBORN),

	/**
	 * S  E (or S  I): pre-exposure contact or infection
	 * (frequency = , mean period = *<sup>-1</sup>*)
	 */
	SUSCEPTIBILITY(MSEIRSCompartment.EXPOSED),

	/**
	 * E  I: viral loading/incubation (frequency = , mean
	 * period = *<sup>-1</sup>*)
	 */
	LATENCY(MSEIRSCompartment.INFECTIVE),

	/**
	 * I  R: infectious/recovery (frequency = , mean period =
	 * *<sup>-1</sup>*)
	 */
	INFECTIOUS(MSEIRSCompartment.RECOVERED),

	/**
	 * N  V (or S  V): pre-vaccination (newborn)
	 * susceptibility/acceptance/hesitancy (frequency = , mean period =
	 * *<sup>-1</sup>*), possibly conditional on beliefs
	 */
	ACCEPTANCE(MSEIRSCompartment.VACCINATED),

	/**
	 * R  S: wane/decline (frequency = , mean period =
	 * *<sup>-1</sup>*), possibly conditional on co-morbidity,
	 * genetic factors, ...
	 */
	WANING_PASSIVE(MSEIRSCompartment.DORMANT),

	/**
	 * R  S: wane/decline (frequency = , mean period =
	 * *<sup>-1</sup>*), possibly conditional on co-morbidity,
	 * genetic factors, ...
	 */
	WANING_NATURAL(MSEIRSCompartment.DORMANT),

	/**
	 * R  S: wane/decline (frequency = , mean period =
	 * *<sup>-1</sup>*), possibly conditional on co-morbidity,
	 * genetic factors, ...
	 */
	WANING_ACQUIRED(MSEIRSCompartment.DORMANT);

	/**
	 * E  : dying/mortality (frequency = , mean period =
	 * *<sup>-1</sup>*)
	 */
	//		MORTALITY( null ),

	/**
	 * normal/absent  apparent/sub-/clinical: symptom
	 * latency/incubation
	 */
	//		APPEARANCE,

	/**
	 * apparent/sub-/clinical  normal/asymptomatic:
	 * morbidity/apparent/disabled
	 */
	//		DISAPPEARANCE,
	//

	private val json: String by lazy {
		this.name.toLowerCase().replace("_", "-")
	}

	@JsonValue
	fun json(): String = this.json
}
