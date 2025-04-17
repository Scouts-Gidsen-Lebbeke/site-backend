package be.sgl.backend.dto

import be.sgl.backend.entity.branch.BranchStatus
import be.sgl.backend.entity.user.Sex
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.io.Serializable

open class BranchBaseDTO(
    val id: Int?,
    @NotBlank
    val name: String,
    @NotBlank
    val image: String
) : Serializable

class BranchDTO(
    id: Int?,
    name: String,
    image: String,
    @NotNull
    @Email
    val email: String,
    @NotNull
    @Positive
    val minimumAge: Int,
    @Positive
    val maximumAge: Int?,
    val sex: Sex?,
    val description: String?,
    val law: String?,
    val status: BranchStatus,
    val staffTitle: String?,
    val staff: List<StaffDTO> = emptyList()
) : BranchBaseDTO(id, name, image)