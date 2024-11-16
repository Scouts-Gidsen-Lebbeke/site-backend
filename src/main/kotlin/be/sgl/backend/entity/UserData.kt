package be.sgl.backend.entity

import be.sgl.backend.entity.enum.Sex
import jakarta.persistence.*
import java.time.LocalDate

@Entity
class UserData : Auditable() {
    @Id
    val id: Int? = null
    @OneToOne
    @PrimaryKeyJoinColumn
    lateinit var user: User
    var memberId: String? = null
    var birthdate: LocalDate = LocalDate.now()
    var email: String? = null
    var mobile: String? = null
    var nis: String? = null
    var accountNo: String? = null
    var sex = Sex.UNKNOWN
    var hasReduction = false
    @OneToMany(fetch = FetchType.EAGER)
    val addresses: MutableList<Address> = mutableListOf()
    @OneToMany
    val contacts: MutableList<Contact> = mutableListOf()
}