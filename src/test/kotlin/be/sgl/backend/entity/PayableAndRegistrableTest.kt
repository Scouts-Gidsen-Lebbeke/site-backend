package be.sgl.backend.entity

import be.sgl.backend.entity.registrable.Registrable
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class PayableAndRegistrableTest {

    private class TestPayable : Payable() {
        init {
            name = "Test Payable"
            description = "Test description"
        }
    }

    private class TestRegistrable : Registrable() {
        init {
            name = "Test Event"
            description = "Test description"
        }
    }

    @Test
    fun `Payable should initialize with default dates`() {
        val payable = TestPayable()

        assertNotNull(payable.open)
        assertNotNull(payable.closed)
    }

    @Test
    fun `Payable should allow setting name and description`() {
        val payable = TestPayable()

        assertEquals("Test Payable", payable.name)
        assertEquals("Test description", payable.description)
    }

    @Test
    fun `Payable should allow setting open and closed dates`() {
        val payable = TestPayable()
        val openDate = LocalDateTime.of(2024, 1, 1, 0, 0)
        val closedDate = LocalDateTime.of(2024, 12, 31, 23, 59)

        payable.open = openDate
        payable.closed = closedDate

        assertEquals(openDate, payable.open)
        assertEquals(closedDate, payable.closed)
    }

    @Test
    fun `Registrable should extend Payable`() {
        val registrable = TestRegistrable()

        assertTrue(registrable is Payable)
        assertNotNull(registrable.open)
        assertNotNull(registrable.closed)
    }

    @Test
    fun `Registrable should have start and end dates`() {
        val registrable = TestRegistrable()

        assertNotNull(registrable.start)
        assertNotNull(registrable.end)
    }

    @Test
    fun `Registrable should allow setting price and limits`() {
        val registrable = TestRegistrable()

        registrable.price = 50.0
        registrable.registrationLimit = 100

        assertEquals(50.0, registrable.price)
        assertEquals(100, registrable.registrationLimit)
    }

    @Test
    fun `Registrable should have default boolean flags`() {
        val registrable = TestRegistrable()

        assertTrue(registrable.cancellable)
        assertTrue(registrable.sendConfirmation)
        assertFalse(registrable.sendCompleteConfirmation)
        assertFalse(registrable.cancelled)
    }

    @Test
    fun `Registrable should allow setting boolean flags`() {
        val registrable = TestRegistrable()

        registrable.cancellable = false
        registrable.sendConfirmation = false
        registrable.sendCompleteConfirmation = true
        registrable.cancelled = true

        assertFalse(registrable.cancellable)
        assertFalse(registrable.sendConfirmation)
        assertTrue(registrable.sendCompleteConfirmation)
        assertTrue(registrable.cancelled)
    }

    @Test
    fun `Registrable readAdditionalData should return 0 when no rule`() {
        val registrable = TestRegistrable()
        registrable.additionalFormRule = null

        val result = registrable.readAdditionalData("{\"field\":\"value\"}")

        assertEquals(0.0, result)
    }

    @Test
    fun `Registrable readAdditionalData should return 0 when no data`() {
        val registrable = TestRegistrable()
        registrable.additionalFormRule = "price + 10"

        val result = registrable.readAdditionalData(null)

        assertEquals(0.0, result)
    }

    @Test
    fun `Registrable readAdditionalData should evaluate JSONata expression`() {
        val registrable = TestRegistrable()
        registrable.additionalFormRule = "nights * 10"

        val result = registrable.readAdditionalData("{\"nights\":\"3\"}")

        assertEquals(30.0, result)
    }

    @Test
    fun `Registrable readAdditionalData should coerce negative results to 0`() {
        val registrable = TestRegistrable()
        registrable.additionalFormRule = "nights - 10"

        val result = registrable.readAdditionalData("{\"nights\":\"2\"}")

        assertEquals(0.0, result)
    }

    @Test
    fun `Registrable readAdditionalData should handle complex JSONata expressions`() {
        val registrable = TestRegistrable()
        registrable.additionalFormRule = "adults * 20 + children * 10"

        val result = registrable.readAdditionalData("{\"adults\":\"2\",\"children\":\"3\"}")

        assertEquals(70.0, result)
    }

    @Test
    fun `Registrable should allow setting address`() {
        val registrable = TestRegistrable()
        val address = Address().apply {
            street = "Test Street"
            number = "123"
            zipcode = "1000"
            town = "Brussels"
            country = "BE"
        }

        registrable.address = address

        assertNotNull(registrable.address)
        assertEquals("Test Street", registrable.address?.street)
    }

    @Test
    fun `Registrable should allow setting communication options`() {
        val registrable = TestRegistrable()

        registrable.communicationCC = "cc@example.com"
        registrable.sendConfirmation = true
        registrable.sendCompleteConfirmation = true

        assertEquals("cc@example.com", registrable.communicationCC)
        assertTrue(registrable.sendConfirmation)
        assertTrue(registrable.sendCompleteConfirmation)
    }

    @Test
    fun `Registrable should allow setting additional form and rule`() {
        val registrable = TestRegistrable()
        val formJson = "{\"fields\":[{\"name\":\"nights\",\"type\":\"number\"}]}"
        val rule = "nights * 25"

        registrable.additionalForm = formJson
        registrable.additionalFormRule = rule

        assertEquals(formJson, registrable.additionalForm)
        assertEquals(rule, registrable.additionalFormRule)
    }
}
