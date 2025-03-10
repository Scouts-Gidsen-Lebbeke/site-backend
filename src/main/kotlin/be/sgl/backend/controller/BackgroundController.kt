package be.sgl.backend.controller

import be.sgl.backend.config.BadRequestResponse
import be.sgl.backend.config.security.OnlyAdmin
import be.sgl.backend.service.ImageService
import be.sgl.backend.service.ImageService.Companion.IMAGE_BASE_PATH
import be.sgl.backend.service.ImageService.ImageDirectory.BACKGROUND
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.beans.factory.annotation.Autowired
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
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(mediaType = "application/json", schema = Schema(type = "array", implementation = String::class))])
        ]
    )
    fun getBackgrounds(): ResponseEntity<List<String>> {
        val dir = Path(IMAGE_BASE_PATH, BACKGROUND.path).toFile()
        return ResponseEntity.ok(dir.listFiles()?.filter(File::isFile)?.map { it.name } ?: emptyList())
    }

    @PostMapping
    @OnlyAdmin
    @Operation(
        summary = "Upload a background image",
        description = "Uploads the image to the backgrounds folder and returns its file location.",
        responses = [
            ApiResponse(responseCode = "200", description = "Background uploaded", content = [Content(mediaType = "text/plain", schema = Schema(type = "string"))]),
            ApiResponse(responseCode = "401", description = "User has no admin role", content = [Content(schema = Schema(hidden = true))]),
            ApiResponse(responseCode = "500", description = "Image error", content = [Content(mediaType = "text/plain", schema = Schema(type = "string"))])
        ]
    )
    fun addBackground(@RequestParam("file") file: MultipartFile): ResponseEntity<String> {
        return ResponseEntity.ok(imageService.upload(BACKGROUND, file).toString())
    }

    @DeleteMapping("/{filename}")
    @OnlyAdmin
    @Operation(
        summary = "Delete an existing background image",
        description = "Deletes a background image, identified with its file name.",
        responses = [
            ApiResponse(responseCode = "200", description = "Background deleted", content = [Content(mediaType = "text/plain", schema = Schema(type = "string"))]),
            ApiResponse(responseCode = "400", description = "Background doesn't exist", content = [Content(mediaType = "application/json", schema = Schema(implementation = BadRequestResponse::class))]),
            ApiResponse(responseCode = "401", description = "User has no admin role", content = [Content(schema = Schema(hidden = true))]),
            ApiResponse(responseCode = "500", description = "Image error", content = [Content(mediaType = "text/plain", schema = Schema(type = "string"))])
        ]
    )
    fun deleteBackground(@PathVariable filename: String): ResponseEntity<String> {
        imageService.delete(BACKGROUND, filename)
        return ResponseEntity.ok("Background deleted successfully.")
    }
}