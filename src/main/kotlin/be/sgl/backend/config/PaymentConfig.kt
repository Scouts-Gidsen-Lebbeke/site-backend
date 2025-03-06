package be.sgl.backend.config

import be.sgl.backend.util.WhenNotBlank
import be.woutschoovaerts.mollie.Client
import be.woutschoovaerts.mollie.ClientBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class PaymentConfig {

    @Bean
    @WhenNotBlank("organization.api-key.mollie")
    fun mollieApiClient(@Value("\${organization.api-key.mollie}") apiKey: String): Client {
        return ClientBuilder().withApiKey(apiKey).build()
    }
}