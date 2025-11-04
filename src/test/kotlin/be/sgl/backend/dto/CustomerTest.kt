package be.sgl.backend.dto

import be.sgl.backend.entity.user.User
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class CustomerTest {

    @Test
    fun `Customer should be created with name email and id`() {
        val customer = Customer("John Doe", "john.doe@example.com", "CUST-123")

        assertEquals("John Doe", customer.name)
        assertEquals("john.doe@example.com", customer.email)
        assertEquals("CUST-123", customer.id)
    }

    @Test
    fun `Customer should be created with name and email without id`() {
        val customer = Customer("Jane Doe", "jane.doe@example.com")

        assertEquals("Jane Doe", customer.name)
        assertEquals("jane.doe@example.com", customer.email)
        assertNull(customer.id)
    }

    @Test
    fun `Customer should be created from User`() {
        val user = mock(User::class.java)
        `when`(user.getFullName()).thenReturn("John Smith")
        `when`(user.email).thenReturn("john.smith@example.com")
        `when`(user.customerId).thenReturn("CUST-456")

        val customer = Customer(user)

        assertEquals("John Smith", customer.name)
        assertEquals("john.smith@example.com", customer.email)
        assertEquals("CUST-456", customer.id)
    }

    @Test
    fun `Customer should be created from User with null customerId`() {
        val user = mock(User::class.java)
        `when`(user.getFullName()).thenReturn("Jane Smith")
        `when`(user.email).thenReturn("jane.smith@example.com")
        `when`(user.customerId).thenReturn(null)

        val customer = Customer(user)

        assertEquals("Jane Smith", customer.name)
        assertEquals("jane.smith@example.com", customer.email)
        assertNull(customer.id)
    }

    @Test
    fun `Customer data class should support copy`() {
        val customer = Customer("John Doe", "john.doe@example.com", "CUST-123")
        val copiedCustomer = customer.copy(name = "Jane Doe")

        assertEquals("Jane Doe", copiedCustomer.name)
        assertEquals("john.doe@example.com", copiedCustomer.email)
        assertEquals("CUST-123", copiedCustomer.id)
    }

    @Test
    fun `Customer data class should support equality`() {
        val customer1 = Customer("John Doe", "john.doe@example.com", "CUST-123")
        val customer2 = Customer("John Doe", "john.doe@example.com", "CUST-123")

        assertEquals(customer1, customer2)
    }

    @Test
    fun `Customer data class should support toString`() {
        val customer = Customer("John Doe", "john.doe@example.com", "CUST-123")

        val toString = customer.toString()

        assertTrue(toString.contains("John Doe"))
        assertTrue(toString.contains("john.doe@example.com"))
        assertTrue(toString.contains("CUST-123"))
    }

    @Test
    fun `Customer data class should support component destructuring`() {
        val customer = Customer("John Doe", "john.doe@example.com", "CUST-123")

        val (name, email, id) = customer

        assertEquals("John Doe", name)
        assertEquals("john.doe@example.com", email)
        assertEquals("CUST-123", id)
    }
}
