package be.sgl.backend.service

import be.sgl.backend.entity.organization.OrganizationTestMother.organization
import be.sgl.backend.entity.organization.OrganizationType
import be.sgl.backend.exception.IncompleteConfigurationException
import be.sgl.backend.repository.organization.OrganizationRepository
import be.sgl.backend.util.anyValue
import be.sgl.backend.util.argumentCaptor
import be.sgl.backend.util.captureValue
import be.sgl.backend.util.eqValue
import jakarta.mail.Message
import jakarta.mail.Multipart
import jakarta.mail.Part
import jakarta.mail.Session
import jakarta.mail.internet.MimeMessage
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.mail.javamail.JavaMailSender
import org.thymeleaf.context.Context
import org.thymeleaf.spring6.SpringTemplateEngine
import java.nio.file.Path
import java.util.Locale
import java.util.Properties

@ExtendWith(MockitoExtension::class)
class MailServiceTest {

    @Mock
    lateinit var mailSender: JavaMailSender
    @Mock
    lateinit var templateEngine: SpringTemplateEngine
    @Mock
    lateinit var organizationRepository: OrganizationRepository

    lateinit var testee: MailService

    private val baseUrl = "https://scouts.be"
    private val organizationEmail = "info@scouts.be"

    @BeforeEach
    fun setUp() {
        testee = MailService(mailSender, templateEngine, organizationRepository, baseUrl)
    }

    @AfterEach
    fun tearDown() {
        LocaleContextHolder.resetLocaleContext()
    }

    private fun givenMimeMessage(): MimeMessage {
        val message = MimeMessage(Session.getInstance(Properties()))
        given(mailSender.createMimeMessage()).willReturn(message)
        return message
    }

    private fun attachmentNames(part: Part): List<String> = when (val content = part.content) {
        is Multipart -> (0 until content.count).flatMap { attachmentNames(content.getBodyPart(it)) }
        else -> listOfNotNull(part.fileName)
    }

    private fun htmlBody(part: Part): String? = when (val content = part.content) {
        is Multipart -> (0 until content.count).firstNotNullOfOrNull { htmlBody(content.getBodyPart(it)) }
        else -> (content as? String)?.takeIf { part.dataHandler.contentType.startsWith("text/html") }
    }

    @Nested
    inner class Send {

        @Test
        fun `should send mail with all recipients, subject and html body when sender is given`() {
            // given
            val message = givenMimeMessage()

            // when
            testee.builder()
                .from("leiding@scouts.be")
                .to("a@mail.be", "b@mail.be")
                .cc("c@mail.be")
                .subject("Onderwerp")
                .body("<p>Hallo</p>")
                .send()

            // then
            then(mailSender).should().send(message)
            assertThat(message.from.map { it.toString() }).containsExactly("leiding@scouts.be")
            assertThat(message.replyTo.map { it.toString() }).containsExactly("leiding@scouts.be")
            assertThat(message.getRecipients(Message.RecipientType.TO).map { it.toString() }).containsExactly("a@mail.be", "b@mail.be")
            assertThat(message.getRecipients(Message.RecipientType.CC).map { it.toString() }).containsExactly("c@mail.be")
            assertThat(message.subject).isEqualTo("Onderwerp")
            assertThat(htmlBody(message)).isEqualTo("<p>Hallo</p>")
            then(organizationRepository).shouldHaveNoInteractions()
        }

        @Test
        fun `should send from the organization email when no sender is given`() {
            // given
            val message = givenMimeMessage()
            given(organizationRepository.getByType(OrganizationType.OWNER)).willReturn(organization().email(organizationEmail).build())

            // when
            testee.builder().to("a@mail.be").subject("Onderwerp").body("body").send()

            // then
            then(mailSender).should().send(message)
            assertThat(message.from.map { it.toString() }).containsExactly(organizationEmail)
            assertThat(message.replyTo.map { it.toString() }).containsExactly(organizationEmail)
        }

        @Test
        fun `should swallow the failure and not send when no organization is configured and no sender is given`() {
            // given
            givenMimeMessage()
            given(organizationRepository.getByType(OrganizationType.OWNER)).willReturn(null)

            // when
            assertThatCode { testee.builder().to("a@mail.be").subject("Onderwerp").body("body").send() }.doesNotThrowAnyException()

            // then
            then(mailSender).should(org.mockito.Mockito.never()).send(anyValue<MimeMessage>())
        }

        @Test
        fun `should swallow the failure and not send when the subject is missing`() {
            // given
            givenMimeMessage()

            // when
            testee.builder().from("leiding@scouts.be").to("a@mail.be").body("body").send()

            // then
            then(mailSender).should(org.mockito.Mockito.never()).send(anyValue<MimeMessage>())
        }

        @Test
        fun `should add every kind of attachment when attachments are given`(@TempDir tempDir: Path) {
            // given
            val message = givenMimeMessage()
            val file = tempDir.resolve("photo.txt").toFile().apply { writeText("content") }

            // when
            testee.builder()
                .from("leiding@scouts.be")
                .to("a@mail.be")
                .subject("Onderwerp")
                .body("body")
                .addAttachment(file)
                .addAttachment(byteArrayOf(1, 2), "form.pdf", "application/pdf")
                .addAttachment("stream".byteInputStream(), "stream.bin")
                .send()

            // then
            then(mailSender).should().send(message)
            assertThat(attachmentNames(message)).containsExactly("photo.txt", "form.pdf", "stream.bin")
        }
    }

