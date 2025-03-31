package be.sgl.backend.controller

import be.sgl.backend.config.CustomUserDetails
import be.sgl.backend.config.security.OnlyAdmin
import be.sgl.backend.config.security.OnlyAuthenticated
import be.sgl.backend.config.security.OnlyStaff
import be.sgl.backend.config.security.Public
import be.sgl.backend.dto.*
import be.sgl.backend.service.event.EventRegistrationService
import be.sgl.backend.service.event.EventService
import io.github.wimdeblauwe.errorhandlingspringbootstarter.ApiErrorResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/events")
@Tag(name = "Events", description = "Endpoints for managing events.")
class EventController {

    @Autowired
    private lateinit var eventService: EventService
    @Autowired
    private lateinit var registrationService: EventRegistrationService

    @GetMapping
    @OnlyAdmin
    @Operation(
        summary = "Get all events",
        description = "Returns a list of all events, regardless of their state.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(type = "array", implementation = EventResultDTO::class))]),
        ]
    )
    fun getAllEvents(): ResponseEntity<List<EventResultDTO>> {
        return ResponseEntity.ok(eventService.getAllEvents())
    }

    @GetMapping("/visible")
    @Public
    @Operation(
        summary = "Get all visible events",
        description = "Returns a list of all events that didn't end yet or aren't cancelled, and thus should be visible for everyone.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(type = "array", implementation = EventDTO::class))])
        ]
    )
    fun getVisibleEvents(): ResponseEntity<List<EventBaseDTO>> {
        return ResponseEntity.ok(eventService.getVisibleEvents())
    }

    @GetMapping("/{id}")
    @Public
    @Operation(
        summary = "Get a specific event",
        description = "Returns the event with the given id, regardless of its state.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = EventDTO::class))]),
            ApiResponse(responseCode = "404", description = "Invalid id", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))])
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
            ApiResponse(responseCode = "201", description = "Event created", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = EventDTO::class))]),
            ApiResponse(responseCode = "400", description = "Bad event format", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))])
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
            ApiResponse(responseCode = "200", description = "Event updated", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = EventDTO::class))]),
            ApiResponse(responseCode = "400", description = "Bad event format", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "404", description = "Invalid id", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))])
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
            ApiResponse(responseCode = "200", description = "Event deleted"),
            ApiResponse(responseCode = "400", description = "Event cannot be deleted", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "404", description = "Invalid id", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun deleteEvent(@PathVariable id: Int): ResponseEntity<Unit> {
        eventService.deleteEvent(id)
        return ResponseEntity.ok().build()
    }

    @GetMapping("/{id}/registrations")
    @OnlyStaff
    @Operation(
        summary = "Get all registrations for the given event",
        description = "Returns a list of all valid (i.e. paid and not cancelled) registrations for the given event.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(type = "array", implementation = EventRegistrationDTO::class))]),
            ApiResponse(responseCode = "404", description = "Invalid id", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun getAllRegistrationsForActivity(@PathVariable id: Int): ResponseEntity<List<EventRegistrationDTO>> {
        return ResponseEntity.ok(registrationService.getAllRegistrationsForEvent(id))
    }

    @GetMapping("/registrations/{registrationId}")
    @Public
    @Operation(
        summary = "Get a specific event registration",
        description = "Returns the registration identified with the given id.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(type = "array", implementation = EventRegistrationDTO::class))]),
            ApiResponse(responseCode = "204", description = "Not found", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun getRegistration(@PathVariable registrationId: Int): ResponseEntity<EventRegistrationDTO?> {
        val registration = registrationService.getEventRegistrationDTOById(registrationId)
        return registration?.let { ResponseEntity.ok(it) } ?: ResponseEntity.noContent().build()
    }

    @PostMapping("/{id}/register")
    @Public
    @Operation(
        summary = "Create a registration for the given event",
        description = "Creates a registration for the event with the given id and data and returns the payment url.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = PaymentUrl::class))]),
            ApiResponse(responseCode = "400", description = "Registration isn't possible", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "404", description = "Invalid id", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun register(@PathVariable id: Int, @AuthenticationPrincipal userDetails: CustomUserDetails?, @Valid @RequestBody attempt: EventRegistrationAttemptData): ResponseEntity<PaymentUrl> {
        val url = registrationService.createPaymentForEvent(id, attempt, userDetails?.username)
        return ResponseEntity.ok(PaymentUrl(url))
    }

    @PostMapping("/updatePayment", consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE])
    @Public
    @CrossOrigin(origins = ["*"])
    @Operation(
        summary = "Trigger a payment update request",
        description = "Retrieves the payment based on the provided id and updates the payment status of the linked event. This call never fails (except on server errors), to avoid exposing payment data.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok")
        ]
    )
    fun updatePayment(@RequestParam id: String): ResponseEntity<Unit> {
        registrationService.updatePayment(id)
        return ResponseEntity.ok().build()
    }

    @PutMapping("/registrations/{registrationId}")
    @OnlyStaff
    fun markPresent(@PathVariable registrationId: Int, @RequestParam present: Boolean): ResponseEntity<Unit> {
        TODO("Existing flow, but never used")
    }

    @DeleteMapping("/registrations/{registrationId}")
    @OnlyAuthenticated
    fun cancelRegistration(@PathVariable registrationId: Int): ResponseEntity<Unit> {
        TODO("New flow, not yet important")
    }
}