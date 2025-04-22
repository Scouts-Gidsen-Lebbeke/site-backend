package be.sgl.backend.controller

import be.sgl.backend.config.security.Public
import be.sgl.backend.service.SseService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@Controller
@RequestMapping("/streams")
@Tag(name = "Streams", description = "Endpoints for managing event streams.")
class SseController {

    @Autowired
    private lateinit var sseService: SseService

    @GetMapping("/{emitterId}")
    @Public
    @Operation(
        summary = "Get a specific event stream",
        description = "Returns the event stream linked to the given id, if any.",
        responses = [
            ApiResponse(responseCode = "200", description = "SSE stream established", content = [Content(mediaType = TEXT_EVENT_STREAM_VALUE, schema = Schema(type = "string", format = "binary"))])
        ]
    )
    fun getEmitter(@PathVariable emitterId: String): SseEmitter? {
        return sseService.getEmitter(emitterId)
    }
}
