package be.sgl.backend.dto.role

import jakarta.validation.constraints.NotBlank

data class MemberRoleChangeRequest(
    @field:NotBlank(message = "{NotBlank.memberRoleChangeRequest.externalId}")
    var externalId: String?,
    var backupExternalId: String?,
    @field:NotBlank(message = "{NotBlank.memberRoleChangeRequest.name}")
    var name: String?,
)