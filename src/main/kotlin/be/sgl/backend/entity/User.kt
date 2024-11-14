package be.sgl.backend.entity

import jakarta.persistence.*
import java.io.Serializable

@Entity
@Table(
    indexes = [
        Index(name = "idx_username", columnList = "username", unique = true),
    ]
)
class User : Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null
    var username: String? = null
    var externalId: String? = null
    var customerId: String? = null
    lateinit var name: String
    lateinit var firstName: String
    var image: String? = null
    @OneToOne(fetch = FetchType.LAZY)
    val userData: UserData? = null
    @ManyToMany
    @JoinTable
    val roles: MutableList<Role> = mutableListOf()
    @ManyToMany
    @JoinTable(name = "sibling_relation")
    val siblings: MutableList<User> = mutableListOf()
}