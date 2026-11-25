package be.sgl.backend.shop.domain

import java.time.LocalDateTime

object ShopOrderTestMother {

    fun shopOrder() = ShopOrderBuilder()

    class ShopOrderBuilder {

        private var id: Int? = 1
        private var paid = false
        private var price = 0.0
        private var paymentId: String? = "paymentId"
        private var createdDate: LocalDateTime? = LocalDateTime.of(2024, 1, 1, 10, 0)
        private var shop = ShopTestMother.shop().build()
        private var userId: Int? = null
        private var name = "lastName"
        private var firstName = "firstName"
        private var email = "email@domain.com"
        private var mobile: String? = null
        private var deliveryOption = DeliveryOption.PICKUP
        private var deliveryDetails: String? = null
        private var remarks: String? = null

        fun id(id: Int?) = apply { this.id = id }

        fun paid() = apply { this.paid = true }

        fun price(price: Double) = apply { this.price = price }

        fun paymentId(paymentId: String?) = apply { this.paymentId = paymentId }

        fun createdDate(createdDate: LocalDateTime?) = apply { this.createdDate = createdDate }

        fun shop(shop: Shop) = apply { this.shop = shop }

        fun userId(userId: Int) = apply { this.userId = userId }

        fun name(name: String) = apply { this.name = name }

        fun firstName(firstName: String) = apply { this.firstName = firstName }

        fun email(email: String) = apply { this.email = email }

        fun mobile(mobile: String) = apply { this.mobile = mobile }

        fun deliveryOption(deliveryOption: DeliveryOption) = apply { this.deliveryOption = deliveryOption }

        fun deliveryDetails(deliveryDetails: String) = apply { this.deliveryDetails = deliveryDetails }

        fun remarks(remarks: String) = apply { this.remarks = remarks }

        fun build() = ShopOrder().also {
            it.id = id
            it.paid = paid
            it.price = price
            it.paymentId = paymentId
            it.createdDate = createdDate
            it.shop = shop
            it.userId = userId
            it.name = name
            it.firstName = firstName
            it.email = email
            it.mobile = mobile
            it.deliveryOption = deliveryOption
            it.deliveryDetails = deliveryDetails
            it.remarks = remarks
        }
    }
}
