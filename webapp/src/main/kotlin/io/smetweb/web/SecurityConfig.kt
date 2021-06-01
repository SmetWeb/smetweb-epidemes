package io.smetweb.web

import org.springframework.context.annotation.Bean
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain

// see https://www.baeldung.com/spring-security-5-reactive#1-bootstrapping-the-reactive-application
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
class SecurityConfig {

    @Bean
    fun springSecurityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http
                .csrf().disable()
                .authorizeExchange()
                .pathMatchers(HttpMethod.POST, "/admin").hasRole("ADMIN")
//                .pathMatchers("/api-docs**").permitAll() // e.g. '.yaml' extension
//                .pathMatchers("/api-docs/**").permitAll()
//                .pathMatchers("/swagger-ui/**").permitAll()
//                .pathMatchers("/h2-console/**").permitAll() // requires servlet engine (e.g. Tomcat)
                .pathMatchers("/**").permitAll()
                .anyExchange().authenticated()
//              .and().httpBasic()
                .and().formLogin()
                .and().build();
    }

}
