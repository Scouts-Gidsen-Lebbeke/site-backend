package be.sgl.backend.access.domain

import be.sgl.backend.shared.persistence.Auditable
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import java.io.Serializable

@Entity
@IdClass(UserRole.Key::class)
class UserRole() : Auditable() {

    @Id
    var userId: Int = 0
    @Id
    var roleId: Int = 0

    constructor(userId: Int, roleId: Int) : this() {
        this.userId = userId
        this.roleId = roleId
    }

    data class Key(var userId: Int = 0, var roleId: Int = 0) : Serializable
}
