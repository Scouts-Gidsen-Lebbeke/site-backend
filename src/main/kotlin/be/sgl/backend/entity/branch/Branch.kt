package be.sgl.backend.entity.branch

import be.sgl.backend.entity.Auditable
import be.sgl.backend.entity.user.Sex
import be.sgl.backend.entity.user.User
import jakarta.persistence.*
import java.time.temporal.TemporalAdjusters.lastDayOfYear
import kotlin.jvm.Transient

@Entity
class Branch : Auditable() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null
    lateinit var name: String
    lateinit var email: String
    var minimumAge = 0
    var maximumAge: Int? = null
    @Enumerated(EnumType.STRING)
    var sex: Sex? = null
    @Lob
    var description: String? = null
    @Lob
    var law: String? = null
    lateinit var image: String
    @Enumerated(EnumType.STRING)
    var status = BranchStatus.PASSIVE
    var staffTitle: String? = null
    @Transient
    var staff = listOf<User>()

    fun matchesUser(user: User): Boolean {
        val age = user.getAge() + user.ageDeviation
        return age >= minimumAge && (maximumAge == null || age <= maximumAge!!) && (sex == null || user.sex == sex)
    }

    override fun toString() = name
}