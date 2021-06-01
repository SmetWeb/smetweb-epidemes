package io.smetweb.web

import org.hibernate.validator.constraints.URL
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.boot.context.properties.bind.DefaultValue
import org.springframework.validation.annotation.Validated
import javax.validation.constraints.Email
import javax.validation.constraints.NotBlank

@Validated
@ConstructorBinding
@ConfigurationProperties(prefix = "smetweb")
data class SmetWebConfig(

        @DefaultValue("8080")
        val httpPort: Int,

        // @field: required, see https://github.com/spring-projects/spring-boot/issues/22666

        @field:NotBlank
        val name: String,

        @field:Email
        val emailAddress: String = "",

        @field:URL
        val url: String = ""

)