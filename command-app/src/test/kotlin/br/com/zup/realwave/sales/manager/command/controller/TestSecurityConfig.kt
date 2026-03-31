package br.com.zup.realwave.sales.manager.command.controller

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain

/**
 * Permissive security configuration used only in controller slice tests.
 * Disables CSRF and permits all requests so that @WebMvcTest can focus
 * on controller logic without OAuth2/JWT setup overhead.
 */
@TestConfiguration
class TestSecurityConfig {

    @Bean
    fun testSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .authorizeHttpRequests { auth -> auth.anyRequest().permitAll() }
        return http.build()
    }
}
