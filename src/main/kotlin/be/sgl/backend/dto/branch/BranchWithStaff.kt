package be.sgl.backend.dto.branch

import be.sgl.backend.dto.user.StaffDTO
import be.sgl.backend.entity.branch.Branch
import be.sgl.backend.entity.branch.BranchStatus
import be.sgl.backend.shared.Sex

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
) {
    companion object {
        fun of(branch: Branch): BranchWithStaff {
            return BranchWithStaff(branch.id!!, branch.name, branch.image, branch.email, branch.minimumAge,
                branch.maximumAge ?: 0, branch.sex, branch.description, branch.law, branch.status, branch.staffTitle,
                branch.staff.map { StaffDTO.of(it, branch) })
        }
    }
}
