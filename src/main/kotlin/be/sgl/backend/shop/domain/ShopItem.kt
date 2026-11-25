package be.sgl.backend.shop.domain

import be.sgl.backend.shared.persistence.Auditable
import jakarta.persistence.*

@Entity
class ShopItem : Auditable() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null
    @ManyToOne
    lateinit var shop: Shop
    lateinit var name: String
    lateinit var info: String
    var price = 0.0
    var image: String? = null
    @ElementCollection
    lateinit var options: List<String>
}