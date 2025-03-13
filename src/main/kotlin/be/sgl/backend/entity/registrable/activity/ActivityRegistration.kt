package be.sgl.backend.entity.registrable.activity

import be.sgl.backend.entity.registrable.Registration
import be.sgl.backend.entity.user.User
import jakarta.persistence.Entity
import jakarta.persistence.ManyToOne
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.absoluteValue

@Entity
class ActivityRegistration() : Registration<Activity>() {
    @ManyToOne
    lateinit var user: User
    @ManyToOne
    lateinit var restriction: ActivityRestriction
    lateinit var start: LocalDateTime
    lateinit var end: LocalDateTime

    constructor(user: User, restriction: ActivityRestriction, price: Double, additionalData: String?) : this() {
        this.user = user
        this.restriction = restriction
        this.start = restriction.alternativeStart ?: restriction.activity.start
        this.end = restriction.alternativeEnd ?: restriction.activity.end
        this.price = price
        this.additionalData = additionalData
    }

    fun calculateDays(): Int {
        return ChronoUnit.DAYS.between(start, end).absoluteValue.toInt()
    }
}