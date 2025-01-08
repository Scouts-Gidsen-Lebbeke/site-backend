package be.sgl.backend.entity.shop

import be.sgl.backend.entity.Payment
import be.sgl.backend.entity.user.User
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.ManyToOne

@Entity
class ShopOrder : Payment() {
    @ManyToOne
    var user: User? = null
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

    override fun getDescription(): String {
        return "${shop.name} #$id"
    }
}