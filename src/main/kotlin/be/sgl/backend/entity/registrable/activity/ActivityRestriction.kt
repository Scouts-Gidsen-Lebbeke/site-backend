package be.sgl.backend.entity.registrable.activity

import be.sgl.backend.entity.Auditable
import be.sgl.backend.entity.branch.Branch
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import java.time.LocalDateTime

@Entity
class ActivityRestriction : Auditable() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null
    @ManyToOne
    lateinit var activity: Activity
    @ManyToOne
    lateinit var branch: Branch
    var name: String? = null
    var alternativeStart: LocalDateTime? = null
    var alternativeEnd: LocalDateTime? = null
    var alternativePrice: Double? = null
}