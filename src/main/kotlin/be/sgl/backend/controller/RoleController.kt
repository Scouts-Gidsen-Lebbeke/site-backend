package be.sgl.backend.controller

import be.sgl.backend.config.security.OnlyAdmin
import be.sgl.backend.config.security.Public
import be.sgl.backend.dto.MemberRoleDTO
import be.sgl.backend.dto.StaffRoleDTO
import be.sgl.backend.dto.ExternalFunction
import be.sgl.backend.dto.RoleDTO
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
@RequestMapping("/roles")
@Tag(name = "Roles", description = "Endpoints for managing roles.")
class RoleController {

    @Autowired
    private lateinit var roleService: RoleService

    @GetMapping
    @OnlyAdmin
    @Operation(
        summary = "Get all roles",
        description = "Returns a list of all roles.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(type = "array", implementation = RoleDTO::class))])
        ]
    )
    fun getAllRoles(): ResponseEntity<List<RoleDTO>> {
        return ResponseEntity.ok(roleService.getAllRoles())
    }

    @GetMapping("/admin")
    @Public
    @Operation(
        summary = "Get the admin role",
        description = "Returns the single admin role.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = RoleDTO::class))]),
            ApiResponse(responseCode = "500", description = "No admin role configured", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun getAdminRole(): ResponseEntity<RoleDTO> {
        return ResponseEntity.ok(roleService.getAdminRole())
    }

    @GetMapping("/functions")
    @OnlyAdmin
    @Operation(
        summary = "Get all external functions",
        description = "Returns a list of all external functions, regardless if they are paid.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(type = "array", implementation = ExternalFunction::class))])
        ]
    )
    fun getAllExternalFunctions(): ResponseEntity<List<ExternalFunction>> {
        return ResponseEntity.ok(roleService.getAllExternalFunctions())
    }

    @GetMapping("/paid-functions")
    @OnlyAdmin
    @Operation(
        summary = "Get all paid external functions",
        description = "Returns a list of all paid external functions.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(type = "array", implementation = ExternalFunction::class))])
        ]
    )
    fun getPaidExternalFunctions(): ResponseEntity<List<ExternalFunction>> {
        return ResponseEntity.ok(roleService.getPaidExternalFunctions())
    }

    @GetMapping("/branch/{branchId}")
    @Public
    @Operation(
        summary = "Get the role linked to the specified branch",
        description = "Returns the single role that is assigned when a user engages in a membership for the given branch, if one.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = RoleDTO::class))]),
            ApiResponse(responseCode = "204", description = "Not found"),
            ApiResponse(responseCode = "404", description = "Invalid id", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun getRoleToSyncByBranch(@PathVariable branchId: Int): ResponseEntity<RoleDTO?> {
        val memberRole = roleService.getRoleToSyncByBranch(branchId)
        return memberRole?.let { ResponseEntity.ok(it) } ?: ResponseEntity.noContent().build()
    }

    @GetMapping("/staff-branch/{branchId}")
    @Public
    @Operation(
        summary = "Get the role linked to the specified staff branch",
        description = "Returns the single staff role that is assigned when a user is marked as staff of the given branch, if one.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = RoleDTO::class))]),
            ApiResponse(responseCode = "204", description = "Not found"),
            ApiResponse(responseCode = "404", description = "Invalid id", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun getStaffRoleToSyncByBranch(@PathVariable branchId: Int): ResponseEntity<RoleDTO?> {
        val staffRole = roleService.getStaffRoleToSyncByBranch(branchId)
        return staffRole?.let { ResponseEntity.ok(it) } ?: ResponseEntity.noContent().build()
    }

    @PostMapping("/branch/{branchId}")
    @OnlyAdmin
    @Operation(
        summary = "Create a member role",
        description = "Creates a member role with the provided request body and returns it.",
        responses = [
            ApiResponse(responseCode = "201", description = "Role created", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = RoleDTO::class))]),
            ApiResponse(responseCode = "400", description = "Bad role format", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "404", description = "Invalid id", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))])

        ]
    )
    fun createMemberRole(@PathVariable branchId: Int, @Valid @RequestBody roleDto: MemberRoleDTO): ResponseEntity<RoleDTO> {
        return ResponseEntity(roleService.createMemberRole(branchId, roleDto), HttpStatus.CREATED)
    }

    @PostMapping("/staff-branch/{branchId}")
    @OnlyAdmin
    @Operation(
        summary = "Create a staff role",
        description = "Creates a staff role with the provided request body and returns it.",
        responses = [
            ApiResponse(responseCode = "201", description = "Role created", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = RoleDTO::class))]),
            ApiResponse(responseCode = "400", description = "Bad role format", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "404", description = "Invalid id", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun createStaffRole(@PathVariable branchId: Int, @Valid @RequestBody roleDto: StaffRoleDTO): ResponseEntity<RoleDTO> {
        return ResponseEntity(roleService.createStaffRole(branchId, roleDto), HttpStatus.CREATED)
    }

    @PutMapping("/branch/{id}")
    @OnlyAdmin
    @Operation(
        summary = "Update an existing member role",
        description = "Updates a member role, identified with the given id, with the provided request body and returns it.",
        responses = [
            ApiResponse(responseCode = "200", description = "Role updated", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = RoleDTO::class))]),
            ApiResponse(responseCode = "400", description = "Bad role format", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "404", description = "Invalid id", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun updateMemberRole(@PathVariable id: Int, @Valid @RequestBody roleDTO: MemberRoleDTO): ResponseEntity<RoleDTO> {
        return ResponseEntity.ok(roleService.mergeMemberRoleDTOChanges(id, roleDTO))
    }

    @PutMapping("/staff-branch/{id}")
    @OnlyAdmin
    @Operation(
        summary = "Update an existing staff role",
        description = "Updates a staff role, identified with the given id, with the provided request body and returns it.",
        responses = [
            ApiResponse(responseCode = "200", description = "Role updated", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = RoleDTO::class))]),
            ApiResponse(responseCode = "400", description = "Bad role format", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "404", description = "Invalid id", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun updateStaffRole(@PathVariable id: Int, @Valid @RequestBody roleDTO: StaffRoleDTO): ResponseEntity<RoleDTO> {
        return ResponseEntity.ok(roleService.mergeStaffRoleDTOChanges(id, roleDTO))
    }

    @DeleteMapping("/{id}")
    @OnlyAdmin
    @Operation(
        summary = "Delete an existing role",
        description = "Deletes a role, identified with the given id. All linked user roles are also deleted.",
        responses = [
            ApiResponse(responseCode = "200", description = "Role deleted"),
            ApiResponse(responseCode = "404", description = "Invalid id", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun deleteRole(@PathVariable id: Int): ResponseEntity<Unit> {
        roleService.deleteRole(id)
        return ResponseEntity.ok().build()
    }
}
