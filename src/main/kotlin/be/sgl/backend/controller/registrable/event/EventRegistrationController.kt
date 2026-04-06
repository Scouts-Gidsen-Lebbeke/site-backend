package be.sgl.backend.controller.registrable.event

import be.sgl.backend.config.CustomUserDetails
import be.sgl.backend.config.security.OnlyStaff
import be.sgl.backend.config.security.Public
import be.sgl.backend.dto.PaymentUrl
import be.sgl.backend.dto.registrable.event.CreateEventRegistrationRequest
import be.sgl.backend.dto.registrable.event.EventRegistrationDTO
import be.sgl.backend.service.registrable.event.EventRegistrationService
import io.github.wimdeblauwe.errorhandlingspringbootstarter.ApiErrorResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.MediaType
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@Validated
@RestController
@RequestMapping("/events")
@Tag(name = "Event registrations", description = "Endpoints for managing event registrations.")
class EventRegistrationController(
    private val registrationService: EventRegistrationService
) {

    @GetMapping("/{id}/registrations", produces = [APPLICATION_JSON_VALUE])
    @OnlyStaff
    @Operation(
        summary = "Get all registrations for the given event",
        description = "Returns a list of all valid (i.e. paid and not cancelled) registrations for the given event.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(array = ArraySchema(schema = Schema(implementation = EventRegistrationDTO::class)))]),
            ApiResponse(responseCode = "404", description = "Invalid id", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun getAllRegistrationsForActivity(@PathVariable id: Int): List<EventRegistrationDTO> {
        return registrationService.getAllRegistrationsForEvent(id)
    }

    @GetMapping("/registrations/{registrationId}", produces = [APPLICATION_JSON_VALUE])
    @Public
    @Operation(
        summary = "Get a specific event registration",
        description = "Returns the registration identified with the given id.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(array = ArraySchema(schema = Schema(implementation = EventRegistrationDTO::class)))]),
            ApiResponse(responseCode = "204", description = "Not found")
        ]
    )
    fun getRegistration(@PathVariable registrationId: Int): ResponseEntity<EventRegistrationDTO> {
        val registration = registrationService.getEventRegistrationDTOById(registrationId)
        return registration?.let { ResponseEntity.ok(it) } ?: ResponseEntity.noContent().build()
    }

    @PostMapping("/{id}/register", consumes = [APPLICATION_JSON_VALUE], produces = [APPLICATION_JSON_VALUE])
    @Public
    @Operation(
        summary = "Create a registration for the given event",
        description = "Creates a registration for the event with the given id and data and returns the payment url.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(schema = Schema(implementation = PaymentUrl::class))]),
            ApiResponse(responseCode = "400", description = "Registration isn't possible", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "404", description = "Invalid id", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun register(@PathVariable id: Int, @AuthenticationPrincipal userDetails: CustomUserDetails?, @Valid @RequestBody request: CreateEventRegistrationRequest): PaymentUrl {
        val url = registrationService.createPaymentForEvent(id, request, userDetails?.username)
        return PaymentUrl(url)
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
    fun updatePayment(@RequestParam id: String) {
        registrationService.updatePayment(id)
    }

    @PatchMapping("/registrations/{registrationId}/complete", produces = [APPLICATION_JSON_VALUE])
    @OnlyStaff
    @Operation(
        summary = "Mark an event registration as completed",
        description = "Retrieves the registration based on the provided id and marks it as completed. Also notifies the linked customer if configured.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok"),
            ApiResponse(responseCode = "400", description = "Registration isn't paid", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "404", description = "Invalid id", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun markCompleted(@PathVariable registrationId: Int) {
        registrationService.markRegistrationAsCompleted(registrationId)
    }

    @DeleteMapping("/registrations/{registrationId}", produces = [APPLICATION_JSON_VALUE])
    @Public
    @Operation(
        summary = "Refund an event registration",
        description = "Retrieves the registration based on the provided id and create a payment refund.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok"),
            ApiResponse(responseCode = "400", description = "Registration isn't eligible for cancellation", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "404", description = "Invalid id", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun cancelRegistration(@PathVariable registrationId: Int) {
        registrationService.cancelRegistration(registrationId)
    }
}