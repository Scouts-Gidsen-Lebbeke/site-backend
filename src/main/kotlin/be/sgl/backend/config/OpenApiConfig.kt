package be.sgl.backend.config

import io.swagger.v3.oas.annotations.ExternalDocumentation
import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.info.Contact
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.annotations.info.License
import io.swagger.v3.oas.annotations.security.SecurityScheme
import org.springframework.context.annotation.Configuration

@Configuration
@OpenAPIDefinition(
    info = Info(
        title = "Site backend",
        description = "Endpoints for managing a youth organisation with a membership plan.",
        contact = Contact(name = "support", email = "webmaster@scoutslebbeke.be"),
        license = License(name = "AGPL-3.0", url = "https://www.gnu.org/licenses/agpl-3.0.html")
    ),
    externalDocs = ExternalDocumentation(
        description = "Source code",
        url = "https://github.com/Scouts-Gidsen-Lebbeke/site-backend"
    )
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT"
)
class OpenApiConfig