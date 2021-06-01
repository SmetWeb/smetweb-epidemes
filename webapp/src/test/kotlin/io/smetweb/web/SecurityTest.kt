package io.smetweb.web

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.reactive.server.WebTestClient

// see https://www.baeldung.com/spring-security-5-reactive#1-bootstrapping-the-reactive-application
@SpringBootTest(classes = [SmetWebApplication::class])
class SecurityTest(
        val context: ApplicationContext
) {

    lateinit var rest: WebTestClient

    @BeforeEach
    fun setup() {
        this.rest = WebTestClient
                .bindToApplicationContext(context)
                .configureClient()
                .build()
    }

    @Test
    fun whenNoCredentials_thenRedirectToLogin() {
        rest.get()
                .uri("/")
                .exchange()
                .expectStatus().is3xxRedirection
    }

    @Test
    @WithMockUser
    fun whenHasCredentials_thenSeesGreeting() {
        rest.get()
                .uri("/")
                .exchange()
                .expectStatus().isOk
                // FIXME extend to match landing page content
                // .expectBody(String::class.java).isEqualTo<Nothing>("Hello, user")
    }
}