package be.sgl.backend.shared.config

import be.sgl.backend.shared.config.LocaleConfig.Companion.BE_NL
import be.sgl.backend.shared.exception.LocalizedException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.context.support.StaticMessageSource
import org.springframework.http.HttpStatus
import java.util.Locale

class LocalizedExceptionHandlerTest {

    private val messageSource = StaticMessageSource().apply {
        addMessage("error.simple", BE_NL, "Er ging iets mis")
        addMessage("error.simple", Locale.ENGLISH, "Something went wrong")
        addMessage("error.params", BE_NL, "Gebruiker {0} heeft {1} inschrijvingen")
    }

    private val testee = LocalizedExceptionHandler(messageSource)

    @Test
    fun `should respond bad request with the message in the requested locale`() {
        // given
        val exception = LocalizedException("error.simple")

        // when
        val response = testee.handleLocalized(exception, Locale.ENGLISH)

        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(response.body!!.message).isEqualTo("Something went wrong")
    }

    @Test
    fun `should fill the message placeholders with the exception parameters`() {
        // given
        val exception = LocalizedException("error.params", "Jimmy", 3)

        // when
        val response = testee.handleLocalized(exception, BE_NL)

        // then
        assertThat(response.body!!.message).isEqualTo("Gebruiker Jimmy heeft 3 inschrijvingen")
    }

    @Test
    fun `should keep the message key as exception message`() {
        // given

        // when
        val exception = LocalizedException("error.params", "Jimmy")

        // then
        assertThat(exception).hasMessage("error.params")
        assertThat(exception.params).containsExactly("Jimmy")
    }

    @Test
    fun `should use the http status code as error code`() {
        // given
        val exception = LocalizedException("error.simple")

        // when
        val response = testee.handleLocalized(exception, BE_NL)

        // then
        assertThat(response.body!!.code).isEqualTo("400")
    }
}
