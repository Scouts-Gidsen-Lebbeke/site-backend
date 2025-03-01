package be.sgl.backend.entity.calendar

import be.sgl.backend.entity.Address
import be.sgl.backend.entity.Auditable
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
class CalendarItem() : Auditable() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null
    @Column(nullable = false)
    lateinit var start: LocalDateTime
    @Column(nullable = false)
    lateinit var end: LocalDateTime
    @Column(nullable = false)
    lateinit var title: String
    @Lob
    @Column(nullable = false)
    lateinit var content: String
    var image: String? = null
    @ManyToMany(mappedBy = "items")
    var calendars = mutableListOf<Calendar>()
    var closed = false
    @OneToOne
    var address: Address? = null

    constructor(start: LocalDateTime, end: LocalDateTime, title: String, content: String, calendar: Calendar) : this() {
        this.start = start
        this.end = end
        this.title = title
        this.content = content
        this.calendars.add(calendar)
    }
}