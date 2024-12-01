package be.sgl.backend.entity.user

import be.sgl.backend.entity.Auditable
import be.sgl.backend.entity.branch.Branch
import jakarta.persistence.*

@Entity
class StaffData : Auditable() {
    @Id
    val id: Int? = null
    @OneToOne
    @PrimaryKeyJoinColumn
    lateinit var user: User
    var totem: String? = null
    var about: String? = null
    @ElementCollection
    @CollectionTable(name = "user_branch_nicknames", joinColumns = [JoinColumn(name = "user_id")])
    @MapKeyJoinColumn(name = "branch_id")
    @Column(name = "nickname")
    var nicknames: Map<Branch, String> = mutableMapOf()
}