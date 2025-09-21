package be.sgl.backend.entity.shop

import be.sgl.backend.entity.Auditable
import jakarta.persistence.*

@Entity
class ShopItemOption : Auditable() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null
    @ManyToOne
    lateinit var shop: ShopItem
    lateinit var name: String
    var stock: Int? = null
}