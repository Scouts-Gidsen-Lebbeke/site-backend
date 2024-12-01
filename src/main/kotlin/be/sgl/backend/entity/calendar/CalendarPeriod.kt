package be.sgl.backend.entity.calendar

import be.sgl.backend.entity.Auditable
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import java.time.LocalDateTime

@Entity
class CalendarPeriod : Auditable() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null
    lateinit var name: String
    var start = LocalDateTime.now()
    var end = LocalDateTime.now()
}