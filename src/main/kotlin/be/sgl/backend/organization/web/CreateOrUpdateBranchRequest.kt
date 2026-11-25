package be.sgl.backend.organization.web

import be.sgl.backend.organization.domain.Branch
import be.sgl.backend.organization.domain.BranchStatus
import be.sgl.backend.shared.Sex
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
) {
    fun toEntity() = Branch().also {
        it.name = name!!
        it.email = email!!
        minimumAge?.let { age -> it.minimumAge = age }
        it.maximumAge = maximumAge
        it.sex = sex
        it.description = description
        it.law = law
        it.image = image!!
        it.status = status!!
        it.staffTitle = staffTitle
    }
}
