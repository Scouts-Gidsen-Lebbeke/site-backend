package be.sgl.backend.controller

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/events")
@Tag(name = "Events", description = "Endpoints for managing events.")
class EventController {
}