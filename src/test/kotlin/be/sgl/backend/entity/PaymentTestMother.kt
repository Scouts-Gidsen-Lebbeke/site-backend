package be.sgl.backend.entity

import be.sgl.backend.entity.branch.Branch
import be.sgl.backend.entity.branch.BranchTestMother
import be.sgl.backend.entity.membership.Membership
import be.sgl.backend.entity.membership.MembershipPeriod
import be.sgl.backend.entity.membership.MembershipPeriodTestMother
import be.sgl.backend.entity.registrable.Registrable
import be.sgl.backend.entity.registrable.Registration
import be.sgl.backend.entity.registrable.activity.Activity
import be.sgl.backend.entity.registrable.activity.ActivityRegistration
import be.sgl.backend.entity.registrable.activity.ActivityRestriction
import be.sgl.backend.entity.registrable.activity.ActivityRestrictionTestMother
import be.sgl.backend.entity.registrable.event.Event
import be.sgl.backend.entity.registrable.event.EventRegistration
import be.sgl.backend.entity.user.User
import be.sgl.backend.entity.user.UserTestMother
import java.time.LocalDateTime

object PaymentTestMother {

    fun activityRegistration() = ActivityRegistrationBuilder()

    fun eventRegistration() = EventRegistrationBuilder()

    fun membership() = MembershipBuilder()

    abstract class PaymentBuilder<T : PaymentBuilder<T, R>, R : Payment> {
        private var id: Int? = 1
        private var paid = false
        private var price: Double = 0.0
        private var paymentId: String? = "paymentId"

        fun id(id: Int): T {
            this.id = id
            return self()
        }

        fun paid(paid: Boolean): T {
            this.paid = paid
            return self()
        }

        fun price(price: Double): T {
            this.price = price
            return self()
        }

        fun paymentId(paymentId: String): T {
            this.paymentId = paymentId
            return self()
        }

        abstract fun self(): T

        protected abstract fun create(): R

        open fun build(): R {
            val payment = create()
            payment.id = id
            payment.paid = paid
            payment.price = price
            payment.paymentId = paymentId
            return payment
        }
    }

    abstract class RegistrationBuilder<T : RegistrationBuilder<T, R, S>, R : Registration<S>, S : Registrable> : PaymentBuilder<T, R>() {

        private var completed = true
        private var additionalData: String? = null

        fun completed(completed: Boolean): T {
            this.completed = completed
            return self()
        }

        fun additionalData(additionalData: String): T {
            this.additionalData = additionalData
            return self()
        }

        override fun build(): R {
            val registrable = super.build()
            registrable.completed = completed
            registrable.additionalData = additionalData
            return registrable
        }
    }

    class ActivityRegistrationBuilder : RegistrationBuilder<ActivityRegistrationBuilder, ActivityRegistration, Activity>() {

        private var activity = PayableTestMother.activity().build()
        private var user = UserTestMother.user().build()
        private var restriction = ActivityRestrictionTestMother.activityRestriction().build()
        private var start = LocalDateTime.now()
        private var end = LocalDateTime.now()

        fun activity(activity: Activity): ActivityRegistrationBuilder {
            this.activity = activity
            return this
        }

        fun user(user: User): ActivityRegistrationBuilder {
            this.user = user
            return this
        }

        fun restriction(restriction: ActivityRestriction): ActivityRegistrationBuilder {
            this.restriction = restriction
            return this
        }

        fun start(start: LocalDateTime): ActivityRegistrationBuilder {
            this.start = start
            return this
        }

        fun end(end: LocalDateTime): ActivityRegistrationBuilder {
            this.end = end
            return this
        }

        override fun self() = this

        override fun create() = ActivityRegistration()

        override fun build(): ActivityRegistration {
            val registration = super.build()
            registration.subscribable = activity
            registration.user = user
            registration.restriction = restriction
            registration.start = start
            registration.end = end
            return registration
        }
    }

    class EventRegistrationBuilder : RegistrationBuilder<EventRegistrationBuilder, EventRegistration, Event>() {

        private var event = PayableTestMother.event().build()
        private var user: User? = null
        private var name = "lastName"
        private var firstName = "firstName"
        private var email = "email@domain.com"
        private var mobile: String? = null

        fun user(user: User): EventRegistrationBuilder {
            this.user = user
            return this
        }

        fun name(name: String): EventRegistrationBuilder {
            this.name = name
            return this
        }

        fun firstName(firstName: String): EventRegistrationBuilder {
            this.firstName = firstName
            return this
        }

        fun email(email: String): EventRegistrationBuilder {
            this.email = email
            return this
        }

        fun mobile(mobile: String): EventRegistrationBuilder {
            this.mobile = mobile
            return this
        }

        override fun create() = EventRegistration()

        override fun self() = this

        override fun build(): EventRegistration {
            val registration = super.build()
            registration.subscribable = event
            registration.user = user
            registration.name = name
            registration.firstName = firstName
            registration.email = email
            registration.mobile = mobile
            return registration
        }
    }

    class MembershipBuilder : PaymentBuilder<MembershipBuilder, Membership>() {

        private var user = UserTestMother.user().build()
        private var period = MembershipPeriodTestMother.membershipPeriod().build()
        private var branch = BranchTestMother.branch().build()

        fun user(user: User): MembershipBuilder {
            this.user = user
            return this
        }

        fun period(period: MembershipPeriod): MembershipBuilder {
            this.period = period
            return this
        }

        fun branch(branch: Branch): MembershipBuilder {
            this.branch = branch
            return this
        }

        override fun create() = Membership()

        override fun self() = this

        override fun build(): Membership {
            val membership = super.build()
            membership.user = user
            membership.period = period
            membership.branch = branch
            return membership
        }
    }
}