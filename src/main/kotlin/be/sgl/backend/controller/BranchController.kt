package be.sgl.backend.controller

import be.sgl.backend.config.security.OnlyAdmin
import be.sgl.backend.config.security.OnlyStaff
import be.sgl.backend.config.security.Public
import be.sgl.backend.dto.BranchBaseDTO
import be.sgl.backend.dto.BranchDTO
import be.sgl.backend.service.BranchService
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
@RequestMapping("/branches")
@Tag(name = "Branches", description = "Endpoints for managing branches.")
class BranchController {

    @Autowired
    lateinit var branchService: BranchService

    @GetMapping
    @OnlyAdmin
    @Operation(
        summary = "Get all branches",
        description = "Returns a list of all branches, regardless of their state.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(type = "array", implementation = BranchDTO::class))])
        ]
    )
    fun getAllBranches(): ResponseEntity<List<BranchDTO>> {
        return ResponseEntity.ok(branchService.getAllBranches())
    }

    @GetMapping("/visible")
    @Public
    @Operation(
        summary = "Get all visible branches",
        description = "Returns a list of all branches that don't have state 'PASSIVE', and thus should be visible for everyone.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(type = "array", implementation = BranchBaseDTO::class))])
        ]
    )
    fun getVisibleBranches(): ResponseEntity<List<BranchBaseDTO>> {
        return ResponseEntity.ok(branchService.getVisibleBranches())
    }

    @GetMapping("/with-calendar")
    @Public
    @Operation(
        summary = "Get all branches with an activity calendar",
        description = "Returns a list of all branches that have state 'ACTIVE', and thus should be visible in the calendar listing.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(type = "array", implementation = BranchBaseDTO::class))])
        ]
    )
    fun getBranchesWithCalendar(): ResponseEntity<List<BranchBaseDTO>> {
        return ResponseEntity.ok(branchService.getBranchesWithCalendar())
    }

    @GetMapping("/{id}")
    @Public
    @Operation(
        summary = "Get a specific branch",
        description = "Returns the branch with the given id, regardless of its state.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = BranchDTO::class))]),
            ApiResponse(responseCode = "404", description = "Invalid id", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun getBranch(@PathVariable id: Int): ResponseEntity<BranchDTO> {
        return ResponseEntity.ok(branchService.getBranchDTOById(id))
    }

    @PostMapping
    @OnlyStaff
    @Operation(
        summary = "Create a branch",
        description = "Creates a branch with the provided request body and returns it.",
        responses = [
            ApiResponse(responseCode = "201", description = "Branch created", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = BranchDTO::class))]),
            ApiResponse(responseCode = "400", description = "Bad branch format", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun createBranch(@Valid @RequestBody branch: BranchDTO): ResponseEntity<BranchDTO> {
        return ResponseEntity(branchService.saveBranchDTO(branch), HttpStatus.CREATED)
    }

    @PutMapping("/{id}")
    @OnlyStaff
    @Operation(
        summary = "Update an existing branch",
        description = "Updates a branch, identified with the given id, with the provided request body and returns it.",
        responses = [
            ApiResponse(responseCode = "200", description = "Branch updated", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = BranchDTO::class))]),
            ApiResponse(responseCode = "400", description = "Bad branch format", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "404", description = "Invalid id", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun updateBranch(@PathVariable id: Int, @Valid @RequestBody branch: BranchDTO): ResponseEntity<BranchDTO> {
        return ResponseEntity.ok(branchService.mergeBranchDTOChanges(id, branch))
    }
}