package be.sgl.backend.shared.util

import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import org.apache.pdfbox.text.PDFTextStripper
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Path
import javax.imageio.ImageIO

class PdfUtilsTest {

    @TempDir
    lateinit var tempDir: Path

    private val form = "forms/participation.pdf"

    private fun stamp(): File {
        val file = tempDir.resolve("stamp.png").toFile()
        ImageIO.write(BufferedImage(20, 10, BufferedImage.TYPE_INT_RGB), "png", file)
        return file
    }

    private fun text(pdf: ByteArray) = Loader.loadPDF(pdf).use { PDFTextStripper().getText(it) }

    @Test
    fun `should fill and flatten the form fields when data is given`() {
        // given
        val data = mapOf("name" to "Doe", "first_name" to "Jimmy", "amount" to 12)

        // when
        val pdf = fillForm(form, data)

        // then
        assertThat(text(pdf)).contains("Doe", "Jimmy", "12")
        Loader.loadPDF(pdf).use { assertThat(it.documentCatalog.acroForm.fields).isEmpty() }
    }

    @Test
    fun `should ignore unknown fields and null values when filling the form`() {
        // given
        val data = mapOf("unknown_field" to "value", "name" to null, "first_name" to "Jimmy")

        // when
        val pdf = fillForm(form, data)

        // then
        assertThat(text(pdf)).contains("Jimmy").doesNotContain("value")
    }

    @Test
    fun `should draw the stamp on the requested page when stamp specs are given`() {
        // given
        val specs = StampSpecs(stamp(), page = 1)

        // when
        val pdf = fillForm(form, emptyMap(), specs)

        // then
        Loader.loadPDF(pdf).use { document ->
            val resources = document.getPage(0).resources
            val images = resources.xObjectNames.map { resources.getXObject(it) }.filterIsInstance<PDImageXObject>()
            assertThat(images).anySatisfy { assertThat(it.width).isEqualTo(20) }
        }
    }

    @Test
    fun `should fail when the stamp page does not exist`() {
        // given
        val specs = StampSpecs(stamp(), page = 5)

        // when / then
        assertThatThrownBy { fillForm(form, emptyMap(), specs) }.isInstanceOf(IndexOutOfBoundsException::class.java)
    }

    @Test
    fun `should fail when the form does not exist`() {
        // given

        // when / then
        assertThatThrownBy { fillForm("forms/unknown.pdf", emptyMap()) }.isInstanceOf(java.io.IOException::class.java)
    }
}
