package be.sgl.backend.entity.user

import be.sgl.backend.entity.branch.Branch
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
    var userData = UserData()
    @OneToOne(fetch = FetchType.LAZY)
    var staffData = StaffData()
    @OneToMany(fetch = FetchType.EAGER)
    val roles: MutableList<UserRole> = mutableListOf()
    @ManyToMany
    @JoinTable(name = "sibling_relation")
    val siblings: MutableList<User> = mutableListOf()

    fun getFullName(): String {
        return "$firstName $name"
    }
}