package be.sgl.backend.entity.calendar

import be.sgl.backend.entity.Address
import be.sgl.backend.entity.Auditable
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
class CalendarItem : Auditable() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null
    var start = LocalDateTime.now()
    var end = LocalDateTime.now()
    lateinit var title: String
    @Lob
    lateinit var content: String
    var image: String? = null
    @ManyToOne
    var period: CalendarPeriod? = null
    @ManyToOne
    var calendar: Calendar? = null
    var closed = false
    @OneToOne
    var address: Address? = null
}