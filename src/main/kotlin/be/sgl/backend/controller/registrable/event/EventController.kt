package be.sgl.backend.controller.registrable.event

import be.sgl.backend.config.security.OnlyAdmin
import be.sgl.backend.config.security.Public
import be.sgl.backend.dto.registrable.event.CreateOrUpdateEventRequest
import be.sgl.backend.dto.registrable.event.EventBaseDTO
import be.sgl.backend.dto.registrable.event.EventDTO
import be.sgl.backend.dto.registrable.event.EventResult
import be.sgl.backend.service.registrable.event.EventService
import io.github.wimdeblauwe.errorhandlingspringbootstarter.ApiErrorResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@Validated
@RestController
@RequestMapping("/events")
@Tag(name = "Events", description = "Endpoints for managing events.")
class EventController(
    private val eventService: EventService
) {

    @GetMapping(produces = [APPLICATION_JSON_VALUE])
    @OnlyAdmin
    @Operation(
        summary = "Get all events",
        description = "Returns a list of all events, regardless of their state.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(array = ArraySchema(schema = Schema(implementation = EventResult::class)))]),
        ]
    )
    fun getAllEvents(): List<EventResult> {
        return eventService.getAllEvents()
    }

    @GetMapping("/visible", produces = [APPLICATION_JSON_VALUE])
    @Public
    @Operation(
        summary = "Get all visible events",
        description = "Returns a list of all events that didn't end yet or aren't cancelled, and thus should be visible for everyone.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(array = ArraySchema(schema = Schema(implementation = EventDTO::class)))])
        ]
    )
    fun getVisibleEvents(): List<EventBaseDTO> {
        return eventService.getVisibleEvents()
    }

    @GetMapping("/{id}", produces = [APPLICATION_JSON_VALUE])
    @Public
    @Operation(
        summary = "Get a specific event",
        description = "Returns the event with the given id, regardless of its state.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(schema = Schema(implementation = EventDTO::class))]),
            ApiResponse(responseCode = "404", description = "Invalid id", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun getEvent(@PathVariable id: Int): EventDTO {
        return eventService.getEventDTOById(id)
    }

    @PostMapping(consumes = [APPLICATION_JSON_VALUE], produces = [APPLICATION_JSON_VALUE])
    @OnlyAdmin
    @Operation(
        summary = "Create an event",
        description = "Creates an event with the provided request body and returns it.",
        responses = [
            ApiResponse(responseCode = "201", description = "Event created", content = [Content(schema = Schema(implementation = EventDTO::class))]),
            ApiResponse(responseCode = "400", description = "Bad event format", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun createEvent(@Valid @RequestBody request: CreateOrUpdateEventRequest): ResponseEntity<EventDTO> {
        return ResponseEntity(eventService.createEvent(request), HttpStatus.CREATED)
    }

    @PutMapping("/{id}", consumes = [APPLICATION_JSON_VALUE], produces = [APPLICATION_JSON_VALUE])
    @OnlyAdmin
    @Operation(
        summary = "Update an existing event",
        description = "Updates an event, identified with the given id, with the provided request body and returns it. Only event with unopened registrations can be fully edited.",
        responses = [
            ApiResponse(responseCode = "200", description = "Event updated", content = [Content(schema = Schema(implementation = EventDTO::class))]),
            ApiResponse(responseCode = "400", description = "Bad event format", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "404", description = "Invalid id", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun updateEvent(@PathVariable id: Int, @Valid @RequestBody request: CreateOrUpdateEventRequest): EventDTO {
        return eventService.updateEvent(id, request)
    }

    @DeleteMapping("/{id}", produces = [APPLICATION_JSON_VALUE])
    @OnlyAdmin
    @Operation(
        summary = "Cancel an existing event",
        description = "Cancels an event, identified with the given id. The event cannot yet be started. If the event has linked registrations, they will be refunded.",
        responses = [
            ApiResponse(responseCode = "200", description = "Event cancelled"),
            ApiResponse(responseCode = "400", description = "Event cannot be cancelled", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "404", description = "Invalid id", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun cancelEvent(@PathVariable id: Int) {
        eventService.cancelEvent(id)
    }
}