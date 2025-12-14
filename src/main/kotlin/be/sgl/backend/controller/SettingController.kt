package be.sgl.backend.controller

import be.sgl.backend.config.security.OnlyAdmin
import be.sgl.backend.config.security.Public
import be.sgl.backend.service.SettingService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType.TEXT_PLAIN_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*

@Controller
@RequestMapping("/settings")
@Tag(name = "Settings", description = "Endpoints for managing settings.")
class SettingController(
    private val settingService: SettingService
) {

    @GetMapping("/{id}")
    @Public
    @Operation(
        summary = "Get the setting value",
        description = "Returns the value of the setting identified with the given id.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(mediaType = TEXT_PLAIN_VALUE)])
        ]
    )
    fun getSetting(@PathVariable id: String): ResponseEntity<String?> {
        return ResponseEntity.ok(settingService.get(id))
    }

    @PutMapping("/{id}")
    @OnlyAdmin
    @Operation(
        summary = "Update the setting value",
        description = "Creates or updates the value of the setting identified with the given id. If the value isn't present, the setting is deleted.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok")
        ]
    )
    fun updateSetting(@PathVariable id: String, @RequestParam value: String?) {
        settingService.update(id, value)
    }
}
