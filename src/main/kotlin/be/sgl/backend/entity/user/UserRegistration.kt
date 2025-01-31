package be.sgl.backend.entity.user

import be.sgl.backend.entity.Address
import jakarta.persistence.*
import java.time.LocalDate

@Entity
class UserRegistration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null
    var externalId: String? = null
    lateinit var name: String
    lateinit var firstName: String
    lateinit var birthdate: LocalDate
    lateinit var email: String
    lateinit var mobile: String
    var sex = Sex.UNKNOWN
    var hasReduction = false
    var hasHandicap = false
    @OneToOne(fetch = FetchType.EAGER)
    lateinit var address: Address
}