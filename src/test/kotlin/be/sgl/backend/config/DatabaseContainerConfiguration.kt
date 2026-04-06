package be.sgl.backend.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.test.context.DynamicPropertyRegistrar
import org.testcontainers.containers.MariaDBContainer

@TestConfiguration(proxyBeanMethods = false)
class DatabaseContainerConfiguration {

    @Bean
    @ServiceConnection
    fun mariaDbContainer(): MariaDBContainer<*> {
        return MariaDBContainer("mariadb:11.3")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")
    }

    @Bean
    fun dbProperties(mariaDBContainer: MariaDBContainer<*>): DynamicPropertyRegistrar {
        return DynamicPropertyRegistrar { registry ->
            registry.add("spring.datasource.url") { mariaDBContainer.jdbcUrl }
            registry.add("spring.datasource.username") { mariaDBContainer.username }
            registry.add("spring.datasource.password") { mariaDBContainer.password }
            registry.add("spring.datasource.driver-class-name") { mariaDBContainer.driverClassName }
        }
    }
}