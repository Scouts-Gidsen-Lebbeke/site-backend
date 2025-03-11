package be.sgl.backend.repository

import be.sgl.backend.entity.Payment
import org.springframework.data.jpa.repository.JpaRepository

interface PaymentRepository<T : Payment> : JpaRepository<T, Int> {
    fun getByPaymentId(paymentId: String): T?
}