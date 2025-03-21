package be.sgl.backend.service

import be.sgl.backend.entity.organization.OrganizationType
import be.sgl.backend.repository.OrganizationRepository
import be.sgl.backend.service.exception.IncompleteConfigurationException
import jakarta.mail.util.ByteArrayDataSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service
import org.thymeleaf.context.Context
import org.thymeleaf.spring6.SpringTemplateEngine
import java.io.File
import java.io.InputStream

@Service
class MailService {

    @Autowired
    private lateinit var mailSender: JavaMailSender
    @Autowired
    private lateinit var templateEngine: SpringTemplateEngine
    @Autowired
    private lateinit var organizationRepository: OrganizationRepository

    fun builder(): MailBuilder {
        return MailBuilder()
    }

    private fun loadTemplate(templateName: String, placeholders: Map<String, Any?>): String {
        val locale = LocaleContextHolder.getLocale()
        return templateEngine.process(templateName, Context(locale, placeholders))
    }

    inner class MailBuilder {

        private var from: String? = null
        private val to = mutableListOf<String>()
        private lateinit var subject: String
        private lateinit var body: String
        private val cc = mutableListOf<String>()
        private val attachments = mutableListOf<Attachment>()

        fun from(from: String) = apply { this.from = from }

        private fun fromDefault(): String {
            val organization = organizationRepository.getByType(OrganizationType.OWNER)
                ?: throw IncompleteConfigurationException("No organization configured!")
            return organization.getEmail()
                ?: throw IncompleteConfigurationException("No organization email configured, not able to send forms!")
        }

        fun to(vararg to: String) = apply { this.to.addAll(to) }

        fun cc(vararg cc: String) = apply { this.cc.addAll(cc) }

        fun subject(subject: String) = apply { this.subject = subject }

        fun body(body: String) = apply { this.body = body }

        fun template(templateName: String, placeholders: Map<String, Any?> = emptyMap()) = apply {
            this.body = loadTemplate(templateName, placeholders)
        }

        fun addAttachment(content: File, name: String = content.name) = apply {
            attachments.add(FileAttachment(name, content))
        }

        fun addAttachment(content: ByteArray, name: String, mimeType: String) = apply {
            attachments.add(ByteArrayAttachment(name, content, mimeType))
        }

        fun addAttachment(content: InputStream, name: String) = apply {
            attachments.add(InputStreamAttachment(name, content))
        }

        fun send() {
            try {
                val mimeMessage = mailSender.createMimeMessage()
                val helper = MimeMessageHelper(mimeMessage, true)
                val fromAddress = from ?: fromDefault()
                helper.setFrom(fromAddress)
                helper.setReplyTo(fromAddress)
                to.forEach(helper::addTo)
                helper.setSubject(subject)
                helper.setText(body, true)
                cc.forEach(helper::addCc)
                attachments.forEach { it.addAttachment(helper) }
                mailSender.send(mimeMessage)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    sealed class Attachment(val name: String) {
        abstract fun addAttachment(helper: MimeMessageHelper)
    }

    class FileAttachment(name: String, private val file: File) : Attachment(name) {
        override fun addAttachment(helper: MimeMessageHelper) {
            helper.addAttachment(name, file)
        }
    }

    class ByteArrayAttachment(name: String, private val content: ByteArray, private val mimeType: String) : Attachment(name) {
        override fun addAttachment(helper: MimeMessageHelper) {
            helper.addAttachment(name, ByteArrayDataSource(content, mimeType))
        }
    }

    class InputStreamAttachment(name: String, private val file: InputStream) : Attachment(name) {
        override fun addAttachment(helper: MimeMessageHelper) {
            helper.addAttachment(name, ByteArrayDataSource(file, "application/octet-stream"))
        }
    }
}