package be.sgl.backend.controller

import be.sgl.backend.config.security.OnlyAuthenticated
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
            ApiResponse(responseCode = "200", description = "Image uploaded", content = [Content(mediaType = "text/plain", schema = Schema(type = "string"))]),
            ApiResponse(responseCode = "500", description = "Image error", content = [Content(mediaType = "text/plain", schema = Schema(type = "string"))])
        ]
    )
    fun uploadCalendarItemImage(@RequestParam("file") file: MultipartFile): ResponseEntity<String> {
        return ResponseEntity.ok(imageService.upload(TEMPORARY, file).toString())
    }
}