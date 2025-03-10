package be.sgl.backend.util

import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import org.springframework.core.io.ClassPathResource
import java.io.ByteArrayOutputStream

fun fillForm(formName: String, formData: Map<String, Any?>, stamp: String? = null): ByteArray {
    val resultStream = ByteArrayOutputStream()
    Loader.loadPDF(ClassPathResource(formName).contentAsByteArray).use { document ->
        val acroForm = document.documentCatalog.acroForm
        for ((fieldName, value) in formData) {
            acroForm.getField(fieldName)?.setValue(value?.toString())
        }
        stamp?.let {
            val image = PDImageXObject.createFromByteArray(document, ClassPathResource(it).contentAsByteArray, it)
            val lastPage = document.getPage(document.numberOfPages - 1)
            val scale = 0.25f * lastPage.mediaBox.width / image.width
            val width = image.width * scale
            val height = image.height * scale
            PDPageContentStream(document, lastPage, PDPageContentStream.AppendMode.APPEND, true, true).use { contentStream ->
                contentStream.drawImage(image, 70f, 140f, width, height)
            }
        }
        acroForm.flatten()
        document.save(resultStream)
    }
    return resultStream.toByteArray()
}