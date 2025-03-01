package be.sgl.backend.entity.calendar

import be.sgl.backend.entity.Auditable
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import java.time.LocalDate

@Entity
class CalendarPeriod : Auditable() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null
    @Column(nullable = false)
    lateinit var name: String
    @Column(nullable = false)
    lateinit var start: LocalDate
    @Column(nullable = false)
    lateinit var end: LocalDate
}