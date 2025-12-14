package be.sgl.backend.dto.membership

import be.sgl.backend.util.StartEndDate
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import java.time.LocalDate

@Schema(name = "MembershipPeriod", description = "The complete membership configuration.")
class MembershipPeriodDTO(
    var id: Int?,
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
) : StartEndDate