package be.sgl.backend.config

import be.woutschoovaerts.mollie.Client
import be.woutschoovaerts.mollie.ClientBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class MollieConfig {

    @Value("\${mollie.api.key}")
    private lateinit var apiKey: String

    @Bean
    fun mollieApiClient(): Client {
        return ClientBuilder().withApiKey(apiKey).build()
    }
}
