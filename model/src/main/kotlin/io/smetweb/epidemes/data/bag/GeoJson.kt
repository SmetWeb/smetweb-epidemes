package io.smetweb.epidemes.data.bag

import com.fasterxml.jackson.annotation.JsonProperty

class GeoJson {
    @JsonProperty("type")
    var type: String? = null

    @JsonProperty("coordinates")
    lateinit var coords: FloatArray
}