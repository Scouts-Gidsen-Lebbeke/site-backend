package be.sgl.backend.controller.registrable.activity

import be.sgl.backend.config.security.OnlyAdmin
import be.sgl.backend.config.security.Public
import be.sgl.backend.dto.registrable.activity.ActivityBaseDTO
import be.sgl.backend.dto.registrable.activity.ActivityDTO
import be.sgl.backend.dto.registrable.activity.ActivityResult
import be.sgl.backend.dto.registrable.activity.CreateOrUpdateActivityRequest
import be.sgl.backend.service.registrable.activity.ActivityService
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
@RequestMapping("/activities")
@Tag(name = "Activity", description = "Endpoints for managing activities and registering users at these activities.")
class ActivityController(
    private val activityService: ActivityService
) {

    @GetMapping(produces = [APPLICATION_JSON_VALUE])
    @OnlyAdmin
    @Operation(
        summary = "Get all activities",
        description = "Returns a list of all activities, regardless of their state.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(array = ArraySchema(schema = Schema(implementation = ActivityResult::class)))])
        ]
    )
    fun getAllActivities(): List<ActivityResult> {
        return activityService.getAllActivities()
    }

    @GetMapping("/visible", produces = [APPLICATION_JSON_VALUE])
    @Public
    @Operation(
        summary = "Get all visible activities",
        description = "Returns a list of all activities that didn't end yet or aren't cancelled, and thus should be visible for everyone.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(array = ArraySchema(schema = Schema(implementation = ActivityDTO::class)))])
        ]
    )
    fun getVisibleActivities(): List<ActivityBaseDTO> {
        return activityService.getVisibleActivities()
    }

    @GetMapping("/{id}", produces = [APPLICATION_JSON_VALUE])
    @Public
    @Operation(
        summary = "Get a specific activity",
        description = "Returns the activity with the given id, regardless of its state.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(schema = Schema(implementation = ActivityDTO::class))]),
            ApiResponse(responseCode = "404", description = "Invalid id", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun getActivity(@PathVariable id: Int): ActivityDTO {
        return activityService.getActivityDTOById(id)
    }

    @PostMapping(consumes = [APPLICATION_JSON_VALUE], produces = [APPLICATION_JSON_VALUE])
    @OnlyAdmin
    @Operation(
        summary = "Create an activity",
        description = "Creates an activity with the provided request body and returns it.",
        responses = [
            ApiResponse(responseCode = "201", description = "Activity created", content = [Content(schema = Schema(implementation = ActivityDTO::class))]),
            ApiResponse(responseCode = "400", description = "Bad activity format", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun createActivity(@Valid @RequestBody request: CreateOrUpdateActivityRequest): ResponseEntity<ActivityDTO> {
        return ResponseEntity(activityService.createActivity(request), HttpStatus.CREATED)
    }

    @PutMapping("/{id}", consumes = [APPLICATION_JSON_VALUE], produces = [APPLICATION_JSON_VALUE])
    @OnlyAdmin
    @Operation(
        summary = "Update an existing activity",
        description = "Updates an activity, identified with the given id, with the provided request body and returns it. Only activities with unopened registrations can be fully edited.",
        responses = [
            ApiResponse(responseCode = "200", description = "Activity updated", content = [Content(schema = Schema(implementation = ActivityDTO::class))]),
            ApiResponse(responseCode = "400", description = "Bad activity format", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "404", description = "Invalid id", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun updateActivity(@PathVariable id: Int, @Valid @RequestBody request: CreateOrUpdateActivityRequest): ActivityDTO {
        return activityService.updateActivity(id, request)
    }

    @DeleteMapping("/{id}", produces = [APPLICATION_JSON_VALUE])
    @OnlyAdmin
    @Operation(
        summary = "Cancel an existing activity",
        description = "Cancels an activity, identified with the given id. The activity cannot yet be started. If the activity has linked registrations, they will be refunded.",
        responses = [
            ApiResponse(responseCode = "200", description = "Activity cancelled"),
            ApiResponse(responseCode = "400", description = "Activity cannot be cancelled", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "404", description = "Invalid id", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun cancelActivity(@PathVariable id: Int) {
        activityService.cancelActivity(id)
    }
}