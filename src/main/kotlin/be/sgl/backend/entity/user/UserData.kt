package be.sgl.backend.entity.user

import be.sgl.backend.entity.Address
import be.sgl.backend.entity.Auditable
import jakarta.persistence.*
import java.time.LocalDate
import java.time.Period

@Entity
class UserData() : Auditable() {
    @Id
    val id: Int? = null
    @OneToOne
    @MapsId
    @JoinColumn(name = "id")
    lateinit var user: User
    var memberId: String? = null
    lateinit var birthdate: LocalDate
    var mobile: String? = null
    var nis: String? = null
    var accountNo: String? = null
    var sex = Sex.UNKNOWN
    var hasReduction = false
    var hasHandicap = false
    var ageDeviation = 0
    @OneToMany(fetch = FetchType.EAGER, cascade = [CascadeType.ALL])
    val addresses: MutableList<Address> = mutableListOf()
    @OneToMany
    val contacts: MutableList<Contact> = mutableListOf()

    constructor(user: User) : this() {
        this.user = user
    }

    fun getAge(referenceDate: LocalDate = LocalDate.now()): Int {
        return Period.between(birthdate, referenceDate).years
    }
}