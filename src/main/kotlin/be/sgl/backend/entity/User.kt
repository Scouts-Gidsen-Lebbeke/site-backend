package be.sgl.backend.entity

import jakarta.persistence.*
import java.io.Serializable

@Entity
class User : Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null
    var externalId: String? = null
    var customerId: String? = null
    lateinit var name: String
        private set
    lateinit var firstName: String
        private set
    var image = "default.png"
}