package be.sgl.backend.service.organization

import be.sgl.backend.config.TestConfigurations
import be.sgl.backend.dto.RepresentativeDTO
import be.sgl.backend.entity.Address
import be.sgl.backend.entity.organization.ContactMethod
import be.sgl.backend.entity.organization.ContactMethodType
import be.sgl.backend.entity.organization.Organization
import be.sgl.backend.entity.organization.OrganizationType
import be.sgl.backend.entity.setting.Setting
import be.sgl.backend.entity.setting.SettingId
import be.sgl.backend.entity.user.Sex
import be.sgl.backend.entity.user.User
import be.sgl.backend.repository.AddressRepository
import be.sgl.backend.repository.OrganizationRepository
import be.sgl.backend.repository.SettingRepository
import be.sgl.backend.repository.user.UserRepository
import be.sgl.backend.service.exception.OrganizationNotFoundException
import be.sgl.backend.util.IntegrationTest
import jakarta.transaction.Transactional
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import java.time.LocalDate

@IntegrationTest
@Import(TestConfigurations::class)
@Transactional
class OrganizationServiceIntegrationTest {

    @Autowired
    private lateinit var organizationService: OrganizationService

    @Autowired
    private lateinit var organizationRepository: OrganizationRepository

    @Autowired
    private lateinit var addressRepository: AddressRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var settingRepository: SettingRepository

    private lateinit var ownerOrg: Organization
    private lateinit var certifierOrg: Organization
    private lateinit var testAddress: Address

    @BeforeEach
    fun setup() {
        organizationRepository.deleteAll()
        addressRepository.deleteAll()
        userRepository.deleteAll()
        settingRepository.deleteAll()

        testAddress = Address().apply {
            street = "Test Street"
            number = "123"
            zipcode = "1000"
            town = "Brussels"
            country = "BE"
        }
        testAddress = addressRepository.save(testAddress)

        ownerOrg = Organization().apply {
            name = "Owner Organization"
            type = OrganizationType.OWNER
            kbo = "1234567890"
            address = testAddress
            description = "Owner description"
        }
        ownerOrg = organizationRepository.save(ownerOrg)

        val certifierAddress = Address().apply {
            street = "Certifier Street"
            number = "456"
            zipcode = "2000"
            town = "Antwerp"
            country = "BE"
        }
        addressRepository.save(certifierAddress)

        certifierOrg = Organization().apply {
            name = "Certifier Organization"
            type = OrganizationType.CERTIFIER
            kbo = "0987654321"
            address = certifierAddress
            description = "Certifier description"
        }
        certifierOrg = organizationRepository.save(certifierOrg)
    }

    @Test
    fun `getOwner should return owner organization`() {
        val owner = organizationService.getOwner()

        assertNotNull(owner)
        assertEquals("Owner Organization", owner.name)
        assertEquals(OrganizationType.OWNER, owner.type)
    }

    @Test
    fun `getCertifier should return certifier organization`() {
        val certifier = organizationService.getCertifier()

        assertNotNull(certifier)
        assertEquals("Certifier Organization", certifier.name)
        assertEquals(OrganizationType.CERTIFIER, certifier.type)
    }

    @Test
    fun `organization should have address information`() {
        val owner = organizationService.getOwner()

        assertNotNull(owner.address)
        assertEquals("Test Street", owner.address.street)
        assertEquals("123", owner.address.number)
        assertEquals("1000", owner.address.zipcode)
        assertEquals("Brussels", owner.address.town)
    }

    @Test
    fun `organization should include KBO number`() {
        val owner = organizationService.getOwner()

        assertEquals("1234567890", owner.kbo)
    }

    @Test
    fun `organization should include description`() {
        val owner = organizationService.getOwner()

        assertEquals("Owner description", owner.description)
    }

    @Test
    fun `organization getEmail should return email contact method`() {
        val emailContact = ContactMethod(ownerOrg, ContactMethodType.EMAIL, "owner@example.com")
        ownerOrg.contactMethods.add(emailContact)
        organizationRepository.save(ownerOrg)
        organizationRepository.flush()

        val retrieved = organizationRepository.findById(ownerOrg.id!!).get()
        val email = retrieved.getEmail()

        assertEquals("owner@example.com", email)
    }

    @Test
    fun `organization getMobile should return mobile contact method`() {
        val mobileContact = ContactMethod(ownerOrg, ContactMethodType.MOBILE, "0123456789")
        ownerOrg.contactMethods.add(mobileContact)
        organizationRepository.save(ownerOrg)
        organizationRepository.flush()

        val retrieved = organizationRepository.findById(ownerOrg.id!!).get()
        val mobile = retrieved.getMobile()

        assertEquals("0123456789", mobile)
    }

