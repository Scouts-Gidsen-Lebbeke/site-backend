package be.sgl.backend.shared.config

import org.springframework.security.oauth2.jwt.Jwt
import java.time.Instant

object CustomUserDetailsTestMother {

    fun jwt() = JwtBuilder()

    fun userDetails() = JwtBuilder()

    /**
     * Builds a Keycloak-like [Jwt]; [build] turns it into [CustomUserDetails], [buildJwt] returns the raw token.
     */
    class JwtBuilder {
        private var username: String? = "username"
        private var firstName: String? = "firstName"
        private var lastName: String? = "lastName"
        private var email: String? = "firstName.lastName@corp.com"
        private var externalId: String? = "externalId"
        private var roles: List<String>? = null

        fun username(username: String?) = apply { this.username = username }

        fun firstName(firstName: String?) = apply { this.firstName = firstName }

        fun lastName(lastName: String?) = apply { this.lastName = lastName }

        fun email(email: String?) = apply { this.email = email }

        fun externalId(externalId: String?) = apply { this.externalId = externalId }

        fun roles(vararg roles: String) = apply { this.roles = roles.toList() }

        fun buildJwt(): Jwt {
            val builder = Jwt.withTokenValue("token")
                .header("alg", "none")
                .issuedAt(Instant.EPOCH)
                .expiresAt(Instant.EPOCH.plusSeconds(60))
                .claim("iss", "issuer")
            username?.let { builder.claim("preferred_username", it) }
            firstName?.let { builder.claim("given_name", it) }
            lastName?.let { builder.claim("family_name", it) }
            email?.let { builder.claim("email", it) }
            externalId?.let { builder.claim("sub", it) }
            roles?.let { builder.claim("roles", it) }
            return builder.build()
        }

        fun build() = CustomUserDetails(buildJwt())
    }
}
