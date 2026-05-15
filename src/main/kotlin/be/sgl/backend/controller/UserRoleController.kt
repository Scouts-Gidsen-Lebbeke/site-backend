package be.sgl.backend.controller

import be.sgl.backend.config.security.OnlyAdmin
import be.sgl.backend.dto.UserRoleDTO
import be.sgl.backend.dto.StaffLinkDTO
import be.sgl.backend.service.RoleService
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
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*

@Controller
@RequestMapping("/user-roles")
@Tag(name = "User roles", description = "Endpoints for managing user roles.")
class UserRoleController {

    @Autowired
    private lateinit var roleService: RoleService

    @GetMapping("/role/{roleId}")
    @OnlyAdmin
    @Operation(
        summary = "Get all user roles for the given role",
        description = "Returns a list of all user roles, filtered by the given role.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(type = "array", implementation = UserRoleDTO::class))])
        ]
    )
    fun getUserRolesByRole(@PathVariable roleId: Int): ResponseEntity<List<UserRoleDTO>> {
        return ResponseEntity.ok(roleService.getUserRolesByRole(roleId))
    }

    @PostMapping("/staff")
    @OnlyAdmin
    @Operation(
        summary = "Mark the user as staff of the given branch",
        description = "Links a staff role linked to the branch with provided id to the requested user.",
        responses = [
            ApiResponse(responseCode = "201", description = "User role created", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = UserRoleDTO::class))]),
            ApiResponse(responseCode = "400", description = "User role already exists or invalid branch", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "404", description = "User or branch not found", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun markUserAsStaff(@Valid @RequestBody link: StaffLinkDTO): ResponseEntity<UserRoleDTO> {
        return ResponseEntity(roleService.markUserAsStaff(link.branchId!!, link.username!!), HttpStatus.CREATED)
    }

    @PostMapping("/admin")
    @OnlyAdmin
    @Operation(
        summary = "Mark the user as admin",
        description = "Links an admin role to the requested user.",
        responses = [
            ApiResponse(responseCode = "201", description = "User role created", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = UserRoleDTO::class))]),
            ApiResponse(responseCode = "400", description = "User role already exists", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "404", description = "User not found", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "500", description = "No admin role configured", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun markUserAsAdmin(@RequestBody username: String): ResponseEntity<UserRoleDTO> {
        return ResponseEntity(roleService.markUserAsAdmin(username), HttpStatus.CREATED)
    }

    @DeleteMapping("/{id}")
    @OnlyAdmin
    @Operation(
        summary = "Delete a user role",
        description = "Deletes the user role, identified with the provided id. Only admin and staff roles can be manually unlinked.",
        responses = [
            ApiResponse(responseCode = "200", description = "User role deleted"),
            ApiResponse(responseCode = "400", description = "Invalid role or bad deassign request", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "404", description = "User role not found", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun deassignRoleFromUser(@PathVariable id: Int): ResponseEntity<Unit> {
        roleService.deassignRoleFromUser(id)
        return ResponseEntity.ok().build()
    }
}
