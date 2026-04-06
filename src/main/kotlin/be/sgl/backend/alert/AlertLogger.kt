package be.sgl.backend.alert

import be.sgl.backend.service.MailService
import mu.KotlinLogging
import org.springframework.stereotype.Service

@Service
class AlertLogger(
    private val properties: AlertProperties,
    private val mailService: MailService
) {

    private val logger = KotlinLogging.logger {}

    fun alert(code: AlertCode, message: () -> String) {
        alert(code, message.invoke())
    }

    fun alert(code: AlertCode, message: String) {
        if (!properties.enabled) {
            logger.error { "Skipped raising $code alert: $message" }
            return
        }
        logger.error { "Raising $code alert: $message" }
        mailService.builder()
            .to(properties.recipient!!)
            .subject("${properties.environment}: $code")
            .body(message)
            .send()
        logger.debug { "Alert raised" }
    }
}