package io.smetweb.epidemes.situ

import io.smetweb.refer.Ref

/** [Geography] refers to a distinct "lay of the land" (topology) of [regions][Region] */
sealed interface Geography: Ref<String> {

	enum class GeoType(override val value: String) : Geography {

		/** [DEFAULT] (administrative) geography, e.g. state, territory, county, zone, province, municipality, city, neighborhood */
		DEFAULT("admin"),

		/** the [HEALTH] geography, e.g. GGD */
		HEALTH("health"),

		/** the [EMERGENCY] (medical, police, fire dept, military) geography, e.g. COROP, zone */
		EMERGENCY("emergency"),

		/** the [RELIGION] geography, e.g. synod, diocese, parish */
		RELIGION("reli"),

		/** the [ELECTORAL]/political geography: constituency, precinct, district */
		ELECTORAL("elect"),

		/** the [OPERATIONS] geography, e.g. division, territory */
		OPERATIONS("ops"),

		;
	}
}