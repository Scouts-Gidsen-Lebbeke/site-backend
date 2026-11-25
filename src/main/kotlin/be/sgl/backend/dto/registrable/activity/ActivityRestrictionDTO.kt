package be.sgl.backend.dto.registrable.activity

import be.sgl.backend.organization.web.BranchBaseDTO
import be.sgl.backend.entity.registrable.activity.ActivityRestriction
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
) {

    /**
     * The activity is not set, it should be linked by the caller.
     */
    fun toEntity() = ActivityRestriction().also {
        it.id = id
        it.branch = branch.toEntity()
        it.name = name
        it.alternativeStart = alternativeStart
        it.alternativeEnd = alternativeEnd
        it.alternativePrice = alternativePrice
        it.alternativeLimit = alternativeLimit
    }

    companion object {
        fun of(restriction: ActivityRestriction): ActivityRestrictionDTO {
            return ActivityRestrictionDTO(restriction.id, BranchBaseDTO.of(restriction.branch), restriction.name,
                restriction.alternativeStart, restriction.alternativeEnd, restriction.alternativePrice,
                restriction.alternativeLimit)
        }
    }
}
