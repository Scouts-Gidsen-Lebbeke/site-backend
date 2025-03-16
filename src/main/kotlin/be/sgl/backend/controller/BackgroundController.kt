package be.sgl.backend.controller

import be.sgl.backend.config.security.OnlyAdmin
import be.sgl.backend.dto.RemoteFile
import be.sgl.backend.service.ImageService
import be.sgl.backend.service.ImageService.Companion.IMAGE_BASE_PATH
import be.sgl.backend.service.ImageService.ImageDirectory.BACKGROUND
import io.github.wimdeblauwe.errorhandlingspringbootstarter.ApiErrorResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.io.File
import kotlin.io.path.Path

@RestController
@RequestMapping("/backgrounds")
@Tag(name = "Backgrounds", description = "Endpoints for managing backgrounds.")
class BackgroundController {

    @Autowired
    private lateinit var imageService: ImageService

    @GetMapping
    @Operation(
        summary = "Get all backgrounds",
        description = "Returns a list of all backgrounds available as static resource.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(type = "array", implementation = RemoteFile::class))])
        ]
    )
    fun getBackgrounds(): ResponseEntity<List<RemoteFile>> {
        val directory = Path(IMAGE_BASE_PATH, BACKGROUND.path)
        val files = directory.toFile().listFiles()?.filter(File::isFile)?.map { RemoteFile(it, directory) } ?: emptyList()
        return ResponseEntity.ok(files)
    }

    @PostMapping
    @OnlyAdmin
    @Operation(
        summary = "Upload a background image",
        description = "Uploads the image to the backgrounds folder and returns its file location.",
        responses = [
            ApiResponse(responseCode = "200", description = "Background uploaded", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = RemoteFile::class))]),
            ApiResponse(responseCode = "500", description = "Image error", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun addBackground(@RequestParam("file") file: MultipartFile): ResponseEntity<RemoteFile> {
        return ResponseEntity.ok(RemoteFile(imageService.upload(BACKGROUND, file)))
    }

    @DeleteMapping("/{filename}")
    @OnlyAdmin
    @Operation(
        summary = "Delete an existing background image",
        description = "Deletes a background image, identified with its file name.",
        responses = [
            ApiResponse(responseCode = "200", description = "Background deleted"),
            ApiResponse(responseCode = "400", description = "Background doesn't exist", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "500", description = "Image error", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun deleteBackground(@PathVariable filename: String): ResponseEntity<Unit> {
        imageService.delete(BACKGROUND, filename)
        return ResponseEntity.ok().build()
    }
}