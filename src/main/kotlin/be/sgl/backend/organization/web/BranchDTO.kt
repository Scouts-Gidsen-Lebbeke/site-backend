package be.sgl.backend.organization.web

import be.sgl.backend.organization.domain.Branch
import be.sgl.backend.organization.domain.BranchStatus
import be.sgl.backend.shared.Sex
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
) {
    companion object {
        fun of(branch: Branch): BranchDTO {
            return BranchDTO(branch.id!!, branch.name, branch.image, branch.email, branch.minimumAge,
                branch.maximumAge ?: 0, branch.sex, branch.description, branch.law, branch.status, branch.staffTitle)
        }
    }
}
