package be.sgl.backend.shop.domain

import be.sgl.backend.shared.persistence.Auditable
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne

@Entity
class ShopOrder : Auditable() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int? = null
    var paid: Boolean = false
    var price: Double = 0.0
    @Column(unique = true)
    var paymentId: String? = null
    @Column(name = "user_id")
    var userId: Int? = null
    @ManyToOne
    lateinit var shop: Shop
    lateinit var name: String
    lateinit var firstName: String
    lateinit var email: String
    var mobile: String? = null
    @Enumerated(EnumType.STRING)
    var deliveryOption = DeliveryOption.PICKUP
    var deliveryDetails: String? = null
    var remarks: String? = null

    fun getDescription(): String {
        return "${shop.name} #$id"
    }
}
