package be.sgl.backend.entity.user

import be.sgl.backend.entity.Auditable
import jakarta.persistence.*
import java.io.Serializable

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

    constructor(user: User, role: Role) : this() {
        this.id = UserRoleId(user.id, role.id)
        this.user = user
        this.role = role
    }

    @Embeddable
    private data class UserRoleId(var userId: Int? = null, var roleId: Int? = null) : Serializable
}