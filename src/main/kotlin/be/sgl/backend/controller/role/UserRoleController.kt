package be.sgl.backend.controller.role

import be.sgl.backend.config.CustomUserDetails
import be.sgl.backend.config.security.OnlyAdmin
import be.sgl.backend.config.security.OnlyAuthenticated
import be.sgl.backend.dto.branch.BranchDTO
import be.sgl.backend.dto.role.UserRoleDTO
import be.sgl.backend.dto.role.StaffLinkRequest
import be.sgl.backend.mapper.branch.BranchMapper
import be.sgl.backend.service.role.FindStaffBranchForUser
import be.sgl.backend.service.role.RoleService
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
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@Validated
@Controller
@RequestMapping("/user-roles")
@Tag(name = "User roles", description = "Endpoints for managing user roles.")
class UserRoleController(
    private val roleService: RoleService,
    private val findStaffBranchForUser: FindStaffBranchForUser,
    private val branchMapper: BranchMapper
) {

    @GetMapping("/role/{roleId}", produces = [APPLICATION_JSON_VALUE])
    @OnlyAdmin
    @Operation(
        summary = "Get all user roles for the given role",
        description = "Returns a list of all user roles, filtered by the given role.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(array = ArraySchema(schema = Schema(implementation = UserRoleDTO::class)))])
        ]
    )
    fun getUserRolesByRole(@PathVariable roleId: Int): ResponseEntity<List<UserRoleDTO>> {
        return ResponseEntity.ok(roleService.getUserRolesByRole(roleId))
    }

    @PostMapping("/staff", consumes = [APPLICATION_JSON_VALUE], produces = [APPLICATION_JSON_VALUE])
    @OnlyAdmin
    @Operation(
        summary = "Mark the user as staff of the given branch",
        description = "Links a staff role linked to the branch with provided id to the requested user.",
        responses = [
            ApiResponse(responseCode = "201", description = "User role created", content = [Content(schema = Schema(implementation = UserRoleDTO::class))]),
            ApiResponse(responseCode = "400", description = "User role already exists or invalid branch", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "404", description = "User or branch not found", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun markUserAsStaff(@Valid @RequestBody link: StaffLinkRequest): ResponseEntity<UserRoleDTO> {
        return ResponseEntity(roleService.markUserAsStaff(link.branchId!!, link.username!!), HttpStatus.CREATED)
    }

    @GetMapping("/staff-branch", produces = [APPLICATION_JSON_VALUE])
    @OnlyAuthenticated
    @Operation(
        summary = "Get the current staff branch for the given user",
        description = "Returns the (unique) active branch linked to a staff role for the user identified with the given username.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(schema = Schema(implementation = BranchDTO::class))]),
            ApiResponse(responseCode = "204", description = "Not found")
        ]
    )
    fun getStaffBranchForCurrentUser(@AuthenticationPrincipal userDetails: CustomUserDetails): ResponseEntity<BranchDTO> {
        val staffBranch = findStaffBranchForUser.execute(userDetails.username)
        return staffBranch?.let { ResponseEntity.ok(branchMapper.toDto(it)) } ?: ResponseEntity.noContent().build()
    }

    @PostMapping("/admin", consumes = [APPLICATION_JSON_VALUE], produces = [APPLICATION_JSON_VALUE])
    @OnlyAdmin
    @Operation(
        summary = "Mark the user as admin",
        description = "Links an admin role to the requested user.",
        responses = [
            ApiResponse(responseCode = "201", description = "User role created", content = [Content(schema = Schema(implementation = UserRoleDTO::class))]),
            ApiResponse(responseCode = "400", description = "User role already exists", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "404", description = "User not found", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "500", description = "No admin role configured", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun markUserAsAdmin(@Valid @RequestBody username: String): ResponseEntity<UserRoleDTO> {
        return ResponseEntity(roleService.markUserAsAdmin(username), HttpStatus.CREATED)
    }

    @DeleteMapping("/{id}", produces = [APPLICATION_JSON_VALUE])
    @OnlyAdmin
    @Operation(
        summary = "Delete a user role",
        description = "Deletes the user role, identified with the provided id. Only admin and staff roles can be manually unlinked.",
        responses = [
            ApiResponse(responseCode = "200", description = "User role deleted"),
            ApiResponse(responseCode = "400", description = "Invalid role or bad deassign request", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "404", description = "User role not found", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun deassignRoleFromUser(@PathVariable id: Int) {
        roleService.deassignRoleFromUser(id)
    }
}
