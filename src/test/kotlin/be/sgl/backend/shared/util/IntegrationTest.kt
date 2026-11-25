package be.sgl.backend.shared.util

import be.sgl.backend.shared.config.DatabaseContainerConfiguration
import be.sgl.backend.shared.config.MailContainerConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(DatabaseContainerConfiguration::class, MailContainerConfiguration::class)
annotation class IntegrationTest
