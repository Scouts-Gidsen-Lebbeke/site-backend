package be.sgl.backend.entity

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SimplifiedPaymentStatusTest {

    @Test
    fun `SimplifiedPaymentStatus should have PAID status`() {
        val status = SimplifiedPaymentStatus.PAID
        assertEquals("PAID", status.name)
    }

    @Test
    fun `SimplifiedPaymentStatus should have CANCELLED status`() {
        val status = SimplifiedPaymentStatus.CANCELLED
        assertEquals("CANCELLED", status.name)
    }

    @Test
    fun `SimplifiedPaymentStatus should have REFUNDED status`() {
        val status = SimplifiedPaymentStatus.REFUNDED
        assertEquals("REFUNDED", status.name)
    }

    @Test
    fun `SimplifiedPaymentStatus should have ONGOING status`() {
        val status = SimplifiedPaymentStatus.ONGOING
        assertEquals("ONGOING", status.name)
    }

    @Test
    fun `SimplifiedPaymentStatus valueOf should return correct status`() {
        assertEquals(SimplifiedPaymentStatus.PAID, SimplifiedPaymentStatus.valueOf("PAID"))
        assertEquals(SimplifiedPaymentStatus.CANCELLED, SimplifiedPaymentStatus.valueOf("CANCELLED"))
        assertEquals(SimplifiedPaymentStatus.REFUNDED, SimplifiedPaymentStatus.valueOf("REFUNDED"))
        assertEquals(SimplifiedPaymentStatus.ONGOING, SimplifiedPaymentStatus.valueOf("ONGOING"))
    }

    @Test
    fun `SimplifiedPaymentStatus values should return all statuses`() {
        val values = SimplifiedPaymentStatus.values()
        assertEquals(4, values.size)
        assertTrue(values.contains(SimplifiedPaymentStatus.PAID))
        assertTrue(values.contains(SimplifiedPaymentStatus.CANCELLED))
        assertTrue(values.contains(SimplifiedPaymentStatus.REFUNDED))
        assertTrue(values.contains(SimplifiedPaymentStatus.ONGOING))
    }

    @Test
    fun `SimplifiedPaymentStatus ordinal should match declaration order`() {
        assertEquals(0, SimplifiedPaymentStatus.PAID.ordinal)
        assertEquals(1, SimplifiedPaymentStatus.CANCELLED.ordinal)
        assertEquals(2, SimplifiedPaymentStatus.REFUNDED.ordinal)
        assertEquals(3, SimplifiedPaymentStatus.ONGOING.ordinal)
    }
}
