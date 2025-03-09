package be.sgl.backend.controller

import be.sgl.backend.service.ImageService.ImageDirectory.BACKGROUND
import be.sgl.backend.service.ImageService.Companion.IMAGE_BASE_PATH
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.io.File
import kotlin.io.path.Path

@RestController
@RequestMapping("/backgrounds")
@Tag(name = "Backgrounds", description = "Endpoints for managing backgrounds.")
class BackgroundController {
    @GetMapping
    fun getBackgrounds(): ResponseEntity<List<String>> {
        val dir = Path(IMAGE_BASE_PATH, BACKGROUND.path).toFile()
        return ResponseEntity.ok(dir.listFiles()?.filter(File::isFile)?.map { it.name } ?: emptyList())
    }
}