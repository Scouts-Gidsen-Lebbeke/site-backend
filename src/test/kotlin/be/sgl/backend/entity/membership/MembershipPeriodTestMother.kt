package be.sgl.backend.entity.membership

import java.time.LocalDate

object MembershipPeriodTestMother {

    fun membershipPeriod() = MembershipPeriodBuilder()

    class MembershipPeriodBuilder {

        private var id: Int? = null
        private var start = LocalDate.now()
        private var end = LocalDate.now().plusYears(1)
        private var price = 100.0
        private var registrationLimit: Int? = null
        private var reductionFactor = 3.0
        private var siblingReduction = 0.0
        private var restrictions = mutableListOf<MembershipRestriction>()

        fun id(id: Int) = apply { this.id = id }

        fun start(start: LocalDate) = apply { this.start = start }

        fun end(end: LocalDate) = apply { this.end = end }

        fun price(price: Double) = apply { this.price = price }

        fun registrationLimit(registrationLimit: Int) = apply { this.registrationLimit = registrationLimit }

        fun reductionFactor(reductionFactor: Double) = apply { this.reductionFactor = reductionFactor }

        fun siblingReduction(reduction: Double) = apply { this.siblingReduction = reduction }

        fun restrictions(vararg restrictions: MembershipRestriction) = apply { this.restrictions.addAll(restrictions) }

        fun build(): MembershipPeriod {
            val period = MembershipPeriod()
            period.start = start
            period.end = end
            period.price = price
            period.registrationLimit = registrationLimit
            period.reductionFactor = reductionFactor
            period.siblingReduction = siblingReduction
            period.restrictions = restrictions
            return period
        }
    }
}