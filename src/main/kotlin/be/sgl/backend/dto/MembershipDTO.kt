package be.sgl.backend.dto

import be.sgl.backend.entity.membership.Membership
import be.sgl.backend.entity.membership.MembershipPeriod
import be.sgl.backend.entity.registrable.RegistrableStatus
import be.sgl.backend.entity.registrable.RegistrableStatus.Companion.getStatus
import be.sgl.backend.util.StartEndDate
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "Basic information about a membership period.")
open class MembershipPeriodBaseDTO(
    val id: Int?,
    @field:NotNull(message = "{NotNull.membershipPeriod.start}")
    override var start: LocalDate?,
    @field:NotNull(message = "{NotNull.membershipPeriod.end}")
    override var end: LocalDate?
) : StartEndDate

@Schema(description = "The complete membership configuration.")
class MembershipPeriodDTO(
    id: Int?,
    start: LocalDate?,
    end: LocalDate?,
    @field:NotNull(message = "{NotNull.membershipPeriod.price}")
    @field:PositiveOrZero(message = "{PositiveOrZero.membershipPeriod.price}")
    var price: Double,
    @field:Positive(message = "{Positive.membershipPeriod.registrationLimit}")
    var registrationLimit: Int?,
    @field:NotNull(message = "{NotNull.membershipPeriod.reductionFactor}")
    var reductionFactor: Double,
    @field:NotNull(message = "{NotNull.membershipPeriod.siblingReduction}")
    val siblingReduction: Double,
    var restrictions: List<MembershipRestrictionDTO>
) : MembershipPeriodBaseDTO(id, start, end)

// DTO for statistics list overview
class MembershipPeriodResultDTO(
    id: Int?,
    start: LocalDate,
    end: LocalDate,
    var registrationCount: Int,
    var totalPrice: Double
) : MembershipPeriodBaseDTO(id, start, end) {
    constructor(period: MembershipPeriod, memberships: List<Double>) :
            this(period.id, period.start, period.end, memberships.count(), memberships.sum())
}

@Schema(description = "A limitation on the membership registration ability for a branch.")
data class MembershipRestrictionDTO(
    val id: Int?,
    var branch: BranchBaseDTO?,
    var alternativePrice: Double?,
    var alternativeLimit: Int?
)

data class MembershipDTO(
    val id: Int?,
    val period: MembershipPeriodBaseDTO,
    val branch: BranchBaseDTO,
    val user: UserDTO,
    val price: Double,
    val paid: Boolean,
    val createdDate: LocalDateTime?
)