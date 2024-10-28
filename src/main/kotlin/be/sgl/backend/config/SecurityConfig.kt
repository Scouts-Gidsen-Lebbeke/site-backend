package be.sgl.backend.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
class SecurityConfig {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .authorizeHttpRequests { authorizeRequests ->
                authorizeRequests
                    .requestMatchers("/private").authenticated()
                    .anyRequest().permitAll()
            }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt {
                    // it.jwkSetUri("https://login.scoutsengidsenvlaanderen.be/auth/realms/scouts/protocol/openid-connect/certs")
                }
            }
//            .oauth2Login { oauth2Login ->
//                oauth2Login
//                    .authorizationEndpoint { authEndpoint ->
//                        authEndpoint.baseUri("/oauth2/authorization")
//                    }
//                    .defaultSuccessUrl("/private")
//            }
        return http.build()
    }
}