package be.sgl.backend.alert

import be.sgl.backend.service.MailService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Answers
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class AlertLoggerTest {

    @Mock
    lateinit var mailService: MailService
    @Mock(answer = Answers.RETURNS_SELF)
    lateinit var mailBuilder: MailService.MailBuilder

    private val recipient = "alerts@scouts.be"
    private val environment = "PROD"

    private fun properties(enabled: Boolean) = AlertProperties().also {
        it.enabled = enabled
        it.recipient = recipient
        it.environment = environment
    }

    @Test
    fun `should mail the alert to the recipient when alerts are enabled`() {
        // given
        val testee = AlertLogger(properties(enabled = true), mailService)
        given(mailService.builder()).willReturn(mailBuilder)

        // when
        testee.alert(AlertCode.NEW_USER_EXISTS_NO_MEMBERSHIP, "User x has no membership")

        // then
        then(mailBuilder).should().to(recipient)
        then(mailBuilder).should().subject("$environment: ${AlertCode.NEW_USER_EXISTS_NO_MEMBERSHIP}")
        then(mailBuilder).should().body("User x has no membership")
        then(mailBuilder).should().send()
    }

    @Test
    fun `should evaluate the message supplier when alerting lazily`() {
        // given
        val testee = AlertLogger(properties(enabled = true), mailService)
        given(mailService.builder()).willReturn(mailBuilder)

        // when
        testee.alert(AlertCode.ACTIVE_MEMBERSHIP_NO_EXTERNAL_LINK) { "lazy message" }

        // then
        then(mailBuilder).should().body("lazy message")
        then(mailBuilder).should().send()
    }

    @Test
    fun `should not send any mail when alerts are disabled`() {
        // given
        val testee = AlertLogger(properties(enabled = false), mailService)

        // when
        testee.alert(AlertCode.NEW_USER_EXISTS_PAID_MEMBERSHIP, "message")

        // then
        then(mailService).shouldHaveNoInteractions()
    }
}
