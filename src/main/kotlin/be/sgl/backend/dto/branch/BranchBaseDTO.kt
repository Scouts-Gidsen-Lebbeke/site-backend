package be.sgl.backend.dto.branch

import be.sgl.backend.entity.branch.Branch
import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "BranchBase")
data class BranchBaseDTO(
    val id: Int,
    val name: String,
    val image: String
) {

    fun toEntity() = Branch().also {
        it.id = id
        it.name = name
        it.image = image
    }

    companion object {
        fun of(branch: Branch): BranchBaseDTO {
            return BranchBaseDTO(branch.id!!, branch.name, branch.image)
        }
    }
}
