package be.sgl.backend.entity.calendar

import be.sgl.backend.entity.Address
import be.sgl.backend.entity.Auditable
import jakarta.persistence.*
import java.time.LocalDate
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
    @Column(nullable = false, length = 1000)
    lateinit var content: String
    var image: String? = null
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "calendar_items",
        joinColumns = [JoinColumn(name = "item_id")],
        inverseJoinColumns = [JoinColumn(name = "calendar_id")]
    )
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

    companion object {
        fun defaultItem(sunday: LocalDate, calendar: Calendar): CalendarItem {
            return CalendarItem(sunday.atTime(14, 0), sunday.atTime(17, 0),
                "Nog in te vullen", "Nog in te vullen", calendar)
        }
    }
}