package be.sgl.backend.service

import be.sgl.backend.exception.ImageMoveException
import be.sgl.backend.exception.ImageUploadException
import be.sgl.backend.service.ImageService.Companion.IMAGE_BASE_PATH
import be.sgl.backend.service.ImageService.ImageDirectory
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.springframework.mock.web.MockMultipartFile
import java.nio.file.Path
import java.nio.file.Paths
import java.util.UUID

/**
 * [ImageService] writes to the hard-coded relative folder [IMAGE_BASE_PATH], so tests only cover
 * the paths that fail before anything is written there. Happy paths need a configurable base path first.
 */
class ImageServiceTest {

    private val testee = ImageService()

    @TempDir
    lateinit var tempDir: Path

    private fun unknownFileName() = "${UUID.randomUUID()}.png"

    /** A file name that escapes [IMAGE_BASE_PATH]/[directory] and points to [target]. */
    private fun traversalTo(target: Path, directory: ImageDirectory) =
        Paths.get(IMAGE_BASE_PATH, directory.path).toAbsolutePath().relativize(target.toAbsolutePath()).toString()

    @Test
    fun `should reject upload when the file is not an image`() {
        // given
        val file = MockMultipartFile("file", "doc.pdf", "application/pdf", byteArrayOf(1))

        // when / then
        assertThatThrownBy { testee.upload(ImageDirectory.TEMPORARY, file) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessage("Only image files are allowed.")
    }

    @Test
    fun `should reject upload when the content type is missing`() {
        // given
        val file = MockMultipartFile("file", "photo.png", null, byteArrayOf(1))

        // when / then
        assertThatThrownBy { testee.upload(ImageDirectory.TEMPORARY, file) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessage("Only image files are allowed.")
    }

    @Test
    fun `should reject upload when the extension is not a raster image`() {
        // given: content type is chosen by the client, so it can't be trusted on its own
        val file = MockMultipartFile("file", "evil.svg", "image/svg+xml", byteArrayOf(1))

        // when / then
        assertThatThrownBy { testee.upload(ImageDirectory.TEMPORARY, file) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("files are allowed")
    }

    @Test
    fun `should reject upload when the file has no extension`() {
        // given
        val file = MockMultipartFile("file", "photo", "image/png", byteArrayOf(1))

        // when / then
        assertThatThrownBy { testee.upload(ImageDirectory.TEMPORARY, file) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("files are allowed")
    }

    @Test
    fun `should reject upload when the file is 50 MB or larger`() {
        // given
        val file = MockMultipartFile("file", "photo.png", "image/png", ByteArray(50 * 1024 * 1024))

        // when / then
        assertThatThrownBy { testee.upload(ImageDirectory.TEMPORARY, file) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("File size exceeds maximum")
    }

    @Test
    fun `should do nothing when deleting without file name`() {
        // given

        // when / then
        assertThatCode { testee.delete(ImageDirectory.NEWS_ITEMS, null) }.doesNotThrowAnyException()
    }

    @Test
    fun `should fail when deleting an image that does not exist`() {
        // given
        val fileName = unknownFileName()

        // when / then
        assertThatThrownBy { testee.delete(ImageDirectory.NEWS_ITEMS, fileName) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining(fileName)
    }

    @Test
    fun `should fail replacing when the old image does not exist`() {
        // given
        val fileName = unknownFileName()
        val file = MockMultipartFile("file", "photo.png", "image/png", byteArrayOf(1))

        // when / then
        assertThatThrownBy { testee.replace(ImageDirectory.NEWS_ITEMS, fileName, file) }
            .isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `should fail moving when the source image does not exist`() {
        // given
        val fileName = unknownFileName()

        // when / then
        assertThatThrownBy { testee.move(fileName, ImageDirectory.TEMPORARY, ImageDirectory.NEWS_ITEMS) }
            .isInstanceOf(ImageMoveException::class.java)
            .hasMessageContaining(fileName)
    }

    @Test
    fun `should return null when getting an image that does not exist`() {
        // given
        val fileName = unknownFileName()

        // when
        val file = testee.get(fileName, ImageDirectory.ORGANIZATION)

        // then
        assertThat(file).isNull()
    }

    @Test
    fun `should describe file and directory in the upload exception`() {
        // given

        // when
        val exception = ImageUploadException("photo.png", ImageDirectory.TEMPORARY.path)

        // then
        assertThat(exception).hasMessage("Image upload of photo.png to tmp failed.")
    }

    @Test
    fun `should not return files outside the image directory when the file name contains path traversal`() {
        // given
        val secret = tempDir.resolve("secret.txt").toFile().apply { writeText("secret") }

        // when
        val file = testee.get(traversalTo(secret.toPath(), ImageDirectory.ORGANIZATION), ImageDirectory.ORGANIZATION)

        // then
        assertThat(file).isNull()
    }

    @Test
    fun `should not delete files outside the image directory when the file name contains path traversal`() {
        // given
        val secret = tempDir.resolve("secret.txt").toFile().apply { writeText("secret") }

        // when
        runCatching { testee.delete(ImageDirectory.NEWS_ITEMS, traversalTo(secret.toPath(), ImageDirectory.NEWS_ITEMS)) }

        // then
        assertThat(secret).exists()
    }

    @Test
    fun `should not move files from outside the image directory when the file name contains path traversal`() {
        // given
        val secret = tempDir.resolve("secret.txt").toFile().apply { writeText("secret") }

        // when / then
        assertThatThrownBy { testee.move(traversalTo(secret.toPath(), ImageDirectory.TEMPORARY), ImageDirectory.TEMPORARY, ImageDirectory.NEWS_ITEMS) }
            .isInstanceOf(IllegalStateException::class.java)
        assertThat(secret).exists()
    }
}
