package be.sgl.backend.dto.membership

import be.sgl.backend.entity.membership.MembershipPeriod
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

// read-only
@Schema(name = "MembershipPeriodBase")
class MembershipPeriodBaseDTO(
    val id: Int,
    val start: LocalDate,
    val end: LocalDate
) {
    companion object {
        fun of(period: MembershipPeriod): MembershipPeriodBaseDTO {
            return MembershipPeriodBaseDTO(period.id!!, period.start, period.end)
        }
    }
}
