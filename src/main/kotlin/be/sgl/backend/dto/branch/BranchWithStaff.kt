package be.sgl.backend.dto.branch

import be.sgl.backend.dto.user.StaffDTO
import be.sgl.backend.entity.branch.BranchStatus
import be.sgl.backend.entity.user.Sex

data class BranchWithStaff(
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
    val staffTitle: String?,
    val staff: List<StaffDTO>
)