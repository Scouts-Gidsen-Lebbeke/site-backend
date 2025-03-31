package be.sgl.backend.controller

import be.sgl.backend.config.security.OnlyAdmin
import be.sgl.backend.config.security.Public
import be.sgl.backend.dto.OrganizationDTO
import be.sgl.backend.service.organization.OrganizationService
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
@RequestMapping("/organizations")
@Tag(name = "Organization", description = "Endpoints for managing organizations.")
class OrganizationController {

    @Autowired
    private lateinit var organizationService: OrganizationService

    @GetMapping("/owner")
    @Public
    @Operation(
        summary = "Get the owning organization",
        description = "Returns the single organization behind this website.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = OrganizationDTO::class))]),
            ApiResponse(responseCode = "409", description = "Not configured", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun getOwner(): ResponseEntity<OrganizationDTO> {
        return ResponseEntity.ok(organizationService.getOwner())
    }

    @GetMapping("/certifier")
    @Public
    @Operation(
        summary = "Get the certifying organization",
        description = "Returns the certifying organization of the owner.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = OrganizationDTO::class))]),
            ApiResponse(responseCode = "409", description = "Not configured", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun getCertifier(): ResponseEntity<OrganizationDTO> {
        return ResponseEntity.ok(organizationService.getCertifier())
    }

    @PostMapping
    @OnlyAdmin
    @Operation(
        summary = "Create a new organization",
        description = "Creates an organization with the provided request body and returns it. Only one organization of each type can be created.",
        responses = [
            ApiResponse(responseCode = "201", description = "Organization created", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = OrganizationDTO::class))]),
            ApiResponse(responseCode = "400", description = "Bad organization format", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "500", description = "Image error", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun createOrganization(@Valid @RequestBody organization: OrganizationDTO): ResponseEntity<OrganizationDTO> {
        return ResponseEntity(organizationService.saveOrganizationDTO(organization), HttpStatus.CREATED)
    }

    @PutMapping("/{id}")
    @OnlyAdmin
    @Operation(
        summary = "Update an existing organization",
        description = "Updates an organization, identified with the given id, with the provided request body and returns it.",
        responses = [
            ApiResponse(responseCode = "200", description = "Organization updated", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = OrganizationDTO::class))]),
            ApiResponse(responseCode = "400", description = "Bad organization format", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "404", description = "Invalid id", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "500", description = "Image error", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun updateOrganization(@PathVariable id: Int, @Valid @RequestBody organization: OrganizationDTO): ResponseEntity<OrganizationDTO> {
        return ResponseEntity.ok(organizationService.mergeOrganizationDTOChanges(id, organization))
    }
}