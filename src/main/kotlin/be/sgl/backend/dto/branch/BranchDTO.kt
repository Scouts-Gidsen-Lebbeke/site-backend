package be.sgl.backend.dto.branch

import be.sgl.backend.dto.user.StaffDTO
import be.sgl.backend.entity.branch.BranchStatus
import be.sgl.backend.entity.user.Sex
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.io.Serializable

open class BranchBaseDTO(
    val id: Int?,
    @field:NotBlank
    val name: String?,
    @field:NotBlank
    val image: String?
) : Serializable

class BranchDTO(
    id: Int?,
    name: String?,
    image: String?,
    @field:NotNull
    @field:Email
    val email: String?,
    @field:NotNull
    @field:Positive
    val minimumAge: Int?,
    @field:Positive
    val maximumAge: Int?,
    val sex: Sex?,
    val description: String?,
    val law: String?,
    @field:NotNull
    val status: BranchStatus?,
    val staffTitle: String?,
    val staff: List<StaffDTO> = emptyList()
) : BranchBaseDTO(id, name, image)