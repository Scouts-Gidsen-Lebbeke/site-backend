package be.sgl.backend.entity.user

import be.sgl.backend.entity.Auditable
import jakarta.persistence.*
import java.io.Serializable
import java.time.LocalDate

@Entity
class UserRole() : Auditable() {

    @EmbeddedId
    private lateinit var id: UserRoleId
    @MapsId("userId")
    @ManyToOne
    @JoinColumn
    lateinit var user: User
    @MapsId("roleId")
    @ManyToOne
    @JoinColumn
    lateinit var role: Role
    var startDate: LocalDate? = null
    var endDate: LocalDate? = null

    constructor(user: User, role: Role, startDate: LocalDate = LocalDate.now(), endDate: LocalDate? = null) : this() {
        this.id = UserRoleId(user.id, role.id)
        this.user = user
        this.role = role
        this.startDate = startDate
        this.endDate = endDate
    }

    @Embeddable
    private data class UserRoleId(var userId: Int? = null, var roleId: Int? = null) : Serializable
}