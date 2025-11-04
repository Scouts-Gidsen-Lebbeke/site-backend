package be.sgl.backend.alert

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AlertCodeTest {

    @Test
    fun `AlertCode should have all expected values`() {
        val values = AlertCode.values()
        assertEquals(2, values.size)
        assertTrue(values.contains(AlertCode.NEW_USER_EXISTS_NO_MEMBERSHIP))
        assertTrue(values.contains(AlertCode.NEW_USER_EXISTS_PAID_MEMBERSHIP))
    }

    @Test
    fun `AlertCode valueOf should return correct code`() {
        assertEquals(AlertCode.NEW_USER_EXISTS_NO_MEMBERSHIP, AlertCode.valueOf("NEW_USER_EXISTS_NO_MEMBERSHIP"))
        assertEquals(AlertCode.NEW_USER_EXISTS_PAID_MEMBERSHIP, AlertCode.valueOf("NEW_USER_EXISTS_PAID_MEMBERSHIP"))
    }

    @Test
    fun `AlertCode ordinal should match declaration order`() {
        assertEquals(0, AlertCode.NEW_USER_EXISTS_NO_MEMBERSHIP.ordinal)
        assertEquals(1, AlertCode.NEW_USER_EXISTS_PAID_MEMBERSHIP.ordinal)
    }

    @Test
    fun `AlertCode should have correct name`() {
        assertEquals("NEW_USER_EXISTS_NO_MEMBERSHIP", AlertCode.NEW_USER_EXISTS_NO_MEMBERSHIP.name)
        assertEquals("NEW_USER_EXISTS_PAID_MEMBERSHIP", AlertCode.NEW_USER_EXISTS_PAID_MEMBERSHIP.name)
    }
}
