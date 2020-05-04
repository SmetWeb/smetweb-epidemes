package io.smetweb.epidemes.deme

import io.smetweb.epidemes.data.cbs.CBSRegionType
import io.smetweb.math.ONE_HALF
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import java.math.BigDecimal

@ConstructorBinding
@ConfigurationProperties(prefix = "demography")
data class DemeConfig(

        val populationSize: Long = 500_000,

        // FIXME what is this for?
        val populationSizeRef: Long = 17_000_000,

        val hhPartnerAgeDeltaRange: String = "normal(-2 year; 2 year)",

        val maleFreq: BigDecimal = ONE_HALF,

        val regionalResolution: CBSRegionType = CBSRegionType.MUNICIPAL


)