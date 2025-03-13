package be.sgl.backend.dto

import java.time.LocalDate
import java.time.LocalDateTime

open class MembershipPeriodBaseDTO(
    val id: Int?,
    val start: LocalDate,
    val end: LocalDate
)

class MembershipPeriodDTO(
    val price: Double,
    val registrationLimit: Int?,
    val reductionFactor: Double,
    val siblingReduction: Double,

    id: Int?,
    start: LocalDate,
    end: LocalDate
) : MembershipPeriodBaseDTO(id, start, end)

data class MembershipRestrictionDTO(
    val id: Int?,
    var branch: BranchBaseDTO,
    var alternativePrice: Double?,
    var alternativeLimit: Int?
)

data class MembershipDTO(
    val id: Int?,
    val period: MembershipPeriodBaseDTO,
    val branch: BranchBaseDTO,
    val price: Double,
    val createdDate: LocalDateTime?
)