package be.sgl.backend.dto.role

import jakarta.validation.constraints.NotNull

data class StaffLinkRequest(
    @field:NotNull(message = "{NotNull.staffLinkRequest.username}")
    var username: String?,
    @field:NotNull(message = "{NotNull.staffLinkRequest.branchId}")
    var branchId: Int?
)