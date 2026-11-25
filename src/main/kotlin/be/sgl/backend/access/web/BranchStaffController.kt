package be.sgl.backend.access.web

import be.sgl.backend.access.AccessApi
import be.sgl.backend.organization.BranchApi
import be.sgl.backend.people.UserApi
import be.sgl.backend.setting.SettingApi
import be.sgl.backend.setting.SettingId
import be.sgl.backend.shared.exception.BranchNotFoundException
import be.sgl.backend.shared.security.Public
import io.github.wimdeblauwe.errorhandlingspringbootstarter.ApiErrorResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
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
    private val branchApi: BranchApi,
    private val accessApi: AccessApi,
    private val userApi: UserApi,
    private val settingApi: SettingApi
) {

    @GetMapping("/{id}/staff", produces = [APPLICATION_JSON_VALUE])
    @Public
    @Operation(
        summary = "Get the staff of a branch",
        description = "Returns the staff members of the branch with the given id, or an empty list when staff is not visible.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(array = ArraySchema(schema = Schema(implementation = StaffDTO::class)))]),
            ApiResponse(responseCode = "404", description = "Invalid id", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun getBranchStaff(@PathVariable id: Int): List<StaffDTO> {
        if (branchApi.findSummaries(listOf(id)).isEmpty()) {
            throw BranchNotFoundException()
        }
        if (!settingApi.getOrDefault(SettingId.STAFF_VISIBLE.name, true)) {
            return emptyList()
        }
        return userApi.findStaff(accessApi.staffUserIdsOf(id), id).map(StaffDTO::of)
    }
}
