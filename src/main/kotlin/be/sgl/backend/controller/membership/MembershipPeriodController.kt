package be.sgl.backend.controller.membership

import be.sgl.backend.config.security.OnlyAdmin
import be.sgl.backend.config.security.Public
import be.sgl.backend.dto.membership.CreateOrUpdateMembershipPeriodRequest
import be.sgl.backend.dto.membership.MembershipPeriodDTO
import be.sgl.backend.dto.membership.MembershipPeriodResult
import be.sgl.backend.service.membership.MembershipPeriodService
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
@RequestMapping("/memberships/periods")
@Tag(name = "Membership periods", description = "Endpoints for managing membership periods.")
class MembershipPeriodController(
    private val membershipPeriodService: MembershipPeriodService
) {

    @GetMapping("/periods", produces = [APPLICATION_JSON_VALUE])
    @Public
    @Operation(
        summary = "Get all membership periods",
        description = "Returns a list of all membership periods.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(array = ArraySchema(schema = Schema(implementation = MembershipPeriodResult::class)))])
        ]
    )
    fun getAllMembershipPeriods(): List<MembershipPeriodResult> {
        return membershipPeriodService.getAllMembershipPeriods()
    }

    @GetMapping("/periods/{id}", produces = [APPLICATION_JSON_VALUE])
    @Public
    @Operation(
        summary = "Get a specific membership period",
        description = "Returns the membership period with the given id.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(schema = Schema(implementation = MembershipPeriodDTO::class))]),
            ApiResponse(responseCode = "404", description = "Invalid id", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun getMembershipPeriod(@PathVariable id: Int): MembershipPeriodDTO {
        return membershipPeriodService.getMembershipPeriodDTOById(id)
    }

    @GetMapping("/periods/current", produces = [APPLICATION_JSON_VALUE])
    @Public
    @Operation(
        summary = "Get the current membership period",
        description = "Returns the currently active membership period.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(schema = Schema(implementation = MembershipPeriodDTO::class))])
        ]
    )
    fun getCurrentMembershipPeriod(): MembershipPeriodDTO {
        return membershipPeriodService.getCurrentMembershipPeriod()
    }

    @PostMapping("/periods", consumes = [APPLICATION_JSON_VALUE], produces = [APPLICATION_JSON_VALUE])
    @OnlyAdmin
    @Operation(
        summary = "Create a membership period",
        description = "Creates a membership period with the provided request body and returns it.",
        responses = [
            ApiResponse(responseCode = "201", description = "Membership period created", content = [Content(schema = Schema(implementation = MembershipPeriodDTO::class))]),
            ApiResponse(responseCode = "400", description = "Bad membership period format", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun createPeriod(@Valid @RequestBody request: CreateOrUpdateMembershipPeriodRequest): ResponseEntity<MembershipPeriodDTO> {
        return ResponseEntity(membershipPeriodService.createPeriod(request), HttpStatus.CREATED)
    }

    @PutMapping("/periods/{id}", consumes = [APPLICATION_JSON_VALUE], produces = [APPLICATION_JSON_VALUE])
    @OnlyAdmin
    @Operation(
        summary = "Update an existing membership period",
        description = "Updates a membership period, identified with the given id, with the provided request body and returns it. Only future periods can be edited.",
        responses = [
            ApiResponse(responseCode = "200", description = "Membership period updated", content = [Content(schema = Schema(implementation = MembershipPeriodDTO::class))]),
            ApiResponse(responseCode = "400", description = "Bad membership period format", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "404", description = "Invalid id", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun updatePeriod(@PathVariable id: Int, @Valid @RequestBody request: CreateOrUpdateMembershipPeriodRequest): MembershipPeriodDTO {
        return membershipPeriodService.updatePeriod(id, request)
    }
}