package be.sgl.backend.entity

import be.sgl.backend.entity.registrable.Registrable
import be.sgl.backend.entity.registrable.activity.Activity
import be.sgl.backend.entity.registrable.activity.ActivityRestriction
import be.sgl.backend.entity.registrable.event.Event
import be.sgl.backend.entity.shop.DeliveryOption
import be.sgl.backend.entity.shop.Shop
import be.sgl.backend.entity.shop.ShopItem
import java.time.LocalDateTime

object PayableTestMother {

    fun activity() = ActivityBuilder()

    fun event() = EventBuilder()

    fun shop() = ShopBuilder()

    abstract class PayableBuilder<T : PayableBuilder<T, R>, R : Payable> {
        private var id: Int? = 1
        private var name = "activityName"
        private var description = "activityDescription"
        private var open = LocalDateTime.now()
        private var closed = LocalDateTime.now()

        fun id(id: Int): T {
            this.id = id
            return self()
        }

        fun name(name: String): T {
            this.name = name
            return self()
        }

        fun description(description: String): T {
            this.description = description
            return self()
        }

        fun open(open: LocalDateTime): T {
            this.open = open
            return self()
        }

        fun closed(closed: LocalDateTime): T {
            this.closed = closed
            return self()
        }

        abstract fun self(): T

        protected abstract fun create(): R

        open fun build(): R {
            val registrable = create()
            registrable.id = id
            registrable.name = name
            registrable.description = description
            registrable.open = open
            registrable.closed = closed
            return registrable
        }
    }

    abstract class RegistrableBuilder<T : RegistrableBuilder<T, R>, R : Registrable> : PayableBuilder<T, R>() {

        private var start = LocalDateTime.now()
        private var end = LocalDateTime.now()
        private var price = 0.0
        private var registrationLimit: Int? = null
        private var address: Address? = null
        private var additionalForm: String? = null
        private var additionalFormRule: String? = null
        private var cancellable = true
        private var sendConfirmation = false
        private var sendCompleteConfirmation = false
        private var communicationCC: String? = null
        private var cancelled = false

        fun start(start: LocalDateTime): T {
            this.start = start
            return self()
        }

        fun end(end: LocalDateTime): T {
            this.end = end
            return self()
        }

        fun price(price: Double): T {
            this.price = price
            return self()
        }

        fun registrationLimit(registrationLimit: Int?): T {
            this.registrationLimit = registrationLimit
            return self()
        }

        fun address(address: Address): T {
            this.address = address
            return self()
        }

        fun additionalForm(additionalForm: String): T {
            this.additionalForm = additionalForm
            return self()
        }

        fun additionalFormRule(additionalFormRule: String): T {
            this.additionalFormRule = additionalFormRule
            return self()
        }

        fun cancellable(cancellable: Boolean): T {
            this.cancellable = cancellable
            return self()
        }

        fun sendConfirmation(sendConfirmation: Boolean): T {
            this.sendConfirmation = sendConfirmation
            return self()
        }

        fun sendCompleteConfirmation(sendCompleteConfirmation: Boolean): T {
            this.sendCompleteConfirmation = sendCompleteConfirmation
            return self()
        }

        fun communicationCC(communicationCC: String): T {
            this.communicationCC = communicationCC
            return self()
        }

        fun cancelled(cancelled: Boolean): T {
            this.cancelled = cancelled
            return self()
        }

        override fun build(): R {
            val registrable = super.build()
            registrable.start = start
            registrable.end = end
            registrable.price = price
            registrable.registrationLimit = registrationLimit
            registrable.address = address
            registrable.additionalForm = additionalForm
            registrable.additionalFormRule = additionalFormRule
            registrable.cancellable = cancellable
            registrable.sendConfirmation = sendConfirmation
            registrable.sendCompleteConfirmation = sendCompleteConfirmation
            registrable.communicationCC = communicationCC
            registrable.cancelled = cancelled
            return registrable
        }
    }

    class ActivityBuilder : RegistrableBuilder<ActivityBuilder, Activity>() {

        private var reductionFactor: Double = 3.0
        private var siblingReduction: Double = 0.0
        private var restrictions = mutableListOf<ActivityRestriction>()

        fun reductionFactor(reduction: Double): ActivityBuilder {
            this.reductionFactor = reduction
            return this
        }

        fun siblingReduction(siblingReduction: Double): ActivityBuilder {
            this.siblingReduction = siblingReduction
            return this
        }

        fun restrictions(vararg restrictions: ActivityRestriction): ActivityBuilder {
            this.restrictions = restrictions.toMutableList()
            return this
        }

        override fun self() = this

        override fun create() = Activity()

        override fun build(): Activity {
            val activity = super.build()
            activity.reductionFactor = siblingReduction
            activity.siblingReduction = siblingReduction
            activity.restrictions = restrictions
            return activity
        }
    }

    class EventBuilder : RegistrableBuilder<EventBuilder, Event>() {

        private var needsMobile = false

        override fun create() = Event()

        override fun self() = this

        override fun build(): Event {
            val event = super.build()
            event.needsMobile = needsMobile
            return event
        }
    }

    class ShopBuilder : PayableBuilder<ShopBuilder, Shop>() {

        private var deliveryOptions = mutableListOf<DeliveryOption>()
        private var items = mutableListOf<ShopItem>()

        fun deliveryOptions(vararg deliveryOptions: DeliveryOption): ShopBuilder {
            this.deliveryOptions = deliveryOptions.toMutableList()
            return this
        }

        fun items(vararg items: ShopItem): ShopBuilder {
            this.items = items.toMutableList()
            return this
        }

        override fun create() = Shop()

        override fun self() = this

        override fun build(): Shop {
            val shop = super.build()
            shop.deliveryOptions = deliveryOptions
            shop.items = items
            return shop
        }
    }
}