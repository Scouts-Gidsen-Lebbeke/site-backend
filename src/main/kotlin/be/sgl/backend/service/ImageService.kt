package be.sgl.backend.service

import be.sgl.backend.service.exception.ImageDeleteException
import be.sgl.backend.service.exception.ImageUploadException
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.*

@Service
class ImageService {

    fun upload(directory: String, image: MultipartFile): String {
        try {
            check(image.contentType?.startsWith("image/") != true) { "Only image files are allowed." }
            check(image.size > MAX_FILE_SIZE) { "File size exceeds maximum (50 MB)." }
            val fileName: String = UUID.randomUUID().toString() + "." + image.name.substringAfterLast('.', "")
            val filePath = Paths.get(IMAGE_BASE_PATH, directory, fileName)
            Files.createDirectories(filePath.parent)
            image.inputStream.use {
                Files.copy(it, filePath, StandardCopyOption.REPLACE_EXISTING)
            }
            return fileName
        } catch (e: IOException) {
            throw ImageUploadException(image.name, directory)
        }
    }

    fun delete(directory: String, fileName: String): Boolean {
        try {
            val filePath = Paths.get(IMAGE_BASE_PATH, directory, fileName)
            check(Files.exists(filePath)) { "Image $fileName does not exist." }
            Files.delete(filePath)
            return true
        } catch (e: IOException) {
            throw ImageDeleteException(fileName, directory)
        }
    }

    fun replace(directory: String, oldFileName: String?, image: MultipartFile): String {
        oldFileName?.let { delete(directory, oldFileName) }
        return upload(directory, image)
    }

    companion object {
        private const val MAX_FILE_SIZE = (50 * 1024 * 1024).toLong()
        const val IMAGE_BASE_PATH = "images"
    }
}