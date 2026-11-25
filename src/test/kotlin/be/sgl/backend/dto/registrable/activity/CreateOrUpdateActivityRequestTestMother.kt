package be.sgl.backend.dto.registrable.activity

import be.sgl.backend.shared.address.AddressDTO
import be.sgl.backend.entity.registrable.activity.ActivityRestriction
import be.sgl.backend.entity.registrable.activity.ActivityRestrictionTestMother
import java.time.LocalDateTime

object CreateOrUpdateActivityRequestTestMother {

    fun activityRequest() = CreateOrUpdateActivityRequestBuilder()

    class CreateOrUpdateActivityRequestBuilder {
        private var name = "activityName"
        private var description = "activityDescription"
        private var open = LocalDateTime.now().plusDays(1)
        private var closed = LocalDateTime.now().plusDays(10)
        private var start = LocalDateTime.now().plusDays(20)
        private var end = LocalDateTime.now().plusDays(22)
        private var cancellable = true
        private var price = 100.0
        private var registrationLimit: Int? = null
        private var address: AddressDTO? = null
        private var additionalForm: String? = null
        private var additionalFormRule: String? = null
        private var sendConfirmation = true
        private var sendCompleteConfirmation = false
        private var communicationCC: String? = null
        private var reductionFactor = 3.0
        private var siblingReduction = 0.0
        private var restrictions = listOf(ActivityRestrictionDTO.of(ActivityRestrictionTestMother.activityRestriction().build()))
        private var cancelled = false

        fun name(name: String) = apply { this.name = name }

        fun description(description: String) = apply { this.description = description }

        fun open(open: LocalDateTime) = apply { this.open = open }

        fun closed(closed: LocalDateTime) = apply { this.closed = closed }

        fun start(start: LocalDateTime) = apply { this.start = start }

        fun end(end: LocalDateTime) = apply { this.end = end }

        fun cancellable(cancellable: Boolean) = apply { this.cancellable = cancellable }

        fun price(price: Double) = apply { this.price = price }

        fun registrationLimit(registrationLimit: Int?) = apply { this.registrationLimit = registrationLimit }

        fun address(address: AddressDTO) = apply { this.address = address }

        fun additionalForm(additionalForm: String) = apply { this.additionalForm = additionalForm }

        fun additionalFormRule(additionalFormRule: String) = apply { this.additionalFormRule = additionalFormRule }

        fun sendConfirmation(sendConfirmation: Boolean) = apply { this.sendConfirmation = sendConfirmation }

        fun sendCompleteConfirmation(sendCompleteConfirmation: Boolean) = apply { this.sendCompleteConfirmation = sendCompleteConfirmation }

        fun communicationCC(communicationCC: String) = apply { this.communicationCC = communicationCC }

        fun reductionFactor(reductionFactor: Double) = apply { this.reductionFactor = reductionFactor }

        fun siblingReduction(siblingReduction: Double) = apply { this.siblingReduction = siblingReduction }

        fun restrictions(vararg restrictions: ActivityRestriction) = apply {
            this.restrictions = restrictions.map(ActivityRestrictionDTO::of)
        }

        fun cancelled() = apply { this.cancelled = true }

        fun build() = CreateOrUpdateActivityRequest(name, start, end, closed, cancellable, description, open, price,
            registrationLimit, address, additionalForm, additionalFormRule, sendConfirmation, sendCompleteConfirmation,
            communicationCC, reductionFactor, siblingReduction, restrictions, cancelled)
    }
}
