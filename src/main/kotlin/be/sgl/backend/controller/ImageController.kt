package be.sgl.backend.controller

import be.sgl.backend.config.security.OnlyAuthenticated
import be.sgl.backend.dto.RemoteFile
import be.sgl.backend.service.ImageService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import be.sgl.backend.service.ImageService.ImageDirectory.TEMPORARY
import io.github.wimdeblauwe.errorhandlingspringbootstarter.ApiErrorResponse
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE

@RestController
@RequestMapping("/image")
@Tag(name = "Image", description = "Endpoints for handling temporarily uploaded images items.")
class ImageController {

    @Autowired
    lateinit var imageService: ImageService

    @PostMapping
    @OnlyAuthenticated
    @Operation(
        summary = "Upload an image",
        description = "Uploads the image to the temporary folder and returns its file location.",
        responses = [
            ApiResponse(responseCode = "200", description = "Image uploaded", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = RemoteFile::class))]),
            ApiResponse(responseCode = "400", description = "Bad image format", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "500", description = "Image error", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun uploadCalendarItemImage(@RequestParam("file") file: MultipartFile): ResponseEntity<RemoteFile> {
        val uploadedFile = imageService.upload(TEMPORARY, file)
        return ResponseEntity.ok(RemoteFile(uploadedFile))
    }
}