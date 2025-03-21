package be.sgl.backend.entity.user

import jakarta.persistence.*
import java.io.Serializable

@Entity
class SiblingRelation() : Serializable {
    @EmbeddedId
    private lateinit var id: SiblingRelationId
    @MapsId("userId")
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    lateinit var user: User
    @MapsId("siblingId")
    @ManyToOne
    @JoinColumn(name = "siblings_id", nullable = false)
    lateinit var sibling: User

    constructor(user: User, sibling: User) : this() {
        this.id = SiblingRelationId(user.id, sibling.id)
        this.user = user
        this.sibling = sibling
    }

    @Embeddable
    private data class SiblingRelationId(var userId: Int? = null, var siblingId: Int? = null) : Serializable
}