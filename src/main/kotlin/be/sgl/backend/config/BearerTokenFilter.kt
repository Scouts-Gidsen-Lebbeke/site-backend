package be.sgl.backend.config

import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.FilterConfig
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Component

@Component
class BearerTokenFilter : Filter {

    companion object {
        private val tokenHolder = ThreadLocal<String?>()

        fun getToken(): String? = tokenHolder.get()
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

    override fun init(filterConfig: FilterConfig?) {}
    override fun destroy() {}
}
