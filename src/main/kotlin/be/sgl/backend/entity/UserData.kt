package be.sgl.backend.entity

import be.sgl.backend.entity.enum.Sex
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.PrimaryKeyJoinColumn
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
    @OneToMany
    val addresses: MutableList<Address> = mutableListOf()
    @OneToMany
    val contacts: MutableList<Contact> = mutableListOf()
}