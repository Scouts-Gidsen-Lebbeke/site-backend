package be.sgl.backend.entity

import be.sgl.backend.entity.organization.ContactMethodType
import be.sgl.backend.entity.organization.OrganizationType
import be.sgl.backend.entity.user.ContactRole
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class OrganizationEnumTest {

    // ContactMethodType tests
    @Test
    fun `ContactMethodType should have all expected values`() {
        val values = ContactMethodType.values()
        assertEquals(7, values.size)
        assertTrue(values.contains(ContactMethodType.MOBILE))
        assertTrue(values.contains(ContactMethodType.EMAIL))
        assertTrue(values.contains(ContactMethodType.FACEBOOK))
        assertTrue(values.contains(ContactMethodType.INSTAGRAM))
        assertTrue(values.contains(ContactMethodType.WHATSAPP))
        assertTrue(values.contains(ContactMethodType.TIKTOK))
        assertTrue(values.contains(ContactMethodType.LINK))
    }

    @Test
    fun `ContactMethodType valueOf should return correct type`() {
        assertEquals(ContactMethodType.MOBILE, ContactMethodType.valueOf("MOBILE"))
        assertEquals(ContactMethodType.EMAIL, ContactMethodType.valueOf("EMAIL"))
        assertEquals(ContactMethodType.FACEBOOK, ContactMethodType.valueOf("FACEBOOK"))
        assertEquals(ContactMethodType.INSTAGRAM, ContactMethodType.valueOf("INSTAGRAM"))
        assertEquals(ContactMethodType.WHATSAPP, ContactMethodType.valueOf("WHATSAPP"))
        assertEquals(ContactMethodType.TIKTOK, ContactMethodType.valueOf("TIKTOK"))
        assertEquals(ContactMethodType.LINK, ContactMethodType.valueOf("LINK"))
    }

    @Test
    fun `ContactMethodType ordinal should match declaration order`() {
        assertEquals(0, ContactMethodType.MOBILE.ordinal)
        assertEquals(1, ContactMethodType.EMAIL.ordinal)
        assertEquals(2, ContactMethodType.FACEBOOK.ordinal)
        assertEquals(3, ContactMethodType.INSTAGRAM.ordinal)
        assertEquals(4, ContactMethodType.WHATSAPP.ordinal)
        assertEquals(5, ContactMethodType.TIKTOK.ordinal)
        assertEquals(6, ContactMethodType.LINK.ordinal)
    }

    // OrganizationType tests
    @Test
    fun `OrganizationType should have all expected values`() {
        val values = OrganizationType.values()
        assertEquals(2, values.size)
        assertTrue(values.contains(OrganizationType.OWNER))
        assertTrue(values.contains(OrganizationType.CERTIFIER))
    }

    @Test
    fun `OrganizationType valueOf should return correct type`() {
        assertEquals(OrganizationType.OWNER, OrganizationType.valueOf("OWNER"))
        assertEquals(OrganizationType.CERTIFIER, OrganizationType.valueOf("CERTIFIER"))
    }

    @Test
    fun `OrganizationType ordinal should match declaration order`() {
        assertEquals(0, OrganizationType.OWNER.ordinal)
        assertEquals(1, OrganizationType.CERTIFIER.ordinal)
    }

    // ContactRole tests
    @Test
    fun `ContactRole should have all expected values`() {
        val values = ContactRole.values()
        assertEquals(4, values.size)
        assertTrue(values.contains(ContactRole.MOTHER))
        assertTrue(values.contains(ContactRole.FATHER))
        assertTrue(values.contains(ContactRole.GUARDIAN))
        assertTrue(values.contains(ContactRole.RESPONSIBLE))
    }

    @Test
    fun `ContactRole valueOf should return correct role`() {
        assertEquals(ContactRole.MOTHER, ContactRole.valueOf("MOTHER"))
        assertEquals(ContactRole.FATHER, ContactRole.valueOf("FATHER"))
        assertEquals(ContactRole.GUARDIAN, ContactRole.valueOf("GUARDIAN"))
        assertEquals(ContactRole.RESPONSIBLE, ContactRole.valueOf("RESPONSIBLE"))
    }

    @Test
    fun `ContactRole ordinal should match declaration order`() {
        assertEquals(0, ContactRole.MOTHER.ordinal)
        assertEquals(1, ContactRole.FATHER.ordinal)
        assertEquals(2, ContactRole.GUARDIAN.ordinal)
        assertEquals(3, ContactRole.RESPONSIBLE.ordinal)
    }
}
