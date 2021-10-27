package io.smetweb.epidemes.deme

import io.smetweb.epidemes.data.cbs.CBSRegionType
import io.smetweb.math.ONE_HALF
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import java.math.BigDecimal

@ConstructorBinding
@ConfigurationProperties(prefix = "demography")
data class DemeConfig(

	val module: Class<*> = DemeBrokerSimple::class.java,

	val populationSize: Long = 500_000,

	/** TODO read referent pop size from cbs data? */
	val referentPopulationSize: Long = 17_000_000,

	val hhPartnerAgeDeltaRange: String = "[-5 year; 1 year]",

	/** TODO draw partner age difference from CBS 37422, 37890, 60036ned? */
	val hhPartnerAgeDeltaDist: String = "normal(-2 year; 2 year)",

	val maleFreq: BigDecimal = ONE_HALF,

	// for demographic congruence accuracy
	val regionalResolution: CBSRegionType = CBSRegionType.MUNICIPAL,

	val hhDynamicsTimeSeries: String = "37230ned_TS_2012_2017.json",

	val hhBirthTimeSeries: String = "37201_TS_2010_2015.json",

	val hhAgeTimeSeries: String = "71486ned-TS-2010-2016.json",
) {

}