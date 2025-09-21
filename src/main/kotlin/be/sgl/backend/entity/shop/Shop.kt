package be.sgl.backend.entity.shop

import be.sgl.backend.entity.Payable
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.OneToMany
import java.time.LocalDateTime

@Entity
class Shop : Payable() {
    @Enumerated(EnumType.STRING)
    @ElementCollection
    var deliveryOptions = mutableListOf<DeliveryOption>()
    @OneToMany
    var items = mutableListOf<ShopItem>()
    var needsMobile: Boolean = false

    var pickupFrom: LocalDateTime? = null
    var pickupTo: LocalDateTime? = null
    var specifyPickupTime = false

    var deliveryFrom: LocalDateTime? = null
    var deliveryTo: LocalDateTime? = null
    var specifyDeliveryTime = false
    var deliveryCost: Double? = null

    var shippingCost: Double? = null

    val isNotYetOpen: Boolean
        get() = LocalDateTime.now() < open
    val isClosed: Boolean
        get() = LocalDateTime.now() > closed
}