    @Nested
    inner class ToOrganization {

        @Test
        fun `should add the organization email as recipient when it is configured`() {
            // given
            val message = givenMimeMessage()
            given(organizationRepository.getByType(OrganizationType.OWNER)).willReturn(organization().email(organizationEmail).build())

            // when
            testee.builder().from("leiding@scouts.be").toOrganization().subject("Onderwerp").body("body").send()

            // then
            assertThat(message.getRecipients(Message.RecipientType.TO).map { it.toString() }).containsExactly(organizationEmail)
        }

        @Test
        fun `should fail when no organization is configured`() {
            // given
            given(organizationRepository.getByType(OrganizationType.OWNER)).willReturn(null)

            // when / then
            assertThatThrownBy { testee.builder().toOrganization() }
                .isInstanceOf(IncompleteConfigurationException::class.java)
                .hasMessage("No organization configured!")
        }

        @Test
        fun `should fail when the organization has no email`() {
            // given
            given(organizationRepository.getByType(OrganizationType.OWNER)).willReturn(organization().build())

            // when / then
            assertThatThrownBy { testee.builder().toOrganization() }
                .isInstanceOf(IncompleteConfigurationException::class.java)
                .hasMessage("No organization email configured!")
        }
    }

    @Nested
    inner class Template {

        @Test
        fun `should render the template with base url, extra placeholders and current locale`() {
            // given
            val message = givenMimeMessage()
            val locale = Locale.FRENCH
            LocaleContextHolder.setLocale(locale)
            val captor = argumentCaptor<Context>()
            given(templateEngine.process(eqValue("template.html"), captureValue(captor))).willReturn("<p>rendered</p>")

            // when
            testee.builder()
                .from("leiding@scouts.be")
                .to("a@mail.be")
                .subject("Onderwerp")
                .template("template.html", mapOf("member" to "Jimmy"))
                .send()

            // then
            assertThat(htmlBody(message)).isEqualTo("<p>rendered</p>")
            assertThat(captor.value.locale).isEqualTo(locale)
            assertThat(captor.value.getVariable("baseUrl")).isEqualTo(baseUrl)
            assertThat(captor.value.getVariable("member")).isEqualTo("Jimmy")
        }

        @Test
        fun `should let extra placeholders override the base url`() {
            // given
            val captor = argumentCaptor<Context>()
            given(templateEngine.process(eqValue("template.html"), captureValue(captor))).willReturn("rendered")

            // when
            testee.builder().template("template.html", mapOf("baseUrl" to "other"))

            // then
            assertThat(captor.value.getVariable("baseUrl")).isEqualTo("other")
        }
    }
}
