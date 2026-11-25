package be.sgl.backend.dto.registrable.event

import be.sgl.backend.shared.address.AddressDTO
import java.time.LocalDateTime

object CreateOrUpdateEventRequestTestMother {

    fun eventRequest() = CreateOrUpdateEventRequestBuilder()

    class CreateOrUpdateEventRequestBuilder {
        private var name = "eventName"
        private var description = "eventDescription"
        private var open = LocalDateTime.now().plusDays(1)
        private var closed = LocalDateTime.now().plusDays(10)
        private var start = LocalDateTime.now().plusDays(20)
        private var end = LocalDateTime.now().plusDays(21)
        private var cancellable = true
        private var price = 10.0
        private var registrationLimit: Int? = null
        private var address: AddressDTO? = null
        private var additionalForm: String? = null
        private var additionalFormRule: String? = null
        private var sendConfirmation = true
        private var sendCompleteConfirmation = false
        private var communicationCC: String? = null
        private var needsMobile = false

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

        fun needsMobile() = apply { this.needsMobile = true }

        fun build() = CreateOrUpdateEventRequest(name, start, end, closed, cancellable, description, open, price,
            registrationLimit, address, additionalForm, additionalFormRule, sendConfirmation, sendCompleteConfirmation,
            communicationCC, needsMobile)
    }
}
