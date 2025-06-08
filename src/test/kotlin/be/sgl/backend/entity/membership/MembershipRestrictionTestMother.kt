package be.sgl.backend.entity.membership

import be.sgl.backend.entity.branch.Branch
import java.time.LocalDate

class MembershipRestrictionTestMother {

    fun membershipRestriction() = MembershipRestrictionBuilder()

    class MembershipRestrictionBuilder {

        private var id: Int? = 1
        private var period = MembershipPeriodTestMother.membershipPeriod().build()
        private var branch: Branch? = null
        private var alternativeStart: LocalDate? = null
        private var alternativePrice: Double? = null
        private var registrationLimit: Int? = null

        fun id(id: Int) = apply { this.id = id }

        fun period(period: MembershipPeriod) = apply { this.period = period }

        fun branch(branch: Branch) = apply { this.branch = branch }

        fun alternativeStart(start: LocalDate) = apply { this.alternativeStart = start }

        fun alternativePrice(price: Double) = apply { this.alternativePrice = price }

        fun registrationLimit(limit: Int) = apply { this.registrationLimit = limit }

        fun build(): MembershipRestriction {
            val restriction = MembershipRestriction()
            return restriction
        }
    }
}