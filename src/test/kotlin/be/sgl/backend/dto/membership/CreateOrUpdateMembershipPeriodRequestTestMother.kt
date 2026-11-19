package be.sgl.backend.dto.membership

import java.time.LocalDate

object CreateOrUpdateMembershipPeriodRequestTestMother {

    fun membershipPeriodRequest() = CreateOrUpdateMembershipPeriodRequestBuilder()

    class CreateOrUpdateMembershipPeriodRequestBuilder {
        private var start: LocalDate = LocalDate.of(2025, 9, 1)
        private var end: LocalDate = LocalDate.of(2026, 8, 31)
        private var price = 100.0
        private var registrationLimit: Int? = null
        private var reductionFactor = 3.0
        private var siblingReduction = 0.0
        private val restrictions = mutableListOf<MembershipRestrictionDTO>()

        fun start(start: LocalDate) = apply { this.start = start }

        fun end(end: LocalDate) = apply { this.end = end }

        fun price(price: Double) = apply { this.price = price }

        fun registrationLimit(registrationLimit: Int?) = apply { this.registrationLimit = registrationLimit }

        fun reductionFactor(reductionFactor: Double) = apply { this.reductionFactor = reductionFactor }

        fun siblingReduction(siblingReduction: Double) = apply { this.siblingReduction = siblingReduction }

        fun restrictions(vararg restrictions: MembershipRestrictionDTO) = apply { this.restrictions.addAll(restrictions) }

        fun build() = CreateOrUpdateMembershipPeriodRequest(start, end, price, registrationLimit, reductionFactor,
            siblingReduction, restrictions.toList())
    }
}
