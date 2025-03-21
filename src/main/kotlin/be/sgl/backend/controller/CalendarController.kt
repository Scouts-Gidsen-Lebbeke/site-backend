package be.sgl.backend.controller

import be.sgl.backend.config.security.OnlyAdmin
import be.sgl.backend.config.security.OnlyStaff
import be.sgl.backend.dto.CalendarDTO
import be.sgl.backend.dto.CalendarItemWithCalendarsDTO
import be.sgl.backend.dto.CalendarPeriodDTO
import be.sgl.backend.service.CalendarService
import io.github.wimdeblauwe.errorhandlingspringbootstarter.ApiErrorResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/calendars")
@Tag(name = "Calendars", description = "Endpoints for managing calendar periods, calendars and their items.")
class CalendarController {

    @Autowired
    private lateinit var calendarService: CalendarService

    @GetMapping("/periods")
    @Operation(
        summary = "Get all calendar periods",
        description = "Returns a list of all periods.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(type = "array", implementation = CalendarPeriodDTO::class))])
        ]
    )
    fun getAllPeriods(): ResponseEntity<List<CalendarPeriodDTO>> {
        return ResponseEntity.ok(calendarService.getAllCalendarPeriods())
    }

    @PostMapping("/periods")
    @OnlyAdmin
    @Operation(
        summary = "Create a calendar period",
        description = "Creates a calendar period with the provided request body, together with a calendar for all visible branches, and returns it.",
        responses = [
            ApiResponse(responseCode = "201", description = "Calendar period created", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = CalendarPeriodDTO::class))]),
            ApiResponse(responseCode = "400", description = "Bad calendar period format", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun createCalendarPeriod(@Valid @RequestBody calendarPeriodDTO: CalendarPeriodDTO): ResponseEntity<CalendarPeriodDTO> {
        return ResponseEntity(calendarService.saveCalendarPeriodDTO(calendarPeriodDTO), HttpStatus.CREATED)
    }

    @PutMapping("/periods/{id}")
    @OnlyAdmin
    @Operation(
        summary = "Update an existing calendar period",
        description = "Updates a calendar period, identified with the given id, with the provided request body and returns it.",
        responses = [
            ApiResponse(responseCode = "200", description = "Calendar period updated", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = CalendarPeriodDTO::class))]),
            ApiResponse(responseCode = "400", description = "Bad calendar period format", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "404", description = "Invalid id", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun updateCalendarPeriod(@PathVariable id: Int, @Valid @RequestBody calendarPeriodDTO: CalendarPeriodDTO): ResponseEntity<CalendarPeriodDTO> {
        return ResponseEntity.ok(calendarService.mergeCalendarPeriodDTOChanges(id, calendarPeriodDTO))
    }

    @DeleteMapping("/periods/{id}")
    @OnlyAdmin
    @Operation(
        summary = "Delete an existing calendar period",
        description = "Deletes a calendar period, identified with the given id. All linked calendars, its items and images are also deleted.",
        responses = [
            ApiResponse(responseCode = "200", description = "Calendar period deleted"),
            ApiResponse(responseCode = "404", description = "Invalid id", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "500", description = "Image delete error", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun deleteCalendarPeriod(@PathVariable id: Int): ResponseEntity<Unit> {
        calendarService.deleteCalendarPeriod(id)
        return ResponseEntity.ok().build()
    }

    @GetMapping("/current")
    @Operation(
        summary = "Get all current calendars",
        description = "Returns a list of all calendars where the current date lays between its start and end date.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(type = "array", implementation = CalendarDTO::class))]),
        ]
    )
    fun getCurrentCalendars(): ResponseEntity<List<CalendarDTO>> {
        return ResponseEntity.ok(calendarService.getCurrentCalendars())
    }

    @GetMapping("/period/{id}")
    @OnlyStaff
    @Operation(
        summary = "Get all current calendars",
        description = "Returns a list of all calendars where the current date lays between its start and end date.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(type = "array", implementation = CalendarDTO::class))]),
            ApiResponse(responseCode = "404", description = "Invalid id", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun getCalendarsByPeriod(@PathVariable id: Int): ResponseEntity<List<CalendarDTO>> {
        return ResponseEntity.ok(calendarService.getCalendarsByPeriod(id))
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Get a specific calendar",
        description = "Returns the calendar with the given id.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(type = "array", implementation = CalendarDTO::class))]),
            ApiResponse(responseCode = "404", description = "Invalid id", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun getCalendar(@PathVariable id: Int, @RequestParam(required = false, defaultValue = "false") withDefaults: Boolean): ResponseEntity<CalendarDTO> {
        return ResponseEntity.ok(calendarService.getCalendarDTOById(id, withDefaults))
    }

    @PutMapping("/{id}")
    @OnlyStaff
    @Operation(
        summary = "Update an existing calendar",
        description = "Updates a calendar, identified with the given id, with the provided request body and returns it.",
        responses = [
            ApiResponse(responseCode = "200", description = "Calendar updated", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = CalendarDTO::class))]),
            ApiResponse(responseCode = "404", description = "Invalid id", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun updateCalendar(@PathVariable id: Int, @Valid @RequestBody calendarDTO: CalendarDTO): ResponseEntity<CalendarDTO> {
        return ResponseEntity.ok(calendarService.mergeCalendarDTOChanges(id, calendarDTO))
    }

    @GetMapping("/items/{id}")
    @OnlyStaff
    @Operation(
        summary = "Get a specific calendar item",
        description = "Returns the calendar item with the given id.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(type = "array", implementation = CalendarItemWithCalendarsDTO::class))]),
            ApiResponse(responseCode = "404", description = "Invalid id", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun getCalendarItem(@PathVariable id: Int): ResponseEntity<CalendarItemWithCalendarsDTO> {
        return ResponseEntity.ok(calendarService.getCalendarItemDTOById(id))
    }

    @PostMapping("/items")
    @OnlyStaff
    @Operation(
        summary = "Create a calendar item",
        description = "Creates a calendar item with the provided request body, together with a calendar for all visible branches, and returns it.",
        responses = [
            ApiResponse(responseCode = "201", description = "Calendar item created", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = CalendarItemWithCalendarsDTO::class))]),
            ApiResponse(responseCode = "400", description = "Bad calendar item format", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun createCalendarItem(@Valid @RequestBody calendarItemDTO: CalendarItemWithCalendarsDTO): ResponseEntity<CalendarItemWithCalendarsDTO> {
        return ResponseEntity(calendarService.saveCalendarItemDTO(calendarItemDTO), HttpStatus.CREATED)
    }

    @PutMapping("/items/{id}")
    @OnlyStaff
    @Operation(
        summary = "Update an existing calendar item",
        description = "Updates a calendar item, identified with the given id, with the provided request body and returns it.",
        responses = [
            ApiResponse(responseCode = "200", description = "Calendar updated", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = CalendarItemWithCalendarsDTO::class))]),
            ApiResponse(responseCode = "400", description = "Bad calendar item format", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "404", description = "Invalid id", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun updateCalendarItem(@PathVariable id: Int, @Valid @RequestBody calendarItemDTO: CalendarItemWithCalendarsDTO): ResponseEntity<CalendarItemWithCalendarsDTO> {
        return ResponseEntity.ok(calendarService.mergeCalendarItemDTOChanges(id, calendarItemDTO))
    }

    @DeleteMapping("/items/{id}")
    @OnlyStaff
    @Operation(
        summary = "Delete an existing calendar item",
        description = "Deletes a calendar item, identified with the given id. The linked image is also deleted.",
        responses = [
            ApiResponse(responseCode = "200", description = "Calendar item deleted"),
            ApiResponse(responseCode = "404", description = "Invalid id", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "500", description = "Image delete error", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun deleteCalendarItem(@PathVariable id: Int): ResponseEntity<Unit> {
        calendarService.deleteCalendarItem(id)
        return ResponseEntity.ok().build()
    }
}