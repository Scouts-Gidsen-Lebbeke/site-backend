package be.sgl.backend.entity.user

import be.sgl.backend.entity.branch.Branch
import jakarta.persistence.*

@Entity
class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int? = null
    var externalId: String? = null
    var backupExternalId: String? = null
    lateinit var name: String
//    @ManyToOne
//    var branch: Branch? = null
    @ManyToOne
    var staffBranch: Branch? = null
    var level = RoleLevel.GUEST
}