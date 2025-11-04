package be.sgl.backend.service.exception

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

class ImageExceptionTest {

    @Test
    fun `ImageUploadException should have correct message`() {
        val exception = ImageUploadException("test.jpg", "uploads")
        assertEquals("Image upload of test.jpg to uploads failed.", exception.message)
    }

    @Test
    fun `ImageUploadException should be annotated with ResponseStatus INTERNAL_SERVER_ERROR`() {
        val annotation = ImageUploadException::class.java.superclass.getAnnotation(ResponseStatus::class.java)
        assertNotNull(annotation)
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, annotation?.value)
    }

    @Test
    fun `ImageDeleteException should have correct message`() {
        val exception = ImageDeleteException("test.jpg", "uploads")
        assertEquals("Image delete of test.jpg from uploads failed.", exception.message)
    }

    @Test
    fun `ImageMoveException should have correct message`() {
        val exception = ImageMoveException("test.jpg", "source", "target")
        assertEquals("Image move of test.jpg from source to target failed.", exception.message)
    }

    @Test
    fun `ImageUploadException should extend ImageException`() {
        val exception = ImageUploadException("test.jpg", "uploads")
        assertTrue(exception is ImageException)
    }

    @Test
    fun `ImageMoveException should extend ImageException`() {
        val exception = ImageMoveException("test.jpg", "source", "target")
        assertTrue(exception is ImageException)
    }

    @Test
    fun `ImageDeleteException should extend Throwable`() {
        val exception = ImageDeleteException("test.jpg", "uploads")
        assertTrue(exception is Throwable)
    }
}
