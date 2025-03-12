package be.sgl.backend.controller

import be.sgl.backend.config.BadRequestResponse
import be.sgl.backend.config.security.OnlyAdmin
import be.sgl.backend.dto.EventBaseDTO
import be.sgl.backend.dto.EventDTO
import be.sgl.backend.service.EventService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

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

    @GetMapping("/{id}")
    @Operation(
        summary = "Get a specific event",
        description = "Returns the event with the given id, regardless of its state.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(mediaType = "application/json", schema = Schema(implementation = EventDTO::class))]),
            ApiResponse(responseCode = "404", description = "Invalid id", content = [Content(mediaType = "text/plain", schema = Schema(type = "string"))])
        ]
    )
    fun getEvent(@PathVariable id: Int): ResponseEntity<EventDTO> {
        return ResponseEntity.ok(eventService.getEventDTOById(id))
    }

    @PostMapping
    @OnlyAdmin
    @Operation(
        summary = "Create an event",
        description = "Creates an event with the provided request body and returns it.",
        responses = [
            ApiResponse(responseCode = "201", description = "Event created", content = [Content(mediaType = "application/json", schema = Schema(implementation = EventDTO::class))]),
            ApiResponse(responseCode = "400", description = "Bad event format", content = [Content(mediaType = "application/json", schema = Schema(implementation = BadRequestResponse::class))]),
            ApiResponse(responseCode = "401", description = "User has no admin role", content = [Content(schema = Schema(hidden = true))])
        ]
    )
    fun createEvent(@Valid @RequestBody eventDTO: EventDTO): ResponseEntity<EventDTO> {
        return ResponseEntity(eventService.saveEventDTO(eventDTO), HttpStatus.CREATED)
    }

    @PutMapping("/{id}")
    @OnlyAdmin
    @Operation(
        summary = "Update an existing event",
        description = "Updates an event, identified with the given id, with the provided request body and returns it. Only event with unopened registrations can be fully edited.",
        responses = [
            ApiResponse(responseCode = "200", description = "Event updated", content = [Content(mediaType = "application/json", schema = Schema(implementation = EventDTO::class))]),
            ApiResponse(responseCode = "400", description = "Bad event format", content = [Content(mediaType = "application/json", schema = Schema(implementation = BadRequestResponse::class))]),
            ApiResponse(responseCode = "401", description = "User has no admin role", content = [Content(schema = Schema(hidden = true))]),
            ApiResponse(responseCode = "404", description = "Invalid id", content = [Content(mediaType = "text/plain", schema = Schema(type = "string"))])
        ]
    )
    fun updateEvent(@PathVariable id: Int, @Valid @RequestBody eventDTO: EventDTO): ResponseEntity<EventDTO> {
        return ResponseEntity.ok(eventService.mergeEventDTOChanges(id, eventDTO))
    }

    @DeleteMapping("/{id}")
    @OnlyAdmin
    @Operation(
        summary = "Delete an existing event",
        description = "Deletes an event, identified with the given id. The event cannot yet have registrations.",
        responses = [
            ApiResponse(responseCode = "200", description = "Event deleted", content = [Content(mediaType = "text/plain", schema = Schema(type = "string"))]),
            ApiResponse(responseCode = "400", description = "Event cannot be deleted", content = [Content(mediaType = "application/json", schema = Schema(implementation = BadRequestResponse::class))]),
            ApiResponse(responseCode = "401", description = "User has no admin role", content = [Content(schema = Schema(hidden = true))]),
            ApiResponse(responseCode = "404", description = "Invalid id", content = [Content(mediaType = "text/plain", schema = Schema(type = "string"))])
        ]
    )
    fun deleteEvent(@PathVariable id: Int): ResponseEntity<String> {
        eventService.deleteEvent(id)
        return ResponseEntity.ok("Event deleted successfully.")
    }
}