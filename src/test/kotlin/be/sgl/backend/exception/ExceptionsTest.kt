package be.sgl.backend.exception

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

class ExceptionsTest {

    private fun statusOf(exception: Throwable) =
        AnnotatedElementUtils.findMergedAnnotation(exception.javaClass, ResponseStatus::class.java)?.code

    @Test
    fun `should map not found exceptions to 404 with a descriptive message`() {
        // given
        val exception = UserNotFoundException("jimmy")

        // when
        val status = statusOf(exception)

        // then
        assertThat(status).isEqualTo(HttpStatus.NOT_FOUND)
        assertThat(exception).hasMessage("User jimmy not found")
    }

    @Test
    fun `should map localized exceptions to 400`() {
        // given
        val exception = LocalizedException("key")

        // when
        val status = statusOf(exception)

        // then
        assertThat(status).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `should map incomplete configuration to 409`() {
        // given
        val exception = IncompleteConfigurationException("No organization configured!")

        // when
        val status = statusOf(exception)

        // then
        assertThat(status).isEqualTo(HttpStatus.CONFLICT)
    }

    @Test
    fun `should map image upload and move failures to 500 with a descriptive message`() {
        // given
        val upload = ImageUploadException("a.png", "tmp")
        val move = ImageMoveException("a.png", "tmp", "news")

        // when
        val statuses = listOf(statusOf(upload), statusOf(move))

        // then
        assertThat(statuses).containsOnly(HttpStatus.INTERNAL_SERVER_ERROR)
        assertThat(move).hasMessage("Image move of a.png from tmp to news failed.")
    }

    @Test
    fun `should map image delete failures to 500 like the other image exceptions`() {
        // given
        val delete = ImageDeleteException("a.png", "tmp")

        // when
        val status = statusOf(delete)

        // then
        assertThat(delete).isInstanceOf(ImageException::class.java)
        assertThat(status).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
    }
}