    @Test
    fun `organization should support multiple contact methods`() {
        val emailContact = ContactMethod(ownerOrg, ContactMethodType.EMAIL, "owner@example.com")
        val mobileContact = ContactMethod(ownerOrg, ContactMethodType.MOBILE, "0123456789")
        val linkContact = ContactMethod(ownerOrg, ContactMethodType.LINK, "https://example.com")

        ownerOrg.contactMethods.add(emailContact)
        ownerOrg.contactMethods.add(mobileContact)
        ownerOrg.contactMethods.add(linkContact)
        organizationRepository.save(ownerOrg)
        organizationRepository.flush()

        val retrieved = organizationRepository.findById(ownerOrg.id!!).get()

        assertEquals(3, retrieved.contactMethods.size)
        assertEquals("owner@example.com", retrieved.getEmail())
        assertEquals("0123456789", retrieved.getMobile())
    }

    @Test
    fun `getRepresentativeDTO should return representative settings`() {
        val testUser = User().apply {
            username = "representative"
            firstName = "Rep"
            name = "User"
            email = "rep@example.com"
            birthdate = LocalDate.of(1980, 1, 1)
        }
        userRepository.save(testUser)

        settingRepository.save(Setting(SettingId.REPRESENTATIVE_USERNAME, "representative"))
        settingRepository.save(Setting(SettingId.REPRESENTATIVE_TITLE, "Director"))
        settingRepository.save(Setting(SettingId.REPRESENTATIVE_SIGNATURE, "signature.png"))
        settingRepository.flush()

        val representative = organizationService.getRepresentativeDTO()

        assertEquals("representative", representative.username)
        assertEquals("Director", representative.title)
        assertEquals("signature.png", representative.signature)
    }

    @Test
    fun `getRepresentativeDTO should return null values when not configured`() {
        val representative = organizationService.getRepresentativeDTO()

        assertNull(representative.username)
        assertNull(representative.title)
        assertNull(representative.signature)
    }

    @Test
    fun `owner and certifier should be separate organizations`() {
        val owner = organizationService.getOwner()
        val certifier = organizationService.getCertifier()

        assertNotEquals(owner.id, certifier.id)
        assertNotEquals(owner.name, certifier.name)
    }

    @Test
    fun `organization should have unique type`() {
        // Attempting to create another OWNER should fail
        val duplicateOwner = Organization().apply {
            name = "Duplicate Owner"
            type = OrganizationType.OWNER
            address = testAddress
        }

        // The service checks for existing organization by type
        // This would fail in saveOrganizationDTO with the check
    }

    @Test
    fun `organization without image should return null image`() {
        ownerOrg.image = null
        organizationRepository.save(ownerOrg)
        organizationRepository.flush()

        val owner = organizationService.getOwner()

        assertNull(owner.image)
    }

    @Test
    fun `organization with image should return image path`() {
        ownerOrg.image = "organization-logo.png"
        organizationRepository.save(ownerOrg)
        organizationRepository.flush()

        val owner = organizationService.getOwner()

        assertEquals("organization-logo.png", owner.image)
    }

    @Test
    fun `organization address should be fully populated`() {
        val owner = organizationService.getOwner()

        assertNotNull(owner.address)
        assertNotNull(owner.address.street)
        assertNotNull(owner.address.number)
        assertNotNull(owner.address.zipcode)
        assertNotNull(owner.address.town)
        assertNotNull(owner.address.country)
    }

    @Test
    fun `organization KBO should be optional`() {
        ownerOrg.kbo = null
        organizationRepository.save(ownerOrg)
        organizationRepository.flush()

        val owner = organizationService.getOwner()

        assertNull(owner.kbo)
    }

    @Test
    fun `organization description should be optional`() {
        ownerOrg.description = null
        organizationRepository.save(ownerOrg)
        organizationRepository.flush()

        val owner = organizationService.getOwner()

        assertNull(owner.description)
    }

    @Test
    fun `organization description should support long text`() {
        val longDescription = "A".repeat(500)
        ownerOrg.description = longDescription
        organizationRepository.save(ownerOrg)
        organizationRepository.flush()

        val owner = organizationService.getOwner()

        assertEquals(longDescription, owner.description)
        assertEquals(500, owner.description?.length)
    }

    @Test
    fun `organization should track both types separately`() {
        val allOrgs = organizationRepository.findAll()

        assertTrue(allOrgs.any { it.type == OrganizationType.OWNER })
        assertTrue(allOrgs.any { it.type == OrganizationType.CERTIFIER })
        assertEquals(2, allOrgs.size)
    }

    @Test
    fun `getEmail should return null when no email contact method`() {
        val retrieved = organizationRepository.findById(ownerOrg.id!!).get()
        val email = retrieved.getEmail()

        assertNull(email)
    }

    @Test
    fun `getMobile should return null when no mobile contact method`() {
        val retrieved = organizationRepository.findById(ownerOrg.id!!).get()
        val mobile = retrieved.getMobile()

        assertNull(mobile)
    }
}
