package be.sgl.backend.alert

import be.sgl.backend.service.MailService
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class AlertLogger {

    private val logger = KotlinLogging.logger {}

    @Autowired
    private lateinit var mailService: MailService
    @Value("\${mail.alert.recipient}")
    private lateinit var mailRecipient: String
    @Value("\${app.environment}")
    private lateinit var environment: String
    @Value("\${app.base.url}")
    private lateinit var host: String

    fun alert(code: AlertCode, message: () -> String) {
        alert(code, message.invoke())
    }

    fun alert(code: AlertCode, message: String) {
        logger.error { "Raising $code alert: $message" }
        mailService.builder()
            .to(mailRecipient)
            .subject("$host - $environment: $code")
            .body(message)
            .send()
        logger.debug { "Alert raised" }
    }
}