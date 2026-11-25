package be.sgl.backend.controller.branch

import be.sgl.backend.dto.branch.BranchWithStaff
import be.sgl.backend.service.branch.GetBranchWithStaff
import be.sgl.backend.shared.security.Public
import io.github.wimdeblauwe.errorhandlingspringbootstarter.ApiErrorResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/branches")
@Tag(name = "Branches", description = "Endpoints for managing branches.")
class BranchStaffController(
    private val getBranchWithStaff: GetBranchWithStaff
) {

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
        return getBranchWithStaff.execute(id)
    }
}
