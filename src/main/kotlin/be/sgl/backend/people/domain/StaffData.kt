package be.sgl.backend.people.domain

import be.sgl.backend.shared.persistence.Auditable
import jakarta.persistence.*

@Entity
class StaffData() : Auditable() {
    @Id
    val id: Int? = null
    @OneToOne
    @MapsId
    @JoinColumn(name = "id")
    lateinit var user: User
    var totem: String? = null
    var about: String? = null
    @ElementCollection
    @CollectionTable(name = "user_branch_nicknames", joinColumns = [JoinColumn(name = "user_id")])
    @MapKeyColumn(name = "branch_id")
    @Column(name = "nickname")
    var nicknames: Map<Int, String> = mutableMapOf()

    constructor(user: User) : this() {
        this.user = user
    }
}