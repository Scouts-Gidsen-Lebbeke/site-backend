package be.sgl.backend.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service
import org.thymeleaf.context.Context
import org.thymeleaf.spring6.SpringTemplateEngine

@Service
class MailService {

    @Autowired
    private lateinit var mailSender: JavaMailSender
    @Autowired
    private lateinit var templateEngine: SpringTemplateEngine

    fun sendSimpleEmail(to: String?, subject: String?, text: String?) {
        val message = SimpleMailMessage()
        message.setTo(to)
        message.subject = subject
        message.text = text
        message.from = ""
        mailSender.send(message)
    }

    fun sendTemplateMailWithSubjectTo(to: String, subject: String, templateName: String, placeholders: Map<String, Any>) {
        try {
            val mimeMessage = mailSender.createMimeMessage()
            val helper = MimeMessageHelper(mimeMessage, true)
            helper.setTo(to)
            helper.setSubject(subject)
            helper.setText(loadTemplate(templateName, placeholders), true)
            helper.setFrom("")
            mailSender.send(mimeMessage)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadTemplate(templateName: String, placeholders: Map<String, Any>): String {
        val locale = LocaleContextHolder.getLocale()
        return templateEngine.process(templateName, Context(locale, placeholders))
    }
}