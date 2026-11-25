package be.sgl.backend.shop.domain

import java.time.LocalDateTime

object ShopTestMother {

    fun shop() = ShopBuilder()

    class ShopBuilder {

        private var id: Int? = 1
        private var name = "shopName"
        private var description = "shopDescription"
        private var open = LocalDateTime.now()
        private var closed = LocalDateTime.now()
        private var deliveryOptions = mutableListOf<DeliveryOption>()
        private var items = mutableListOf<ShopItem>()

        fun id(id: Int?) = apply { this.id = id }

        fun name(name: String) = apply { this.name = name }

        fun description(description: String) = apply { this.description = description }

        fun open(open: LocalDateTime) = apply { this.open = open }

        fun closed(closed: LocalDateTime) = apply { this.closed = closed }

        fun deliveryOptions(vararg deliveryOptions: DeliveryOption) = apply { this.deliveryOptions = deliveryOptions.toMutableList() }

        fun items(vararg items: ShopItem) = apply { this.items = items.toMutableList() }

        fun build() = Shop().also {
            it.id = id
            it.name = name
            it.description = description
            it.open = open
            it.closed = closed
            it.deliveryOptions = deliveryOptions
            it.items = items
        }
    }
}
