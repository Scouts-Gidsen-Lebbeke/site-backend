package be.sgl.backend.entity.shop

import be.sgl.backend.entity.Payable
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.OneToMany

@Entity
class Shop : Payable() {
    @Enumerated(EnumType.STRING)
    @ElementCollection
    var deliveryOptions = mutableListOf<DeliveryOption>()
    @OneToMany
    var items = mutableListOf<ShopItem>()
}