package be.sgl.backend.entity.calendar

import be.sgl.backend.entity.Address
import be.sgl.backend.entity.Auditable
import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
class CalendarItem : Auditable() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null
    var start = LocalDate.now()
    var end = LocalDate.now()
    lateinit var title: String
    @Lob
    lateinit var content: String
    var image: String? = null
    @OneToMany
    var calendars = mutableListOf<CalendarItem>()
    var closed = false
    @OneToOne
    var address: Address? = null
}