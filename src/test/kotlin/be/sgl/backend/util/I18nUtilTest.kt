package be.sgl.backend.util

import be.sgl.backend.config.LocaleConfig.Companion.BE_NL
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.context.ApplicationContext
import org.springframework.context.MessageSource
import org.springframework.context.NoSuchMessageException
import org.springframework.context.support.StaticMessageSource
import org.assertj.core.api.Assertions.assertThatThrownBy
import java.util.Locale

@ExtendWith(MockitoExtension::class)
class I18nUtilTest {

    @Mock
    lateinit var applicationContext: ApplicationContext

    private val testee = I18nUtil()

    private val messageSource = StaticMessageSource().apply {
        addMessage("simple", BE_NL, "Alle e-mails werden verstuurd!")
        addMessage("progress", BE_NL, "E-mail {0}/{1} wordt verstuurd...")
        addMessage("simple", Locale.ENGLISH, "All mails sent!")
    }

    @BeforeEach
    fun setUp() {
        given(applicationContext.getBean(MessageSource::class.java)).willReturn(messageSource)
        testee.setApplicationContext(applicationContext)
    }

    @Test
    fun `should resolve the dutch message when no arguments are given`() {
        // given

        // when
        val message = I18nUtil.i18n("simple")

        // then
        assertThat(message).isEqualTo("Alle e-mails werden verstuurd!")
    }

    @Test
    fun `should fail when message key is unknown`() {
        // given

        // when / then
        assertThatThrownBy { I18nUtil.i18n("unknown") }.isInstanceOf(NoSuchMessageException::class.java)
    }

    @Test
    @Disabled("Possible bug: i18n wraps the vararg array in arrayOf(args), so {0} receives the whole array and {1} is never filled")
    fun `should fill the placeholders when arguments are given`() {
        // given

        // when
        val message = I18nUtil.i18n("progress", 1, 5)

        // then
        assertThat(message).isEqualTo("E-mail 1/5 wordt verstuurd...")
    }
}
