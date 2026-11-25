package be.sgl.backend.organization

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "BranchSummary")
data class BranchSummary(
    val id: Int,
    val name: String,
    val image: String
)
