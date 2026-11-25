package be.sgl.backend.organization.web

import jakarta.validation.constraints.NotBlank

data class StaffRoleChangeRequest(
    var externalId: String?,
    var backupExternalId: String?,
    @field:NotBlank(message = "{NotBlank.staffRoleChangeRequest.name}")
    var name: String?,
    var staffLevel: Boolean = false
)