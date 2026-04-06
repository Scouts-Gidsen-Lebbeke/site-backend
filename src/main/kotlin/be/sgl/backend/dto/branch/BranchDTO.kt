package be.sgl.backend.dto.branch

import be.sgl.backend.entity.branch.BranchStatus
import be.sgl.backend.entity.user.Sex
import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "Branch")
data class BranchDTO(
    val id: Int,
    val name: String,
    val image: String,
    val email: String,
    val minimumAge: Int,
    val maximumAge: Int,
    val sex: Sex?,
    val description: String?,
    val law: String?,
    val status: BranchStatus,
    val staffTitle: String?
)