package be.sgl.backend.controller

import be.sgl.backend.config.security.OnlyAdmin
import be.sgl.backend.dto.ExternalFunction
import be.sgl.backend.dto.RoleDTO
import be.sgl.backend.service.RoleService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

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
}
