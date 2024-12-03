package be.sgl.backend.entity.shop

enum class DeliveryOption(val cost: Double = 0.0) {
    PICKUP, DELIVERY(4.0), SHIPPING(7.0), ARRANGED
}