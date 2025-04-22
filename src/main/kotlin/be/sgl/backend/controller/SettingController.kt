package be.sgl.backend.controller

import be.sgl.backend.config.security.Public
import be.sgl.backend.service.SettingService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/settings")
@Tag(name = "Settings", description = "Endpoints for managing settings.")
class SettingController {

    @Autowired
    private lateinit var settingService: SettingService

    @GetMapping("/calendar-name")
    @Public
    @Operation(
        summary = "Get the name of the calendar.",
        description = "Returns the name used for denoting the calendar. If nothing is configured, it creates and returns the default name.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = String::class))])
        ]
    )
    fun getCalendarName(): ResponseEntity<String> {
        return ResponseEntity.ok(settingService.getCalendarName())
    }
}
