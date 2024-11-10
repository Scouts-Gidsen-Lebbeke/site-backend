package be.sgl.backend.entity

import be.sgl.backend.entity.enum.BranchStatus
import jakarta.persistence.*

@Entity
class Branch : Auditable() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null
    lateinit var name: String
    lateinit var email: String
    var minimumAge = 0
    var maximumAge: Int? = null
    @Lob
    var description: String? = null
    @Lob
    var law: String? = null
    lateinit var image: String
    @Enumerated(EnumType.STRING)
    var status = BranchStatus.PASSIVE
    lateinit var staffTitle: String
}