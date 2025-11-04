package be.sgl.backend.dto

import be.sgl.backend.entity.user.ContactRole
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Paths

class DTOTest {

    // PaymentUrl tests
    @Test
    fun `PaymentUrl should be created with url`() {
        val paymentUrl = PaymentUrl("https://example.com/payment")

        assertEquals("https://example.com/payment", paymentUrl.url)
    }

    @Test
    fun `PaymentUrl data class should support copy`() {
        val paymentUrl = PaymentUrl("https://example.com/payment")
        val copied = paymentUrl.copy(url = "https://example.com/new-payment")

        assertEquals("https://example.com/new-payment", copied.url)
    }

    @Test
    fun `PaymentUrl data class should support equality`() {
        val paymentUrl1 = PaymentUrl("https://example.com/payment")
        val paymentUrl2 = PaymentUrl("https://example.com/payment")

        assertEquals(paymentUrl1, paymentUrl2)
    }

    // RemoteFile tests
    @Test
    fun `RemoteFile should be created with fileName and directory`() {
        val remoteFile = RemoteFile("test.pdf", "/uploads")

        assertEquals("test.pdf", remoteFile.fileName)
        assertEquals("/uploads", remoteFile.directory)
    }

    @Test
    fun `RemoteFile should be created from File and Path`() {
        val file = File("test.pdf")
        val path = Paths.get("/uploads/test.pdf")

        val remoteFile = RemoteFile(file, path)

        assertEquals("test.pdf", remoteFile.fileName)
        assertEquals("/uploads/test.pdf", remoteFile.directory)
    }

    @Test
    fun `RemoteFile should be created from Path`() {
        val path = Paths.get("/uploads/test.pdf")

        val remoteFile = RemoteFile(path)

        assertEquals("test.pdf", remoteFile.fileName)
        assertEquals("/uploads", remoteFile.directory)
    }

    @Test
    fun `RemoteFile data class should support copy`() {
        val remoteFile = RemoteFile("test.pdf", "/uploads")
        val copied = remoteFile.copy(fileName = "new-test.pdf")

        assertEquals("new-test.pdf", copied.fileName)
        assertEquals("/uploads", copied.directory)
    }

    @Test
    fun `RemoteFile data class should support equality`() {
        val remoteFile1 = RemoteFile("test.pdf", "/uploads")
        val remoteFile2 = RemoteFile("test.pdf", "/uploads")

        assertEquals(remoteFile1, remoteFile2)
    }

    // ContactDTO tests
    @Test
    fun `ContactDTO should be created with all fields`() {
        val address = AddressDTO(1, "Main St", "123", null, "1000", "Brussels", "BE", null)
        val contact = ContactDTO(
            id = 1,
            name = "Doe",
            firstName = "John",
            role = ContactRole.FATHER,
            mobile = "0123456789",
            email = "john.doe@example.com",
            nis = "85073003328",
            address = address
        )

        assertEquals(1, contact.id)
        assertEquals("Doe", contact.name)
        assertEquals("John", contact.firstName)
        assertEquals(ContactRole.FATHER, contact.role)
        assertEquals("0123456789", contact.mobile)
        assertEquals("john.doe@example.com", contact.email)
        assertEquals("85073003328", contact.nis)
        assertEquals(address, contact.address)
    }

    @Test
    fun `ContactDTO should be created with nullable fields as null`() {
        val contact = ContactDTO(
            id = null,
            name = "Doe",
            firstName = "Jane",
            role = ContactRole.MOTHER,
            mobile = null,
            email = null,
            nis = null,
            address = null
        )

        assertNull(contact.id)
        assertEquals("Doe", contact.name)
        assertEquals("Jane", contact.firstName)
        assertEquals(ContactRole.MOTHER, contact.role)
        assertNull(contact.mobile)
        assertNull(contact.email)
        assertNull(contact.nis)
        assertNull(contact.address)
    }

    @Test
    fun `ContactDTO data class should support copy`() {
        val contact = ContactDTO(
            id = 1,
            name = "Doe",
            firstName = "John",
            role = ContactRole.FATHER,
            mobile = "0123456789",
            email = "john.doe@example.com",
            nis = null,
            address = null
        )

        val copied = contact.copy(firstName = "Jane", role = ContactRole.MOTHER)

        assertEquals(1, copied.id)
        assertEquals("Doe", copied.name)
        assertEquals("Jane", copied.firstName)
        assertEquals(ContactRole.MOTHER, copied.role)
    }

    @Test
    fun `ContactDTO data class should support equality`() {
        val contact1 = ContactDTO(
            id = 1,
            name = "Doe",
            firstName = "John",
            role = ContactRole.FATHER,
            mobile = "0123456789",
            email = "john.doe@example.com",
            nis = null,
            address = null
        )
        val contact2 = ContactDTO(
            id = 1,
            name = "Doe",
            firstName = "John",
            role = ContactRole.FATHER,
            mobile = "0123456789",
            email = "john.doe@example.com",
            nis = null,
            address = null
        )

        assertEquals(contact1, contact2)
    }

    @Test
    fun `ContactDTO with different roles should not be equal`() {
        val contact1 = ContactDTO(
            id = 1,
            name = "Doe",
            firstName = "John",
            role = ContactRole.FATHER,
            mobile = null,
            email = null,
            nis = null,
            address = null
        )
        val contact2 = ContactDTO(
            id = 1,
            name = "Doe",
            firstName = "John",
            role = ContactRole.MOTHER,
            mobile = null,
            email = null,
            nis = null,
            address = null
        )

        assertNotEquals(contact1, contact2)
    }
}
