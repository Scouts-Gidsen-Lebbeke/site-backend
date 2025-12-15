package be.sgl.backend.dto.branch

import be.sgl.backend.entity.branch.BranchStatus
import be.sgl.backend.entity.user.Sex
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive

data class CreateOrUpdateBranchRequest(
    @field:NotBlank
    var name: String?,
    @field:NotBlank
    var image: String?,
    @field:NotNull
    @field:Email
    var email: String?,
    @field:NotNull
    @field:Positive
    var minimumAge: Int?,
    @field:Positive
    var maximumAge: Int?,
    var sex: Sex?,
    var description: String?,
    var law: String?,
    @field:NotNull
    var status: BranchStatus?,
    var staffTitle: String?
)