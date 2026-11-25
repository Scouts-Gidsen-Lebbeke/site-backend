package be.sgl.backend.controller.role

import be.sgl.backend.dto.role.ExternalFunction
import be.sgl.backend.service.organization.FindExternalFunctions
import be.sgl.backend.shared.security.OnlyAdmin
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
@RequestMapping("/roles")
@Tag(name = "Roles", description = "Endpoints for managing roles.")
class ExternalFunctionController(
    private val findExternalFunctions: FindExternalFunctions
) {

    @GetMapping("/functions", produces = [APPLICATION_JSON_VALUE])
    @OnlyAdmin
    @Operation(
        summary = "Get all external functions",
        description = "Returns a list of all external functions, regardless if they are paid.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(array = ArraySchema(schema = Schema(implementation = ExternalFunction::class)))])
        ]
    )
    fun getAllExternalFunctions(): List<ExternalFunction> {
        return findExternalFunctions.execute(false)
    }

    @GetMapping("/paid-functions", produces = [APPLICATION_JSON_VALUE])
    @OnlyAdmin
    @Operation(
        summary = "Get all paid external functions",
        description = "Returns a list of all paid external functions.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(array = ArraySchema(schema = Schema(implementation = ExternalFunction::class)))])
        ]
    )
    fun getPaidExternalFunctions(): List<ExternalFunction> {
        return findExternalFunctions.execute(true)
    }
}
