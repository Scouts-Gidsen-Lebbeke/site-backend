package be.sgl.backend.controller

import be.sgl.backend.dto.OrganizationDTO
import be.sgl.backend.service.organization.OrganizationService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/organizations")
@Tag(name = "Organization", description = "Endpoints for managing organizations.")
class OrganizationController {

    @Autowired
    private lateinit var organizationService: OrganizationService

    @GetMapping("/owner")
    @Operation(
        summary = "Get the owning organization",
        description = "Returns the single organization behind this website.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(mediaType = "application/json", schema = Schema(implementation = OrganizationDTO::class))]),
            ApiResponse(responseCode = "409", description = "Not configured", content = [Content(schema = Schema(hidden = true))])
        ]
    )
    fun getOwner(): ResponseEntity<OrganizationDTO> {
        return ResponseEntity.ok(organizationService.getOwner())
    }
}