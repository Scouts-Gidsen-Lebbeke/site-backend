package be.sgl.backend.dto.membership

import be.sgl.backend.dto.branch.BranchBaseDTO
import be.sgl.backend.entity.membership.MembershipRestriction
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(name = "MembershipRestriction", description = "A limitation on the membership registration ability for a branch.")
data class MembershipRestrictionDTO(
    var id: Int?,
    var branch: BranchBaseDTO?,
    var alternativeStart: LocalDate?,
    var alternativePrice: Double?,
    var alternativeLimit: Int?
) {

    /**
     * The period is not set, it should be linked by the caller.
     */
    fun toEntity() = MembershipRestriction().also {
        it.id = id
        it.branch = branch?.toEntity()
        it.alternativeStart = alternativeStart
        it.alternativePrice = alternativePrice
        it.registrationLimit = alternativeLimit
    }

    companion object {
        fun of(restriction: MembershipRestriction): MembershipRestrictionDTO {
            return MembershipRestrictionDTO(restriction.id, restriction.branch?.let(BranchBaseDTO::of),
                restriction.alternativeStart, restriction.alternativePrice, restriction.registrationLimit)
        }
    }
}
