package be.sgl.backend.entity.user

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
    @OneToOne(cascade = [CascadeType.ALL], mappedBy = "user")
    var userData = UserData(this)
    @OneToOne(cascade = [CascadeType.ALL], mappedBy = "user")
    var staffData = StaffData(this)
    @OneToMany(fetch = FetchType.EAGER, cascade = [CascadeType.ALL])
    val roles: MutableList<UserRole> = mutableListOf()
    val level: RoleLevel
        get() = roles.maxOfOrNull { it.role.level } ?: RoleLevel.GUEST
    @ManyToMany
    @JoinTable(name = "sibling_relation")
    val siblings: MutableList<User> = mutableListOf()

    fun getFullName(): String {
        return "$firstName $name"
    }
}