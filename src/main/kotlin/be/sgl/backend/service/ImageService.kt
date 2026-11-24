package be.sgl.backend.service

import be.sgl.backend.exception.ImageDeleteException
import be.sgl.backend.exception.ImageMoveException
import be.sgl.backend.exception.ImageUploadException
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.InvalidPathException
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
            val extension = image.originalFilename?.substringAfterLast('.', "")?.lowercase()
            check(extension in ALLOWED_EXTENSIONS) { "Only ${ALLOWED_EXTENSIONS.joinToString()} files are allowed." }
            val fileName = UUID.randomUUID().toString() + "." + extension
            val filePath = resolve(directory, fileName)
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
            val filePath = resolve(directory, fileName)
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
        val sourceFile = resolve(sourceDir, fileName).toFile()
        val targetFile = resolve(targetDir, fileName)
        try {
            if (!sourceFile.exists() || !sourceFile.isFile) {
                throw ImageMoveException(fileName, sourceDir.path, targetDir.path)
            }
            targetFile.parent.createDirectories()
            sourceFile.copyTo(targetFile.toFile(), overwrite = true)
            Files.delete(sourceFile.toPath())
            return targetFile
        } catch (e: IOException) {
            logger.error(e) { "Error while moving image file $fileName from ${sourceDir.path} to ${targetDir.path}:\n${e.stackTraceToString()}" }
            throw ImageMoveException(fileName, sourceDir.path, targetDir.path)
        }
    }

    fun get(fileName: String, sourceDir: ImageDirectory): File? {
        val path = runCatching { resolve(sourceDir, fileName) }.getOrNull() ?: return null
        return path.toFile().takeIf { it.exists() && it.isFile }
    }

    /**
     * Resolves [fileName] inside [directory] and makes sure it doesn't escape it (e.g. via `../` or an absolute path).
     * Images are stored flat, so the file must sit directly in the directory.
     */
    private fun resolve(directory: ImageDirectory, fileName: String): Path {
        val base = Paths.get(IMAGE_BASE_PATH, directory.path).toAbsolutePath().normalize()
        val path = try {
            base.resolve(fileName).normalize()
        } catch (e: InvalidPathException) {
            null
        }
        check(path != null && path.parent == base) { "Invalid image file name $fileName." }
        return path
    }

    companion object {
        private const val MAX_FILE_SIZE = (50 * 1024 * 1024).toLong()
        const val IMAGE_BASE_PATH = "images"
        private val ALLOWED_EXTENSIONS = setOf("png", "jpg", "jpeg", "gif", "webp")
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