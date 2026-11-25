package be.sgl.backend.entity.user

import be.sgl.backend.shared.Sex
import be.sgl.backend.shared.security.RoleLevel
import be.sgl.backend.shared.address.Address
import jakarta.persistence.*
import org.hibernate.annotations.Formula
import java.io.Serializable
import java.time.LocalDate
import java.time.Period
import kotlin.jvm.Transient

@Entity
@Table(
    indexes = [
        Index(name = "idx_username", columnList = "username", unique = true),
    ]
)
class User : Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int? = null

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

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
    val addresses: MutableList<Address> = mutableListOf()

    // TODO: read the level from UserLevelProvider once people is extracted
    @Formula("(select coalesce(max(r.level), 0) from user_role ur join role r on r.id = ur.role_id where ur.user_id = id)")
    private var levelOrdinal: Int = 0
    val level: RoleLevel
        get() = RoleLevel.entries[levelOrdinal]

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