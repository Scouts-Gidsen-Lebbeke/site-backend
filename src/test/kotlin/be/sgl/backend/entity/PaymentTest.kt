package be.sgl.backend.entity

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PaymentTest {

    private class TestPayment : Payment() {
        override fun getDescription(): String = "Test Payment Description"
    }

    @Test
    fun `Payment should initialize with default values`() {
        val payment = TestPayment()

        assertNull(payment.id)
        assertFalse(payment.paid)
        assertEquals(0.0, payment.price)
        assertNull(payment.paymentId)
    }

    @Test
    fun `markPaid should set paid to true`() {
        val payment = TestPayment()
        assertFalse(payment.paid)

        payment.markPaid()

        assertTrue(payment.paid)
    }

    @Test
    fun `price can be set and retrieved`() {
        val payment = TestPayment()
        payment.price = 42.50

        assertEquals(42.50, payment.price)
    }

    @Test
    fun `paymentId can be set and retrieved`() {
        val payment = TestPayment()
        payment.paymentId = "PAY-12345"

        assertEquals("PAY-12345", payment.paymentId)
    }

    @Test
    fun `getDescription should return correct description`() {
        val payment = TestPayment()

        assertEquals("Test Payment Description", payment.getDescription())
    }

    @Test
    fun `paid can be set directly`() {
        val payment = TestPayment()
        payment.paid = true

        assertTrue(payment.paid)
    }

    @Test
    fun `markPaid should be idempotent`() {
        val payment = TestPayment()
        payment.markPaid()
        payment.markPaid()

        assertTrue(payment.paid)
    }
}
