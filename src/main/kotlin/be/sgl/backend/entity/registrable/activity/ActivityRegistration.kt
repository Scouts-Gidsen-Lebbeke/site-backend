package be.sgl.backend.entity.registrable.activity

import be.sgl.backend.entity.registrable.Registration
import be.sgl.backend.entity.user.User
import jakarta.persistence.Entity
import jakarta.persistence.ManyToOne
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.absoluteValue

@Entity
class ActivityRegistration : Registration<Activity>() {
    @ManyToOne
    lateinit var user: User
    @ManyToOne
    lateinit var restriction: ActivityRestriction
    lateinit var start: LocalDateTime
    lateinit var end: LocalDateTime

    fun calculateDays(): Int {
        return ChronoUnit.DAYS.between(start, end).absoluteValue.toInt()
    }
}