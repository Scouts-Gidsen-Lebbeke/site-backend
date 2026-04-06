package be.sgl.backend.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.test.context.DynamicPropertyRegistrar
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.MariaDBContainer

@TestConfiguration(proxyBeanMethods = false)
class MailContainerConfiguration {

    @Bean
    fun mailhogContainer(): GenericContainer<*> {
        return GenericContainer("mailhog/mailhog:latest")
            .withExposedPorts(1025, 8025) // SMTP + Web UI
    }

    @Bean
    fun mailProperties(mailhogContainer: GenericContainer<*>): DynamicPropertyRegistrar {
        return DynamicPropertyRegistrar { registry ->
            registry.add("spring.mail.host") { mailhogContainer.host }
            registry.add("spring.mail.port") { mailhogContainer.getMappedPort(1025) }
            registry.add("spring.mail.username") { "" }
            registry.add("spring.mail.password") { "" }
        }
    }
}