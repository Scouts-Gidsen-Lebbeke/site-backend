package be.sgl.backend.service

import be.sgl.backend.service.exception.ImageDeleteException
import be.sgl.backend.service.exception.ImageMoveException
import be.sgl.backend.service.exception.ImageUploadException
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.*
import kotlin.io.path.createDirectories

@Service
class ImageService {

    private val logger = KotlinLogging.logger {}

    fun upload(directory: ImageDirectory, image: MultipartFile): Path {
        try {
            check(image.contentType?.startsWith("image/") == true) { "Only image files are allowed." }
            check(image.size < MAX_FILE_SIZE) { "File size exceeds maximum ($MAX_FILE_SIZE MB)." }
            val extension = image.originalFilename?.substringAfterLast('.', "") ?: ""
            val fileName = UUID.randomUUID().toString() + "." + extension
            val filePath = Paths.get(IMAGE_BASE_PATH, directory.path, fileName)
            Files.createDirectories(filePath.parent)
            image.inputStream.use {
                Files.copy(it, filePath, StandardCopyOption.REPLACE_EXISTING)
            }
            return filePath
        } catch (e: IOException) {
            logger.error(e) { "Error while uploading image file ${image.name} to ${directory.path}:\n${e.stackTraceToString()}" }
            throw ImageUploadException(image.name, directory.path)
        }
    }

    fun delete(directory: ImageDirectory, fileName: String?) {
        fileName ?: return
        try {
            val filePath = Paths.get(IMAGE_BASE_PATH, directory.path, fileName)
            check(Files.exists(filePath)) { "Image $fileName does not exist." }
            Files.delete(filePath)
        } catch (e: IOException) {
            logger.error(e) { "Error while deleting image file $fileName to ${directory.path}:\n${e.stackTraceToString()}" }
            throw ImageDeleteException(fileName, directory.path)
        }
    }

    fun replace(directory: ImageDirectory, oldFileName: String?, image: MultipartFile): Path {
        delete(directory, oldFileName)
        return upload(directory, image)
    }

    fun move(fileName: String, sourceDir: ImageDirectory, targetDir: ImageDirectory): Path {
        try {
            val sourceFile = Paths.get(IMAGE_BASE_PATH, sourceDir.path, fileName).toFile()
            if (!sourceFile.exists() || !sourceFile.isFile) {
                throw ImageMoveException(fileName, sourceDir.path, targetDir.path)
            }
            val targetFile = Paths.get(IMAGE_BASE_PATH, targetDir.path, fileName).createDirectories()
            sourceFile.copyTo(targetFile.toFile(), overwrite = true)
            Files.delete(sourceFile.toPath())
            return targetFile
        } catch (e: IOException) {
            logger.error(e) { "Error while moving image file $fileName from ${sourceDir.path} to ${targetDir.path}:\n${e.stackTraceToString()}" }
            throw ImageMoveException(fileName, sourceDir.path, targetDir.path)
        }
    }

    fun get(fileName: String, sourceDir: ImageDirectory): File? {
        return Paths.get(IMAGE_BASE_PATH, sourceDir.path, fileName).toFile().takeIf { it.exists() && it.isFile }
    }

    companion object {
        private const val MAX_FILE_SIZE = (50 * 1024 * 1024).toLong()
        const val IMAGE_BASE_PATH = "images"
    }

    enum class ImageDirectory(val path: String) {
        TEMPORARY("tmp"),
        NEWS_ITEMS("news"),
        CALENDAR_ITEMS("calendar"),
        PROFILE_PICTURE("profile"),
        BACKGROUND("background"),
        ORGANIZATION("organization"),
        BRANCH("branch")
    }
}