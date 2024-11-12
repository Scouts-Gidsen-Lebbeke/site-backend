package be.sgl.backend.entity

import jakarta.persistence.*
import java.io.Serializable

@Entity
class User : Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null
    var username: String? = null
    var externalId: String? = null
    var customerId: String? = null
    lateinit var name: String
    lateinit var firstName: String
    var image = "default.png"
}