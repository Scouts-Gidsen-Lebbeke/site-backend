package be.sgl.backend.shop.domain

import be.sgl.backend.shared.persistence.Auditable
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Lob
import jakarta.persistence.OneToMany
import java.time.LocalDateTime

@Entity
class Shop : Auditable() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int? = null
    lateinit var name: String
    @Lob
    lateinit var description: String
    var open: LocalDateTime = LocalDateTime.now()
    var closed: LocalDateTime = LocalDateTime.now()
    @Enumerated(EnumType.STRING)
    @ElementCollection
    var deliveryOptions = mutableListOf<DeliveryOption>()
    @OneToMany
    var items = mutableListOf<ShopItem>()
}
