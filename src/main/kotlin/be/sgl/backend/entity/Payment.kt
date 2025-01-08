package be.sgl.backend.entity

import be.woutschoovaerts.mollie.data.payment.PaymentStatus
import jakarta.persistence.*

@MappedSuperclass
abstract class Payment : Auditable() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null
    @Enumerated(EnumType.STRING)
    var status: PaymentStatus = PaymentStatus.OPEN
    var price: Double = 0.0
    var paymentId: String? = null

    abstract fun getDescription(): String
}