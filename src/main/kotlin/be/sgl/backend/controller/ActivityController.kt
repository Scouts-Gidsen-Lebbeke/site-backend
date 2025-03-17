package be.sgl.backend.controller

import be.sgl.backend.config.CustomUserDetails
import be.sgl.backend.config.security.OnlyAdmin
import be.sgl.backend.config.security.OnlyAuthenticated
import be.sgl.backend.config.security.OnlyStaff
import be.sgl.backend.dto.*
import be.sgl.backend.service.activity.ActivityRegistrationService
import be.sgl.backend.service.activity.ActivityService
import io.github.wimdeblauwe.errorhandlingspringbootstarter.ApiErrorResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.MediaType.APPLICATION_PDF_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/activities")
@Tag(name = "Activity", description = "Endpoints for managing activities and registering users at these activities.")
class ActivityController {

    @Autowired
    private lateinit var activityService: ActivityService
    @Autowired
    private lateinit var registrationService: ActivityRegistrationService

    @GetMapping
    @OnlyAdmin
    @Operation(
        summary = "Get all activities",
        description = "Returns a list of all activities, regardless of their state.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(type = "array", implementation = ActivityResultDTO::class))])
        ]
    )
    fun getAllActivities(): ResponseEntity<List<ActivityResultDTO>> {
        return ResponseEntity.ok(activityService.getAllActivities())
    }

    @GetMapping("/visible")
    @Operation(
        summary = "Get all visible activities",
        description = "Returns a list of all activities that didn't end yet or aren't cancelled, and thus should be visible for everyone.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(type = "array", implementation = ActivityDTO::class))])
        ]
    )
    fun getVisibleActivities(): ResponseEntity<List<ActivityBaseDTO>> {
        return ResponseEntity.ok(activityService.getVisibleActivities())
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Get a specific activity",
        description = "Returns the activity with the given id, regardless of its state.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ActivityDTO::class))]),
            ApiResponse(responseCode = "404", description = "Invalid id", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun getActivity(@PathVariable id: Int): ResponseEntity<ActivityDTO> {
        return ResponseEntity.ok(activityService.getActivityDTOById(id))
    }

    @PostMapping
    @OnlyAdmin
    @Operation(
        summary = "Create an activity",
        description = "Creates an activity with the provided request body and returns it.",
        responses = [
            ApiResponse(responseCode = "201", description = "Activity created", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ActivityDTO::class))]),
            ApiResponse(responseCode = "400", description = "Bad activity format", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun createActivity(@Valid @RequestBody activityDTO: ActivityDTO): ResponseEntity<ActivityDTO> {
        return ResponseEntity(activityService.saveActivityDTO(activityDTO), HttpStatus.CREATED)
    }

    @PutMapping("/{id}")
    @OnlyAdmin
    @Operation(
        summary = "Update an existing activity",
        description = "Updates an activity, identified with the given id, with the provided request body and returns it. Only activities with unopened registrations can be fully edited.",
        responses = [
            ApiResponse(responseCode = "200", description = "Activity updated", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ActivityDTO::class))]),
            ApiResponse(responseCode = "400", description = "Bad activity format", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "404", description = "Invalid id", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun updateActivity(@PathVariable id: Int, @Valid @RequestBody activityDTO: ActivityDTO): ResponseEntity<ActivityDTO> {
        return ResponseEntity.ok(activityService.mergeActivityDTOChanges(id, activityDTO))
    }

    @DeleteMapping("/{id}")
    @OnlyAdmin
    @Operation(
        summary = "Delete an existing activity",
        description = "Deletes an activity, identified with the given id. The activity cannot yet have registrations.",
        responses = [
            ApiResponse(responseCode = "200", description = "Activity deleted"),
            ApiResponse(responseCode = "400", description = "Activity cannot be deleted", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "404", description = "Invalid id", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun deleteActivity(@PathVariable id: Int): ResponseEntity<Unit> {
        activityService.deleteActivity(id)
        return ResponseEntity.ok().build()
    }

    @GetMapping("/{id}/registrations")
    @OnlyStaff
    @Operation(
        summary = "Get all registrations for the given activity",
        description = "Returns a list of all valid (i.e. paid and not cancelled) registrations for the given activity.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(type = "array", implementation = ActivityRegistrationDTO::class))]),
            ApiResponse(responseCode = "404", description = "Invalid id", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun getAllRegistrationsForActivity(@PathVariable id: Int): ResponseEntity<List<ActivityRegistrationDTO>> {
        return ResponseEntity.ok(registrationService.getAllRegistrationsForActivity(id))
    }

    @GetMapping("/registrations")
    @OnlyAuthenticated
    @Operation(
        summary = "Get all activity registrations for the current user",
        description = "Returns a list of all valid (i.e. paid and not cancelled) activity registrations for the given user.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(type = "array", implementation = ActivityRegistrationDTO::class))]),
            ApiResponse(responseCode = "404", description = "Invalid id", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun getAllRegistrationsForCurrentUser(@AuthenticationPrincipal userDetails: CustomUserDetails): ResponseEntity<List<ActivityRegistrationDTO>> {
        return ResponseEntity.ok(registrationService.getAllRegistrationsForUser(userDetails.username))
    }

    @GetMapping("/registrations/{registrationId}")
    @Operation(
        summary = "Get a specific activity registration",
        description = "Returns the registration identified with the given id.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(type = "array", implementation = ActivityRegistrationDTO::class))]),
            ApiResponse(responseCode = "404", description = "Invalid id", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun getRegistration(@PathVariable registrationId: Int): ResponseEntity<ActivityRegistrationDTO?> {
        return ResponseEntity.ok(registrationService.getActivityRegistrationDTOById(registrationId))
    }

    @GetMapping("/{id}/status")
    @OnlyAuthenticated
    @Operation(
        summary = "Get the registration status for the current user",
        description = "Returns the registration status for the current user and the activity identified with the given id.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(type = "array", implementation = ActivityRegistrationStatus::class))]),
            ApiResponse(responseCode = "404", description = "Invalid id", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun getRegistrationStatusForCurrentUser(@PathVariable id: Int, @AuthenticationPrincipal userDetails: CustomUserDetails): ResponseEntity<ActivityRegistrationStatus> {
        return ResponseEntity.ok(registrationService.getStatusForActivityAndUser(id, userDetails.username))
    }

    @GetMapping("/{id}/user/{username}/status")
    @OnlyStaff
    @Operation(
        summary = "Get the registration status for the given user",
        description = "Returns the registration status for the user with the provided username and the activity identified with the given id.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(type = "array", implementation = ActivityRegistrationStatus::class))]),
            ApiResponse(responseCode = "404", description = "Invalid id", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun getRegistrationStatusForUser(@PathVariable id: Int, @PathVariable username: String): ResponseEntity<ActivityRegistrationStatus> {
        return ResponseEntity.ok(registrationService.getStatusForActivityAndUser(id, username))
    }

    @PostMapping("/{id}/register/{restrictionId}")
    @OnlyAuthenticated
    @Operation(
        summary = "Register the current user to the given activity",
        description = "Creates a registration to the given activity for the current user (if allowed) and returns the payment url.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = PaymentUrl::class))]),
            ApiResponse(responseCode = "400", description = "Registration isn't possible", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "404", description = "Invalid id", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun registerCurrentUser(@PathVariable id: Int, @PathVariable restrictionId: Int, @AuthenticationPrincipal userDetails: CustomUserDetails, @RequestBody data: String): ResponseEntity<PaymentUrl> {
        val url = registrationService.createPaymentForActivity(id, restrictionId, userDetails.username, data)
        return ResponseEntity.ok(PaymentUrl(url))
    }

    @PostMapping("/{id}/user/{username}/register/{restrictionId}")
    @OnlyAuthenticated
    @Operation(
        summary = "Register the given user to the given activity",
        description = "Creates a registration to the given activity for the given user (if allowed) and returns the payment url.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = PaymentUrl::class))]),
            ApiResponse(responseCode = "400", description = "Registration isn't possible", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "404", description = "Invalid id", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun registerUser(@PathVariable id: Int, @PathVariable username: String, @PathVariable restrictionId: Int, @RequestBody data: String): ResponseEntity<PaymentUrl> {
        val url = registrationService.createPaymentForActivity(id, restrictionId, username, data)
        return ResponseEntity.ok(PaymentUrl(url))
    }

    @PostMapping("/updatePayment", consumes = [MediaType.TEXT_PLAIN_VALUE])
    @PreAuthorize("permitAll()")
    @Operation(
        summary = "Trigger a payment update request",
        description = "Retrieves the payment based on the provided id and updates the payment status of the linked activity. This call never fails (except on server errors), to avoid exposing payment data.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok")
        ]
    )
    fun updatePayment(@RequestBody paymentId: String): ResponseEntity<Unit> {
        registrationService.updatePayment(paymentId)
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

    @GetMapping("/registrations/{registrationId}/certificate")
    @OnlyAuthenticated
    @Operation(
        summary = "Generate the participation certificate",
        description = "Returns a pdf with a participation certificate status for the current user and the completed registration identified with the given id.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(mediaType = APPLICATION_PDF_VALUE)]),
            ApiResponse(responseCode = "400", description = "Registration isn't completed", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "404", description = "Invalid id", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun getCertificateForRegistration(@PathVariable registrationId: Int): ResponseEntity<ByteArray> {
        val form = registrationService.getCertificateForRegistration(registrationId)
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"form.pdf\"")
            .contentType(MediaType.APPLICATION_PDF)
            .body(form)
    }
}