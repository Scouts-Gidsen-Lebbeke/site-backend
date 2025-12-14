package be.sgl.backend.dto.membership

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

// read-only
@Schema(name = "MembershipPeriodBase")
class MembershipPeriodBaseDTO(
    val id: Int,
    val start: LocalDate,
    val end: LocalDate
)