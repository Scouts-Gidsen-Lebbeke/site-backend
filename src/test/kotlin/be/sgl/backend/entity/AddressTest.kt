package be.sgl.backend.entity

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AddressTest {

    @Test
    fun `getStreetAddress should format street and number correctly`() {
        val address = Address().apply {
            street = "Main Street"
            number = "123"
            zipcode = "1000"
            town = "Brussels"
            country = "BE"
        }

        assertEquals("Main Street 123", address.getStreetAdress())
    }

    @Test
    fun `getStreetAddress should include subPremise when present`() {
        val address = Address().apply {
            street = "Main Street"
            number = "123"
            subPremise = "A"
            zipcode = "1000"
            town = "Brussels"
            country = "BE"
        }

        assertEquals("Main Street 123A", address.getStreetAdress())
    }

    @Test
    fun `getStreetAddress should not include subPremise when null`() {
        val address = Address().apply {
            street = "Main Street"
            number = "123"
            subPremise = null
            zipcode = "1000"
            town = "Brussels"
            country = "BE"
        }

        assertEquals("Main Street 123", address.getStreetAdress())
    }

    @Test
    fun `toString should format complete address correctly`() {
        val address = Address().apply {
            street = "Main Street"
            number = "123"
            zipcode = "1000"
            town = "Brussels"
            country = "BE"
        }

        assertEquals("Main Street 123, 1000 Brussels (BE)", address.toString())
    }

    @Test
    fun `toString should format complete address with subPremise`() {
        val address = Address().apply {
            street = "Main Street"
            number = "123"
            subPremise = "B"
            zipcode = "1000"
            town = "Brussels"
            country = "BE"
        }

        assertEquals("Main Street 123B, 1000 Brussels (BE)", address.toString())
    }

    @Test
    fun `postalAddress should default to false`() {
        val address = Address().apply {
            street = "Main Street"
            number = "123"
            zipcode = "1000"
            town = "Brussels"
            country = "BE"
        }

        assertFalse(address.postalAdress)
    }

    @Test
    fun `postalAddress can be set to true`() {
        val address = Address().apply {
            street = "Main Street"
            number = "123"
            zipcode = "1000"
            town = "Brussels"
            country = "BE"
            postalAdress = true
        }

        assertTrue(address.postalAdress)
    }

    @Test
    fun `externalId can be set and retrieved`() {
        val address = Address().apply {
            street = "Main Street"
            number = "123"
            zipcode = "1000"
            town = "Brussels"
            country = "BE"
            externalId = "EXT-12345"
        }

        assertEquals("EXT-12345", address.externalId)
    }

    @Test
    fun `description can be set and retrieved`() {
        val address = Address().apply {
            street = "Main Street"
            number = "123"
            zipcode = "1000"
            town = "Brussels"
            country = "BE"
            description = "Main office"
        }

        assertEquals("Main office", address.description)
    }
}
