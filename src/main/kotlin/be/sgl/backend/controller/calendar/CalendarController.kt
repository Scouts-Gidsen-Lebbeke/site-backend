package be.sgl.backend.controller.calendar

import be.sgl.backend.config.security.OnlyStaff
import be.sgl.backend.config.security.Public
import be.sgl.backend.dto.calendar.*
import be.sgl.backend.service.calendar.CalendarService
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
@RequestMapping("/calendars")
@Tag(name = "Calendars", description = "Endpoints for managing calendars and their items.")
class CalendarController(
    private val calendarService: CalendarService
) {

    @GetMapping("/current", produces = [APPLICATION_JSON_VALUE])
    @Public
    @Operation(
        summary = "Get all current calendars",
        description = "Returns a list of all calendars where the current date lays between its start and end date.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(array = ArraySchema(schema = Schema(implementation = CalendarDTO::class)))]),
        ]
    )
    fun getCurrentCalendars(): List<CalendarDTO> {
        return calendarService.getCurrentCalendars()
    }

    @GetMapping("/period/{id}", produces = [APPLICATION_JSON_VALUE])
    @Public
    @Operation(
        summary = "Get the calendars for a specified period",
        description = "Returns a list of all calendars linked to the period identified with the given id.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(array = ArraySchema(schema = Schema(implementation = CalendarDTO::class)))]),
            ApiResponse(responseCode = "404", description = "Invalid id", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun getCalendarsByPeriod(@PathVariable id: Int): List<CalendarDTO> {
        return calendarService.getCalendarsByPeriod(id)
    }

    @GetMapping("/{id}", produces = [APPLICATION_JSON_VALUE])
    @Public
    @Operation(
        summary = "Get a specific calendar",
        description = "Returns the calendar with the given id.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(schema = Schema(implementation = CalendarDTO::class))]),
            ApiResponse(responseCode = "404", description = "Invalid id", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun getCalendar(@PathVariable id: Int, @RequestParam(required = false, defaultValue = "false") withDefaults: Boolean): CalendarDTO {
        return calendarService.getCalendarDTOById(id, withDefaults)
    }

    @PutMapping("/{id}", consumes = [APPLICATION_JSON_VALUE], produces = [APPLICATION_JSON_VALUE])
    @OnlyStaff
    @Operation(
        summary = "Update an existing calendar",
        description = "Updates a calendar, identified with the given id, with the provided request body and returns it.",
        responses = [
            ApiResponse(responseCode = "200", description = "Calendar updated", content = [Content(schema = Schema(implementation = CalendarDTO::class))]),
            ApiResponse(responseCode = "404", description = "Invalid id", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun updateCalendar(@PathVariable id: Int, @Valid @RequestBody updateRequest: UpdateCalendarRequest): CalendarDTO {
        return calendarService.updateCalendar(id, updateRequest)
    }

    @GetMapping("/items/{id}", produces = [APPLICATION_JSON_VALUE])
    @Public
    @Operation(
        summary = "Get a specific calendar item",
        description = "Returns the calendar item with the given id.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(schema = Schema(implementation = CalendarItemWithCalendarsDTO::class))]),
            ApiResponse(responseCode = "404", description = "Invalid id", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun getCalendarItem(@PathVariable id: Int): CalendarItemWithCalendarsDTO {
        return calendarService.getCalendarItemDTOById(id)
    }

    @PostMapping("/items", consumes = [APPLICATION_JSON_VALUE], produces = [APPLICATION_JSON_VALUE])
    @OnlyStaff
    @Operation(
        summary = "Create a calendar item",
        description = "Creates a calendar item with the provided request body, together with a calendar for all visible branches, and returns it.",
        responses = [
            ApiResponse(responseCode = "201", description = "Calendar item created", content = [Content(schema = Schema(implementation = CalendarItemWithCalendarsDTO::class))]),
            ApiResponse(responseCode = "400", description = "Bad calendar item format", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun createCalendarItem(@Valid @RequestBody request: CreateOrUpdateCalendarItemRequest): ResponseEntity<CalendarItemWithCalendarsDTO> {
        return ResponseEntity(calendarService.createCalendarItem(request), HttpStatus.CREATED)
    }

    @PutMapping("/items/{id}", consumes = [APPLICATION_JSON_VALUE], produces = [APPLICATION_JSON_VALUE])
    @OnlyStaff
    @Operation(
        summary = "Update an existing calendar item",
        description = "Updates a calendar item, identified with the given id, with the provided request body and returns it.",
        responses = [
            ApiResponse(responseCode = "200", description = "Calendar updated", content = [Content(schema = Schema(implementation = CalendarItemWithCalendarsDTO::class))]),
            ApiResponse(responseCode = "400", description = "Bad calendar item format", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "404", description = "Invalid id", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun updateCalendarItem(@PathVariable id: Int, @Valid @RequestBody request: CreateOrUpdateCalendarItemRequest): CalendarItemWithCalendarsDTO {
        return calendarService.updateCalendarItem(id, request)
    }

    @DeleteMapping("/items/{id}", produces = [APPLICATION_JSON_VALUE])
    @OnlyStaff
    @Operation(
        summary = "Delete an existing calendar item",
        description = "Deletes a calendar item, identified with the given id. The linked image is also deleted.",
        responses = [
            ApiResponse(responseCode = "200", description = "Calendar item deleted"),
            ApiResponse(responseCode = "404", description = "Invalid id", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "500", description = "Image delete error", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun deleteCalendarItem(@PathVariable id: Int) {
        calendarService.deleteCalendarItem(id)
    }
}