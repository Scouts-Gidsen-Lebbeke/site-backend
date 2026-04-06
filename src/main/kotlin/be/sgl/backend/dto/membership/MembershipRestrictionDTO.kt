package be.sgl.backend.dto.membership

import be.sgl.backend.dto.branch.BranchBaseDTO
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(name = "MembershipRestriction", description = "A limitation on the membership registration ability for a branch.")
data class MembershipRestrictionDTO(
    var id: Int?,
    var branch: BranchBaseDTO?,
    var alternativeStart: LocalDate?,
    var alternativePrice: Double?,
    var alternativeLimit: Int?
)