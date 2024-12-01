package be.sgl.backend.entity.calendar

import be.sgl.backend.entity.Auditable
import be.sgl.backend.entity.branch.Branch
import jakarta.persistence.*
import org.hibernate.annotations.SQLRestriction

@Entity
class Calendar : Auditable() {
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
    @OneToMany
    @SQLRestriction("calendar_id = {alias}.calendar_id OR calendar_period_id = {alias}.period_id")
    var items: MutableList<CalendarItem> = mutableListOf()
}