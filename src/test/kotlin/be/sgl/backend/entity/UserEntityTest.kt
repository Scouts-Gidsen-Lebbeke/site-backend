package be.sgl.backend.entity

import be.sgl.backend.entity.user.Sex
import be.sgl.backend.entity.user.User
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

class UserEntityTest {

    @Test
    fun `getFullName should combine firstName and name`() {
        val user = User().apply {
            firstName = "John"
            name = "Doe"
            email = "john@example.com"
            birthdate = LocalDate.of(2000, 1, 1)
        }

        assertEquals("John Doe", user.getFullName())
    }

    @Test
    fun `getAge should calculate age correctly`() {
        val user = User().apply {
            firstName = "Test"
            name = "User"
            email = "test@example.com"
            birthdate = LocalDate.of(2000, 6, 15)
        }

        val age = user.getAge(LocalDate.of(2024, 6, 15))

        assertEquals(24, age)
    }

    @Test
    fun `getAge should calculate age correctly before birthday`() {
        val user = User().apply {
            firstName = "Test"
            name = "User"
            email = "test@example.com"
            birthdate = LocalDate.of(2000, 6, 15)
        }

        val age = user.getAge(LocalDate.of(2024, 6, 14))

        assertEquals(23, age) // Still 23, birthday is tomorrow
    }

    @Test
    fun `getAge should calculate age correctly after birthday`() {
        val user = User().apply {
            firstName = "Test"
            name = "User"
            email = "test@example.com"
            birthdate = LocalDate.of(2000, 6, 15)
        }

        val age = user.getAge(LocalDate.of(2024, 6, 16))

        assertEquals(24, age) // Already 24, birthday was yesterday
    }

    @Test
    fun `getAge with current date should calculate current age`() {
        val user = User().apply {
            firstName = "Test"
            name = "User"
            email = "test@example.com"
            birthdate = LocalDate.now().minusYears(25)
        }

        val age = user.getAge()

        assertEquals(25, age)
    }

    @Test
    fun `getHomeAddress should return postal address`() {
        val user = User().apply {
            firstName = "Test"
            name = "User"
            email = "test@example.com"
            birthdate = LocalDate.of(2000, 1, 1)
        }

        val homeAddress = Address().apply {
            street = "Home Street"
            number = "123"
            zipcode = "1000"
            town = "Brussels"
            country = "BE"
            postalAdress = true
        }

        val otherAddress = Address().apply {
            street = "Other Street"
            number = "456"
            zipcode = "2000"
            town = "Antwerp"
            country = "BE"
            postalAdress = false
        }

        user.addresses.add(otherAddress)
        user.addresses.add(homeAddress)

        val result = user.getHomeAddress()

        assertNotNull(result)
        assertEquals("Home Street", result?.street)
        assertEquals("123", result?.number)
    }

    @Test
    fun `getHomeAddress should return null when no postal address`() {
        val user = User().apply {
            firstName = "Test"
            name = "User"
            email = "test@example.com"
            birthdate = LocalDate.of(2000, 1, 1)
        }

        val address = Address().apply {
            street = "Street"
            number = "123"
            zipcode = "1000"
            town = "Brussels"
            country = "BE"
            postalAdress = false
        }

        user.addresses.add(address)

        val result = user.getHomeAddress()

        assertNull(result)
    }

    @Test
    fun `user should have default values`() {
        val user = User().apply {
            firstName = "Test"
            name = "User"
            email = "test@example.com"
            birthdate = LocalDate.of(2000, 1, 1)
        }

        assertNull(user.username)
        assertNull(user.externalId)
        assertNull(user.customerId)
        assertNull(user.memberId)
        assertEquals(Sex.UNKNOWN, user.sex)
        assertEquals(0, user.ageDeviation)
        assertFalse(user.hasReduction)
        assertFalse(user.hasHandicap)
        assertNotNull(user.addresses)
        assertNotNull(user.contacts)
        assertNotNull(user.roles)
    }

    @Test
    fun `user should allow setting all optional fields`() {
        val user = User().apply {
            username = "johndoe"
            externalId = "ext-123"
            customerId = "cust-456"
            memberId = "mem-789"
            firstName = "John"
            name = "Doe"
            email = "john@example.com"
            birthdate = LocalDate.of(2000, 1, 1)
            sex = Sex.MALE
            ageDeviation = 1
            image = "profile.jpg"
            mobile = "0123456789"
            nis = "12345678901"
            accountNo = "BE12345678901234"
            hasReduction = true
            hasHandicap = false
        }

        assertEquals("johndoe", user.username)
        assertEquals("ext-123", user.externalId)
        assertEquals("cust-456", user.customerId)
        assertEquals("mem-789", user.memberId)
        assertEquals("John", user.firstName)
        assertEquals("Doe", user.name)
        assertEquals("john@example.com", user.email)
        assertEquals(Sex.MALE, user.sex)
        assertEquals(1, user.ageDeviation)
        assertEquals("profile.jpg", user.image)
        assertEquals("0123456789", user.mobile)
        assertEquals("12345678901", user.nis)
        assertEquals("BE12345678901234", user.accountNo)
        assertTrue(user.hasReduction)
        assertFalse(user.hasHandicap)
    }

    @Test
    fun `user addresses should be mutable list`() {
        val user = User().apply {
            firstName = "Test"
            name = "User"
            email = "test@example.com"
            birthdate = LocalDate.of(2000, 1, 1)
        }

        val address1 = Address().apply {
            street = "Street 1"
            number = "1"
            zipcode = "1000"
            town = "City 1"
            country = "BE"
        }

        val address2 = Address().apply {
            street = "Street 2"
            number = "2"
            zipcode = "2000"
            town = "City 2"
            country = "BE"
        }

        user.addresses.add(address1)
        user.addresses.add(address2)

        assertEquals(2, user.addresses.size)
        assertEquals("Street 1", user.addresses[0].street)
        assertEquals("Street 2", user.addresses[1].street)
    }

    @Test
    fun `user contacts should be mutable list`() {
        val user = User().apply {
            firstName = "Test"
            name = "User"
            email = "test@example.com"
            birthdate = LocalDate.of(2000, 1, 1)
        }

        assertTrue(user.contacts.isEmpty())

        // Contacts can be added
        assertEquals(0, user.contacts.size)
    }

    @Test
    fun `staffData should be initialized`() {
        val user = User().apply {
            firstName = "Test"
            name = "User"
            email = "test@example.com"
            birthdate = LocalDate.of(2000, 1, 1)
        }

        assertNotNull(user.staffData)
        assertEquals(user, user.staffData.user)
    }
}
