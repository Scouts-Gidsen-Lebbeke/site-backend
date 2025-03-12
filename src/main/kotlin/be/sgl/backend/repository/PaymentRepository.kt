package be.sgl.backend.repository

import be.sgl.backend.entity.Payment

fun interface PaymentRepository<T : Payment> {
    fun getByPaymentId(paymentId: String): T?
}