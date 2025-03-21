package be.sgl.backend.entity.user

import be.sgl.backend.entity.Address
import jakarta.persistence.*
import java.io.Serializable
import java.time.LocalDate
import java.time.Period

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
    var memberId: String? = null

    @Column(nullable = false)
    lateinit var name: String
    @Column(nullable = false)
    lateinit var firstName: String
    @Column(nullable = false)
    lateinit var email: String
    @Column(nullable = false)
    lateinit var birthdate: LocalDate
    var ageDeviation = 0
    @Column(nullable = false)
    var sex = Sex.UNKNOWN

    var image: String? = null
    var mobile: String? = null
    var nis: String? = null
    var accountNo: String? = null
    var hasReduction = false
    var hasHandicap = false

    @OneToMany(fetch = FetchType.EAGER, cascade = [CascadeType.ALL])
    val addresses: MutableList<Address> = mutableListOf()

    @OneToMany(fetch = FetchType.EAGER, cascade = [CascadeType.ALL])
    val contacts: MutableList<Contact> = mutableListOf()

    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL], fetch = FetchType.EAGER)
    val roles: MutableList<UserRole> = mutableListOf()
    val level: RoleLevel
        get() = roles.maxOfOrNull { it.role.level } ?: RoleLevel.GUEST

    @OneToOne(cascade = [CascadeType.ALL], mappedBy = "user")
    var staffData = StaffData(this)

    fun getFullName(): String {
        return "$firstName $name"
    }

    fun getAge(referenceDate: LocalDate = LocalDate.now()): Int {
        return Period.between(birthdate, referenceDate).years
    }

    fun getHomeAddress(): Address? {
        return addresses.find { it.postalAdress }
    }
}