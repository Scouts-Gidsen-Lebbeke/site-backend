package be.sgl.backend.entity.calendar

import be.sgl.backend.entity.Auditable
import be.sgl.backend.entity.branch.Branch
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
    val id: Int? = null
    @ManyToOne
    lateinit var period: CalendarPeriod
    @ManyToOne
    lateinit var branch: Branch
    @Lob
    var intro: String? = null
    @Lob
    var outro: String? = null
    @ManyToMany(fetch = FetchType.EAGER, cascade = [CascadeType.ALL])
    @JoinTable(
        name = "calendar_items",
        joinColumns = [JoinColumn(name = "calendar_id")],
        inverseJoinColumns = [JoinColumn(name = "item_id")]
    )
    var items: MutableList<CalendarItem> = mutableListOf()

    constructor(period: CalendarPeriod, branch: Branch) : this() {
        this.period = period
        this.branch = branch
    }
}