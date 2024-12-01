package be.sgl.backend.entity.membership

import be.sgl.backend.entity.Auditable
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import java.time.LocalDate

@Entity
class MembershipPeriod : Auditable() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null
    lateinit var start: LocalDate
    lateinit var end: LocalDate
    var price: Double = 0.0
}