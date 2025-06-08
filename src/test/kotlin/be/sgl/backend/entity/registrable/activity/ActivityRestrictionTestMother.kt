package be.sgl.backend.entity.registrable.activity

import be.sgl.backend.entity.PayableTestMother
import be.sgl.backend.entity.branch.Branch
import be.sgl.backend.entity.branch.BranchTestMother
import java.time.LocalDateTime

object ActivityRestrictionTestMother {

    fun activityRestriction() = ActivityRestrictionBuilder()

    class ActivityRestrictionBuilder {

        private var activity = PayableTestMother.activity().build()
        private var branch = BranchTestMother.branch().build()
        private var name: String? = null
        private var alternativeStart: LocalDateTime? = null
        private var alternativeEnd: LocalDateTime? = null
        private var alternativePrice: Double? = null
        private var alternativeLimit: Int? = null

        fun activity(activity: Activity) = apply { this.activity = activity }

        fun branch(branch: Branch) = apply { this.branch = branch }

        fun name(name: String) = apply { this.name = name }

        fun alternativeStart(start: LocalDateTime) = apply { this.alternativeStart = start }

        fun alternativeEnd(end: LocalDateTime) = apply { this.alternativeEnd = end }

        fun alternativePrice(price: Double) = apply { this.alternativePrice = price }

        fun alternativeLimit(limit: Int) = apply { this.alternativeLimit = limit }

        fun build(): ActivityRestriction {
            val restriction = ActivityRestriction()
            restriction.activity = activity
            restriction.branch = branch
            restriction.name = name
            restriction.alternativeStart = alternativeStart
            restriction.alternativeEnd = alternativeEnd
            restriction.alternativePrice = alternativePrice
            restriction.alternativeLimit = alternativeLimit
            return restriction
        }
    }
}