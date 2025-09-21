package be.sgl.backend.entity.shop

import be.sgl.backend.entity.Address
import be.sgl.backend.entity.Payment
import be.sgl.backend.entity.user.User
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
class ShopOrder : Payment() {

    @ManyToOne
    lateinit var shop: Shop
    @OneToMany
    var items = mutableListOf<OrderItem>()

    @ManyToOne
    var user: User? = null
    lateinit var name: String
    lateinit var firstName: String
    lateinit var email: String
    var mobile: String? = null

    var completed = false
    var remarks: String? = null

    @Enumerated(EnumType.STRING)
    var deliveryOption = DeliveryOption.PICKUP

    var pickupFrom: LocalDateTime? = null
    var pickupTo: LocalDateTime? = null

    var deliveryFrom: LocalDateTime? = null
    var deliveryTo: LocalDateTime? = null

    @OneToOne(cascade = [CascadeType.ALL], orphanRemoval = true)
    var shippingOrDeliveryAddress: Address? = null

    var arrangedContactPerson: String? = null

    override fun getDescription(): String {
        return "${shop.name} #$id"
    }
}