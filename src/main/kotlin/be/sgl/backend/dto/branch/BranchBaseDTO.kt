package be.sgl.backend.dto.branch

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "BranchBase")
data class BranchBaseDTO(
    val id: Int,
    val name: String,
    val image: String
)