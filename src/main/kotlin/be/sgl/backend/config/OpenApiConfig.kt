package be.sgl.backend.config

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.models.ExternalDocumentation
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.annotations.security.SecurityScheme
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.servers.Server
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT"
)
class OpenApiConfig {

    @Value("\${spring.application.base-url}")
    private lateinit var baseUrl: String

    @Bean
    fun customOpenAPI(): OpenAPI {
        return OpenAPI()
            .info(Info()
                .title("Site backend")
                .description("Endpoints for managing a youth organisation with a membership plan.")
                .contact(Contact().name("support").email("webmaster@scoutslebbeke.be"))
                .license(License().name("AGPL-3.0").url("https://www.gnu.org/licenses/agpl-3.0.html"))
            )
            .externalDocs(ExternalDocumentation()
                .description("Source code")
                .url("https://github.com/Scouts-Gidsen-Lebbeke/site-backend")
            )
            .servers(listOf(
                Server().url("$baseUrl/api").description("Current")
            ))
    }
}