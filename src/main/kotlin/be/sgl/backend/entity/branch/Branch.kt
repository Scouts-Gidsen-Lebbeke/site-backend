package be.sgl.backend.entity.branch

import be.sgl.backend.shared.persistence.Auditable
import be.sgl.backend.entity.user.Sex
import be.sgl.backend.entity.user.User
import jakarta.persistence.*
import kotlin.jvm.Transient

@Entity
class Branch : Auditable() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int? = null
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
/**
 * [Branch] has no equals (JPA entity), so branches built from DTOs or loaded in another persistence context
 * are never `==`. Compare on id instead, falling back to identity for unsaved branches.
 */
fun Branch?.isSameAs(other: Branch?): Boolean {
    return this === other || (this?.id != null && this.id == other?.id)
}
