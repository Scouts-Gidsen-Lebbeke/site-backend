package be.sgl.backend.controller

import be.sgl.backend.config.security.OnlyAdmin
import be.sgl.backend.dto.EventBaseDTO
import be.sgl.backend.dto.EventDTO
import be.sgl.backend.service.EventService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/events")
@Tag(name = "Events", description = "Endpoints for managing events.")
class EventController {

    @Autowired
    private lateinit var eventService: EventService

    @GetMapping
    @OnlyAdmin
    @Operation(
        summary = "Get all events",
        description = "Returns a list of all events, regardless of their state.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(mediaType = "application/json", schema = Schema(type = "array", implementation = EventDTO::class))]),
            ApiResponse(responseCode = "401", description = "User has no admin role", content = [Content(schema = Schema(hidden = true))])
        ]
    )
    fun getAllEvents(): ResponseEntity<List<EventBaseDTO>> {
        return ResponseEntity.ok(eventService.getAllEvents())
    }

    @GetMapping("/visible")
    @Operation(
        summary = "Get all visible events",
        description = "Returns a list of all events that didn't end yet or aren't cancelled, and thus should be visible for everyone.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(mediaType = "application/json", schema = Schema(type = "array", implementation = EventDTO::class))])
        ]
    )
    fun getVisibleEvents(): ResponseEntity<List<EventBaseDTO>> {
        return ResponseEntity.ok(eventService.getVisibleEvents())
    }
}