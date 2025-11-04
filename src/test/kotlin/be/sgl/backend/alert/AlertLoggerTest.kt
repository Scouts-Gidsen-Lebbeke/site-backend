package be.sgl.backend.alert

import be.sgl.backend.service.MailService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.springframework.test.util.ReflectionTestUtils

class AlertLoggerTest {

    @Mock
    private lateinit var mailService: MailService

    @Mock
    private lateinit var mailBuilder: MailService.MailBuilder

    @InjectMocks
    private lateinit var alertLogger: AlertLogger

    @BeforeEach
    fun setup() {
        MockitoAnnotations.openMocks(this)
        ReflectionTestUtils.setField(alertLogger, "enabled", true)
        ReflectionTestUtils.setField(alertLogger, "mailRecipient", "admin@example.com")
        ReflectionTestUtils.setField(alertLogger, "environment", "test")
        ReflectionTestUtils.setField(alertLogger, "host", "https://test.example.com")
    }

    @Test
    fun `alert with lambda should send email when enabled`() {
        `when`(mailService.builder()).thenReturn(mailBuilder)
        `when`(mailBuilder.to(anyString())).thenReturn(mailBuilder)
        `when`(mailBuilder.subject(anyString())).thenReturn(mailBuilder)
        `when`(mailBuilder.body(anyString())).thenReturn(mailBuilder)

        alertLogger.alert(AlertCode.NEW_USER_EXISTS_NO_MEMBERSHIP) { "Test alert message" }

        verify(mailService).builder()
        verify(mailBuilder).to("admin@example.com")
        verify(mailBuilder).subject("https://test.example.com - test: NEW_USER_EXISTS_NO_MEMBERSHIP")
        verify(mailBuilder).body("Test alert message")
        verify(mailBuilder).send()
    }

    @Test
    fun `alert with string should send email when enabled`() {
        `when`(mailService.builder()).thenReturn(mailBuilder)
        `when`(mailBuilder.to(anyString())).thenReturn(mailBuilder)
        `when`(mailBuilder.subject(anyString())).thenReturn(mailBuilder)
        `when`(mailBuilder.body(anyString())).thenReturn(mailBuilder)

        alertLogger.alert(AlertCode.NEW_USER_EXISTS_PAID_MEMBERSHIP, "Test alert message")

        verify(mailService).builder()
        verify(mailBuilder).to("admin@example.com")
        verify(mailBuilder).subject("https://test.example.com - test: NEW_USER_EXISTS_PAID_MEMBERSHIP")
        verify(mailBuilder).body("Test alert message")
        verify(mailBuilder).send()
    }

    @Test
    fun `alert should not send email when disabled`() {
        ReflectionTestUtils.setField(alertLogger, "enabled", false)

        alertLogger.alert(AlertCode.NEW_USER_EXISTS_NO_MEMBERSHIP, "Test alert message")

        verify(mailService, never()).builder()
    }

    @Test
    fun `alert should use correct mail recipient`() {
        ReflectionTestUtils.setField(alertLogger, "mailRecipient", "custom@example.com")
        `when`(mailService.builder()).thenReturn(mailBuilder)
        `when`(mailBuilder.to(anyString())).thenReturn(mailBuilder)
        `when`(mailBuilder.subject(anyString())).thenReturn(mailBuilder)
        `when`(mailBuilder.body(anyString())).thenReturn(mailBuilder)

        alertLogger.alert(AlertCode.NEW_USER_EXISTS_NO_MEMBERSHIP, "Test alert")

        verify(mailBuilder).to("custom@example.com")
    }

    @Test
    fun `alert should format subject with host environment and code`() {
        ReflectionTestUtils.setField(alertLogger, "host", "https://prod.example.com")
        ReflectionTestUtils.setField(alertLogger, "environment", "production")
        `when`(mailService.builder()).thenReturn(mailBuilder)
        `when`(mailBuilder.to(anyString())).thenReturn(mailBuilder)
        `when`(mailBuilder.subject(anyString())).thenReturn(mailBuilder)
        `when`(mailBuilder.body(anyString())).thenReturn(mailBuilder)

        alertLogger.alert(AlertCode.NEW_USER_EXISTS_PAID_MEMBERSHIP, "Test")

        verify(mailBuilder).subject("https://prod.example.com - production: NEW_USER_EXISTS_PAID_MEMBERSHIP")
    }
}
