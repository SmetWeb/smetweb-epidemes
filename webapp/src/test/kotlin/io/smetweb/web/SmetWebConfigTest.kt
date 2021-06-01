package io.smetweb.web

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import javax.validation.Validation
import javax.validation.Validator

@SpringBootTest//(classes = [SmetWebConfig::class])
@EnableConfigurationProperties(SmetWebConfig::class)
@TestPropertySource("classpath:application-config-validation-test.yml")
class SmetWebConfigTest {

    @Autowired
    private lateinit var smetweb: SmetWebConfig

    private val propertyValidator: Validator by lazy {
        Validation.buildDefaultValidatorFactory().validator
    }

    @Test
    fun `validate HTTP port`() {
        assertEquals(8081, smetweb.httpPort)
    }

    @Test
    fun `validate e-mail address`() {
        assertEquals(0, propertyValidator.validate(smetweb.emailAddress).size)
    }

}