package be.sgl.backend.config.security

import be.sgl.backend.util.ForExternalOrganization
import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import mu.KotlinLogging
import org.springframework.stereotype.Component

@Component
@ForExternalOrganization
class BearerTokenFilter : Filter {

    companion object {

        private val logger = KotlinLogging.logger {}
        private val tokenHolder = ThreadLocal<String?>()

        fun getToken(): String? {
            val token = tokenHolder.get()
            logger.debug { "Fetched token: ${token?.take(20)}" }
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
