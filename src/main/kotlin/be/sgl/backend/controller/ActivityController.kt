package be.sgl.backend.controller

import be.sgl.backend.config.BadRequestResponse
import be.sgl.backend.config.CustomUserDetails
import be.sgl.backend.config.security.OnlyAdmin
import be.sgl.backend.config.security.OnlyAuthenticated
import be.sgl.backend.config.security.OnlyStaff
import be.sgl.backend.dto.ActivityBaseDTO
import be.sgl.backend.dto.ActivityDTO
import be.sgl.backend.dto.ActivityRegistrationDTO
import be.sgl.backend.service.activity.ActivityService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.net.URI

@RestController
@RequestMapping("/activities")
@Tag(name = "Activity", description = "Endpoints for managing activities and registering users at these activities.")
class ActivityController {

    @Autowired
    private lateinit var activityService: ActivityService

    @GetMapping
    @OnlyAdmin
    @Operation(
        summary = "Get all activities",
        description = "Returns a list of all activities, regardless of their state.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(mediaType = "application/json", schema = Schema(type = "array", implementation = ActivityDTO::class))]),
            ApiResponse(responseCode = "401", description = "User has no admin role", content = [Content(schema = Schema(hidden = true))])
        ]
    )
    fun getAllActivities(): ResponseEntity<List<ActivityBaseDTO>> {
        return ResponseEntity.ok(activityService.getAllActivities())
    }

    @GetMapping("/visible")
    @Operation(
        summary = "Get all visible activities",
        description = "Returns a list of all activities that didn't end yet or aren't cancelled, and thus should be visible for everyone.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(mediaType = "application/json", schema = Schema(type = "array", implementation = ActivityDTO::class))])
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
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(mediaType = "application/json", schema = Schema(implementation = ActivityDTO::class))]),
            ApiResponse(responseCode = "404", description = "Invalid id", content = [Content(mediaType = "text/plain", schema = Schema(type = "string"))])
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
            ApiResponse(responseCode = "201", description = "Activity created", content = [Content(mediaType = "application/json", schema = Schema(implementation = ActivityDTO::class))]),
            ApiResponse(responseCode = "400", description = "Bad activity format", content = [Content(mediaType = "application/json", schema = Schema(implementation = BadRequestResponse::class))]),
            ApiResponse(responseCode = "401", description = "User has no admin role", content = [Content(schema = Schema(hidden = true))])
        ]
    )
    fun createActivity(@Valid @RequestBody branch: ActivityDTO): ResponseEntity<ActivityDTO> {
        return ResponseEntity(activityService.saveActivityDTO(branch), HttpStatus.CREATED)
    }

    @PutMapping("/{id}")
    @OnlyAdmin
    @Operation(
        summary = "Update an existing activity",
        description = "Updates an activity, identified with the given id, with the provided request body and returns it. Only activities with unopened registrations can be fully edited.",
        responses = [
            ApiResponse(responseCode = "200", description = "Activity updated", content = [Content(mediaType = "application/json", schema = Schema(implementation = ActivityDTO::class))]),
            ApiResponse(responseCode = "400", description = "Bad activity format", content = [Content(mediaType = "application/json", schema = Schema(implementation = BadRequestResponse::class))]),
            ApiResponse(responseCode = "401", description = "User has no admin role", content = [Content(schema = Schema(hidden = true))]),
            ApiResponse(responseCode = "404", description = "Invalid id", content = [Content(mediaType = "text/plain", schema = Schema(type = "string"))])
        ]
    )
    fun updateActivity(@PathVariable id: Int, @Valid @RequestBody activity: ActivityDTO): ResponseEntity<ActivityDTO> {
        return ResponseEntity.ok(activityService.mergeActivityDTOChanges(id, activity))
    }

    @DeleteMapping("/{id}")
    @OnlyAdmin
    @Operation(
        summary = "Delete an existing activity",
        description = "Deletes an activity, identified with the given id. The activity cannot yet have registrations.",
        responses = [
            ApiResponse(responseCode = "200", description = "Activity deleted", content = [Content(mediaType = "text/plain", schema = Schema(type = "string"))]),
            ApiResponse(responseCode = "400", description = "Activity cannot be deleted", content = [Content(mediaType = "application/json", schema = Schema(implementation = BadRequestResponse::class))]),
            ApiResponse(responseCode = "401", description = "User has no admin role", content = [Content(schema = Schema(hidden = true))]),
            ApiResponse(responseCode = "404", description = "Invalid id", content = [Content(mediaType = "text/plain", schema = Schema(type = "string"))])
        ]
    )
    fun deleteActivity(@PathVariable id: Int): ResponseEntity<String> {
        activityService.deleteActivity(id)
        return ResponseEntity.ok("Activity deactivated successfully.")
    }

    @PostMapping("/{id}/register/{restrictionId}")
    @OnlyAuthenticated
    @Operation(
        summary = "Register the current user to the given activity",
        description = "Creates a registration to the given activity for the current user (if allowed) and redirects to the payment url.",
        responses = [
            ApiResponse(responseCode = "302", description = "Ok", content = [Content(mediaType = "text/plain", schema = Schema(type = "string"))]),
            ApiResponse(responseCode = "400", description = "Registration isn't possible", content = [Content(mediaType = "application/json", schema = Schema(implementation = BadRequestResponse::class))]),
            ApiResponse(responseCode = "401", description = "User is not logged in", content = [Content(schema = Schema(hidden = true))]),
            ApiResponse(responseCode = "404", description = "Invalid id", content = [Content(mediaType = "text/plain", schema = Schema(type = "string"))])
        ]
    )
    fun registerCurrentUser(@PathVariable id: Int, @PathVariable restrictionId: Int, @AuthenticationPrincipal userDetails: CustomUserDetails,
                            @RequestBody data: String, request: HttpServletRequest): ResponseEntity<Unit> {
        request.requestURI + "/updatePayment"
        val checkoutUrl = activityService.createPaymentForActivity(id, restrictionId, userDetails.username, data)
        val headers = HttpHeaders()
        headers.location = URI(checkoutUrl)
        return ResponseEntity(headers, HttpStatus.FOUND)
    }

    @PostMapping("/updatePayment")
    @PreAuthorize("permitAll()")
    @Operation(
        summary = "Trigger a payment update request",
        description = "Retrieves the payment based on the provided id and updates the payment status of the linked activity. This call never fails (except on server errors), to avoid exposing payment data.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(schema = Schema(hidden = true))])
        ]
    )
    fun updatePayment(@RequestBody paymentId: String): ResponseEntity<Unit> {
        activityService.updatePayment(paymentId)
        return ResponseEntity.ok().build()
    }

    @GetMapping("/{id}/registrations")
    @OnlyStaff
    @Operation(
        summary = "Get all registrations for the given activity",
        description = "Returns a list of all valid (i.e. paid and not cancelled) registrations for the given activity.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(mediaType = "application/json", schema = Schema(type = "array", implementation = ActivityRegistrationDTO::class))]),
            ApiResponse(responseCode = "401", description = "User has no staff role", content = [Content(schema = Schema(hidden = true))]),
            ApiResponse(responseCode = "404", description = "Invalid id", content = [Content(mediaType = "text/plain", schema = Schema(type = "string"))])
        ]
    )
    fun getAllRegistrationsForActivity(@PathVariable id: Int): ResponseEntity<List<ActivityRegistrationDTO>> {
        return ResponseEntity.ok(activityService.getAllRegistrationsForActivity(id))
    }
}