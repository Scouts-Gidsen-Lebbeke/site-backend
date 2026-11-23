package be.sgl.backend.dto.membership

import be.sgl.backend.entity.membership.MembershipPeriod
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

// read-only
@Schema(name = "MembershipPeriod", description = "The complete membership configuration.")
class MembershipPeriodDTO(
    val id: Int,
    val start: LocalDate,
    val end: LocalDate,
    val price: Double,
    val registrationLimit: Int?,
    val reductionFactor: Double,
    val siblingReduction: Double,
    val restrictions: List<MembershipRestrictionDTO>
) {
    companion object {
        fun of(period: MembershipPeriod): MembershipPeriodDTO {
            return MembershipPeriodDTO(period.id!!, period.start, period.end, period.price, period.registrationLimit,
                period.reductionFactor, period.siblingReduction, period.restrictions.map(MembershipRestrictionDTO::of))
        }
    }
}
