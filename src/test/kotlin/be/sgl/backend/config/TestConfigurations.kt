package be.sgl.backend.config

import be.sgl.backend.service.payment.CheckoutProvider
import org.mockito.Mockito.mock
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.mail.javamail.JavaMailSender

@TestConfiguration
class TestConfigurations {
    @Bean
    @Primary
    fun checkoutProvider(): CheckoutProvider {
        return MockedCheckoutProvider()
    }
}