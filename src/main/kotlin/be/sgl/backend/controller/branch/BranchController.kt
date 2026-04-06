package be.sgl.backend.controller.branch

import be.sgl.backend.config.security.OnlyAdmin
import be.sgl.backend.config.security.OnlyStaff
import be.sgl.backend.config.security.Public
import be.sgl.backend.dto.branch.BranchDTO
import be.sgl.backend.dto.branch.BranchWithStaff
import be.sgl.backend.dto.branch.CreateOrUpdateBranchRequest
import be.sgl.backend.service.branch.BranchService
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
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/branches")
@Tag(name = "Branches", description = "Endpoints for managing branches.")
class BranchController(
    private val branchService: BranchService
) {

    @GetMapping(produces = [APPLICATION_JSON_VALUE])
    @OnlyAdmin
    @Operation(
        summary = "Get all branches",
        description = "Returns a list of all branches, regardless of their state.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(array = ArraySchema(schema = Schema(implementation = BranchDTO::class)))])
        ]
    )
    fun getAllBranches(): List<BranchDTO> {
        return branchService.getAllBranches()
    }

    @GetMapping("/visible", produces = [APPLICATION_JSON_VALUE])
    @Public
    @Operation(
        summary = "Get all visible branches",
        description = "Returns a list of all branches that don't have state 'PASSIVE', and thus should be visible for everyone.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(array = ArraySchema(schema = Schema(implementation = BranchDTO::class)))])
        ]
    )
    fun getVisibleBranches(): List<BranchDTO> {
        return branchService.getVisibleBranches()
    }

    @GetMapping("/with-calendar", produces = [APPLICATION_JSON_VALUE])
    @Public
    @Operation(
        summary = "Get all branches with an activity calendar",
        description = "Returns a list of all branches that have state 'ACTIVE', and thus should be visible in the calendar listing.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(array = ArraySchema(schema = Schema(implementation = BranchDTO::class)))])
        ]
    )
    fun getBranchesWithCalendar(): List<BranchDTO> {
        return branchService.getBranchesWithCalendar()
    }

    @GetMapping("/{id}", produces = [APPLICATION_JSON_VALUE])
    @Public
    @Operation(
        summary = "Get a specific branch",
        description = "Returns the branch with the given id, regardless of its state.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(schema = Schema(implementation = BranchWithStaff::class))]),
            ApiResponse(responseCode = "404", description = "Invalid id", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun getBranch(@PathVariable id: Int): BranchWithStaff {
        return branchService.getBranchDTOById(id)
    }

    @PostMapping(consumes = [APPLICATION_JSON_VALUE], produces = [APPLICATION_JSON_VALUE])
    @OnlyStaff
    @Operation(
        summary = "Create a branch",
        description = "Creates a branch with the provided request body and returns it.",
        responses = [
            ApiResponse(responseCode = "201", description = "Branch created", content = [Content(schema = Schema(implementation = BranchDTO::class))]),
            ApiResponse(responseCode = "400", description = "Bad branch format", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun createBranch(@Valid @RequestBody request: CreateOrUpdateBranchRequest): ResponseEntity<BranchDTO> {
        return ResponseEntity(branchService.createBranch(request), HttpStatus.CREATED)
    }

    @PutMapping("/{id}", consumes = [APPLICATION_JSON_VALUE], produces = [APPLICATION_JSON_VALUE])
    @OnlyStaff
    @Operation(
        summary = "Update an existing branch",
        description = "Updates a branch, identified with the given id, with the provided request body and returns it.",
        responses = [
            ApiResponse(responseCode = "200", description = "Branch updated", content = [Content(schema = Schema(implementation = BranchDTO::class))]),
            ApiResponse(responseCode = "400", description = "Bad branch format", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "404", description = "Invalid id", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun updateBranch(@PathVariable id: Int, @Valid @RequestBody request: CreateOrUpdateBranchRequest): BranchDTO {
        return branchService.updateBranch(id, request)
    }
}