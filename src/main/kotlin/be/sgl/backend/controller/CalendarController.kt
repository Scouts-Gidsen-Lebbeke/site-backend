package be.sgl.backend.controller

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/calendars")
@Tag(name = "Calendars", description = "Endpoints for managing calendars.")
class CalendarController {
}