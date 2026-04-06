package be.sgl.backend.dto.registrable.activity

import be.sgl.backend.dto.branch.BranchBaseDTO
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(name = "ActivityRestriction", description = "A limitation on the activity registration ability for a branch.")
data class ActivityRestrictionDTO(
    val id: Int?,
    val branch: BranchBaseDTO,
    val name: String?,
    val alternativeStart: LocalDateTime?,
    val alternativeEnd: LocalDateTime?,
    var alternativePrice: Double?,
    val alternativeLimit: Int?
)