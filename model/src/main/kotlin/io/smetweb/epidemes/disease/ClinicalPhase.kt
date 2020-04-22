package io.smetweb.epidemes.disease

import io.smetweb.ref.Ref

/**
 * [ClinicalPhase] is a [Ref] for clinical/symptomatic phases
 */
interface ClinicalPhase: Ref<String> {

	/**
	 * symptoms can be observed, by self and/or others, laymen and/or specialists
	 */
	val apparent: Boolean

	/**
	 * clinical attention or treatment may be appropriate
	 */
	val clinical: Boolean


}