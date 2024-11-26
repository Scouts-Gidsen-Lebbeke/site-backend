package be.sgl.backend.entity

import be.woutschoovaerts.mollie.data.payment.PaymentStatus
import jakarta.persistence.*
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.absoluteValue

@Entity
class ActivityRegistration : Auditable() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null
    @ManyToOne
    lateinit var activity: Activity
    @ManyToOne
    lateinit var user: User
    @Enumerated(EnumType.STRING)
    var status: PaymentStatus = PaymentStatus.OPEN
    var price: Double = 0.0
    var paymentId: String? = null
    var present = false
    lateinit var start: LocalDateTime
    lateinit var end: LocalDateTime

    fun calculateDays(): Int {
        return ChronoUnit.DAYS.between(start, end).absoluteValue.toInt()
    }
}