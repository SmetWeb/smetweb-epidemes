package io.smetweb.xml

import org.apache.commons.lang3.builder.ToStringStyle

class OmitNullsToStringStyle : ToStringStyle() {

	companion object {
		@JvmField val INSTANCE: ToStringStyle = OmitNullsToStringStyle()
	}

	init {
		isUseClassName = false
		isUseIdentityHashCode = false
	}

	override fun append(buffer: StringBuffer, fieldName: String, value: Any?, fullDetail: Boolean?) {
		if (value != null) {
			super.append(buffer, fieldName, value, fullDetail)
		}
	}
}

