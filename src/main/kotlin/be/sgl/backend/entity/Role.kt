package be.sgl.backend.entity

import jakarta.persistence.*

@Entity
class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int? = null
    lateinit var name: String
    @ManyToOne
    var branch: Branch? = null
    @ManyToOne
    var staffBranch: Branch? = null
}