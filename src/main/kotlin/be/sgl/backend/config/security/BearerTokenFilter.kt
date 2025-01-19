package be.sgl.backend.config.security

import be.sgl.backend.service.user.ExternalOrganizationCondition
import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import mu.KotlinLogging
import org.springframework.context.annotation.Conditional
import org.springframework.stereotype.Component

@Component
@Conditional(ExternalOrganizationCondition::class)
class BearerTokenFilter : Filter {

    companion object {
        private val logger = KotlinLogging.logger {}
        private val tokenHolder = ThreadLocal<String?>()

        fun getToken(): String? {
            logger.info { "Fetching token..." }
            val token = tokenHolder.get()
            logger.info { "Fetched token: ${token?.take(20)}" }
            return token
        }
    }

    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        val httpRequest = request as HttpServletRequest
        val token = httpRequest.getHeader("Authorization")?.removePrefix("Bearer ")
        tokenHolder.set(token)
        try {
            chain.doFilter(request, response)
        } finally {
            tokenHolder.remove()
        }
    }
}
