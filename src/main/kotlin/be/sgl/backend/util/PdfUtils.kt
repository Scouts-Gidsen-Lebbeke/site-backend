package be.sgl.backend.util

import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import org.springframework.core.io.ClassPathResource
import java.io.ByteArrayOutputStream
import java.io.File

fun fillForm(formName: String, formData: Map<String, Any?>, stampSpecs: StampSpecs? = null): ByteArray {
    val resultStream = ByteArrayOutputStream()
    Loader.loadPDF(ClassPathResource(formName).contentAsByteArray).use { document ->
        val acroForm = document.documentCatalog.acroForm
        for ((fieldName, value) in formData) {
            acroForm.getField(fieldName)?.setValue(value?.toString())
        }
        stampSpecs?.let {
            val image = PDImageXObject.createFromByteArray(document, it.stamp.readBytes(), it.stamp.name)
            val lastPage = document.getPage(stampSpecs.page - 1)
            val scale = 0.25f * lastPage.mediaBox.width / image.width
            val width = image.width * scale
            val height = image.height * scale
            PDPageContentStream(document, lastPage, PDPageContentStream.AppendMode.APPEND, true, true).use { contentStream ->
                contentStream.drawImage(image, stampSpecs.x, stampSpecs.y, width, height)
            }
        }
        acroForm.flatten()
        document.save(resultStream)
    }
    return resultStream.toByteArray()
}

data class StampSpecs(val stamp: File, val page: Int = 1, val x: Float = 70f, val y: Float = 140f)