package be.sgl.backend.dto.membership

import be.sgl.backend.entity.membership.MembershipPeriod
import be.sgl.backend.util.StartEndDate
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import java.time.LocalDate

class CreateOrUpdateMembershipPeriodRequest(
    @field:NotNull(message = "{NotNull.membershipPeriod.start}")
    override var start: LocalDate?,
    @field:NotNull(message = "{NotNull.membershipPeriod.end}")
    override var end: LocalDate?,
    @field:NotNull(message = "{NotNull.membershipPeriod.price}")
    @field:PositiveOrZero(message = "{PositiveOrZero.membershipPeriod.price}")
    var price: Double?,
    @field:Positive(message = "{Positive.membershipPeriod.registrationLimit}")
    var registrationLimit: Int?,
    @field:NotNull(message = "{NotNull.membershipPeriod.reductionFactor}")
    var reductionFactor: Double?,
    @field:NotNull(message = "{NotNull.membershipPeriod.siblingReduction}")
    var siblingReduction: Double?,
    var restrictions: List<MembershipRestrictionDTO>
) : StartEndDate {

    /**
     * The period of the restrictions is not set, as the period is not persisted yet.
     */
    fun toEntity() = MembershipPeriod().also {
        it.start = start!!
        it.end = end!!
        price?.let { price -> it.price = price }
        it.registrationLimit = registrationLimit
        reductionFactor?.let { factor -> it.reductionFactor = factor }
        siblingReduction?.let { reduction -> it.siblingReduction = reduction }
        it.restrictions = restrictions.map(MembershipRestrictionDTO::toEntity).toMutableList()
    }
}
