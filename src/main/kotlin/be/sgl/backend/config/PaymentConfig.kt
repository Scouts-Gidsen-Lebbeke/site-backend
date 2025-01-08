package be.sgl.backend.config

import be.woutschoovaerts.mollie.Client
import be.woutschoovaerts.mollie.ClientBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class PaymentConfig {

    @Bean
    @ConditionalOnProperty(name = ["mollie.api.key"])
    fun mollieApiClient(@Value("\${mollie.api.key}") apiKey: String): Client {
        return ClientBuilder().withApiKey(apiKey).build()
    }
}