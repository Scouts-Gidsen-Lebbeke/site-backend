package be.sgl.backend.calendar.domain

import be.sgl.backend.shared.persistence.Auditable
import jakarta.persistence.*

@Entity
@Table(
    uniqueConstraints = [UniqueConstraint(columnNames = ["period_id", "branch_id"])],
    indexes = [
        Index(name = "idx_period", columnList = "period_id")
    ]
)
class Calendar() : Auditable() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int? = null
    @ManyToOne
    lateinit var period: CalendarPeriod
    @Column(name = "branch_id", nullable = false)
    var branchId: Int = 0
    @Lob
    var intro: String? = null
    @Lob
    var outro: String? = null
    @ManyToMany(fetch = FetchType.EAGER, mappedBy = "calendars")
    var items: MutableList<CalendarItem> = mutableListOf()

    constructor(period: CalendarPeriod, branchId: Int) : this() {
        this.period = period
        this.branchId = branchId
    }
}