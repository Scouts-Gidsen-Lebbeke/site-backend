package be.sgl.backend.controller.calendar

import be.sgl.backend.config.security.OnlyAdmin
import be.sgl.backend.config.security.Public
import be.sgl.backend.dto.calendar.*
import be.sgl.backend.service.calendar.CalendarPeriodService
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
@RequestMapping("/calendars/periods")
@Tag(name = "Calendar periods", description = "Endpoints for managing calendar periods.")
class CalendarPeriodController(
    private val calendarPeriodService: CalendarPeriodService
) {

    @GetMapping(produces = [APPLICATION_JSON_VALUE])
    @Public
    @Operation(
        summary = "Get all calendar periods",
        description = "Returns a list of all periods.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(array = ArraySchema(schema = Schema(implementation = CalendarPeriodDTO::class)))])
        ]
    )
    fun getAllPeriods(): List<CalendarPeriodDTO> {
        return calendarPeriodService.getAllCalendarPeriods()
    }

    @GetMapping("/{id}", produces = [APPLICATION_JSON_VALUE])
    @Public
    @Operation(
        summary = "Get a specific calendar period",
        description = "Returns the calendar period with the given id.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(schema = Schema(implementation = CalendarPeriodDTO::class))]),
            ApiResponse(responseCode = "404", description = "Invalid id", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun getPeriod(@PathVariable id: Int): CalendarPeriodDTO {
        return calendarPeriodService.getCalendarPeriodDTOById(id)
    }

    @PostMapping(consumes = [APPLICATION_JSON_VALUE], produces = [APPLICATION_JSON_VALUE])
    @OnlyAdmin
    @Operation(
        summary = "Create a calendar period",
        description = "Creates a calendar period with the provided request body, together with a calendar for all visible branches, and returns it.",
        responses = [
            ApiResponse(responseCode = "201", description = "Calendar period created", content = [Content(schema = Schema(implementation = CalendarPeriodDTO::class))]),
            ApiResponse(responseCode = "400", description = "Bad calendar period format", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun createCalendarPeriod(@Valid @RequestBody request: CreateOrUpdateCalendarPeriodRequest): ResponseEntity<CalendarPeriodDTO> {
        return ResponseEntity(calendarPeriodService.createCalendarPeriod(request), HttpStatus.CREATED)
    }

    @PutMapping("/{id}", consumes = [APPLICATION_JSON_VALUE], produces = [APPLICATION_JSON_VALUE])
    @OnlyAdmin
    @Operation(
        summary = "Update an existing calendar period",
        description = "Updates a calendar period, identified with the given id, with the provided request body and returns it.",
        responses = [
            ApiResponse(responseCode = "200", description = "Calendar period updated", content = [Content(schema = Schema(implementation = CalendarPeriodDTO::class))]),
            ApiResponse(responseCode = "400", description = "Bad calendar period format", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "404", description = "Invalid id", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun updateCalendarPeriod(@PathVariable id: Int, @Valid @RequestBody request: CreateOrUpdateCalendarPeriodRequest): CalendarPeriodDTO {
        return calendarPeriodService.updateCalendarPeriod(id, request)
    }

    @DeleteMapping("/{id}", produces = [APPLICATION_JSON_VALUE])
    @OnlyAdmin
    @Operation(
        summary = "Delete an existing calendar period",
        description = "Deletes a calendar period, identified with the given id. All linked calendars, its items and images are also deleted.",
        responses = [
            ApiResponse(responseCode = "200", description = "Calendar period deleted"),
            ApiResponse(responseCode = "404", description = "Invalid id", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "500", description = "Image delete error", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun deleteCalendarPeriod(@PathVariable id: Int) {
        calendarPeriodService.deleteCalendarPeriod(id)
    }
}