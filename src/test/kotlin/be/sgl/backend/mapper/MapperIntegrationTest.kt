package be.sgl.backend.mapper

import be.sgl.backend.config.TestConfigurations
import be.sgl.backend.dto.AddressDTO
import be.sgl.backend.dto.BranchDTO
import be.sgl.backend.entity.Address
import be.sgl.backend.entity.branch.Branch
import be.sgl.backend.entity.branch.BranchStatus
import be.sgl.backend.entity.user.Sex
import be.sgl.backend.util.IntegrationTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import

@IntegrationTest
@Import(TestConfigurations::class)
class MapperIntegrationTest {

    @Autowired
    private lateinit var addressMapper: AddressMapper

    @Autowired
    private lateinit var branchMapper: BranchMapper

    @Test
    fun `AddressMapper should map entity to DTO correctly`() {
        val address = Address().apply {
            street = "Test Street"
            number = "123"
            subPremise = "A"
            zipcode = "1000"
            town = "Brussels"
            country = "BE"
            description = "Main office"
        }

        val dto = addressMapper.toDto(address)

        assertEquals("Test Street", dto.street)
        assertEquals("123", dto.number)
        assertEquals("A", dto.subPremise)
        assertEquals("1000", dto.zipcode)
        assertEquals("Brussels", dto.town)
        assertEquals("BE", dto.country)
        assertEquals("Main office", dto.description)
    }

    @Test
    fun `AddressMapper should map DTO to entity correctly`() {
        val dto = AddressDTO(
            id = null,
            street = "DTO Street",
            number = "456",
            subPremise = "B",
            zipcode = "2000",
            town = "Antwerp",
            country = "BE",
            description = "Branch office"
        )

        val entity = addressMapper.toEntity(dto)

        assertEquals("DTO Street", entity.street)
        assertEquals("456", entity.number)
        assertEquals("B", entity.subPremise)
        assertEquals("2000", entity.zipcode)
        assertEquals("Antwerp", entity.town)
        assertEquals("BE", entity.country)
        assertEquals("Branch office", entity.description)
    }

    @Test
    fun `AddressMapper should handle null subPremise and description`() {
        val address = Address().apply {
            street = "Simple Street"
            number = "1"
            subPremise = null
            zipcode = "3000"
            town = "Leuven"
            country = "BE"
            description = null
        }

        val dto = addressMapper.toDto(address)

        assertNull(dto.subPremise)
        assertNull(dto.description)

        val backToEntity = addressMapper.toEntity(dto)

        assertNull(backToEntity.subPremise)
        assertNull(backToEntity.description)
    }

    @Test
    fun `BranchMapper should map entity to DTO correctly`() {
        val branch = Branch().apply {
            name = "Test Branch"
            email = "test@example.com"
            minimumAge = 6
            maximumAge = 8
            sex = Sex.MALE
            description = "Test description"
            law = "Test law"
            image = "test.jpg"
            status = BranchStatus.ACTIVE
            staffTitle = "Leader"
        }

        val dto = branchMapper.toDto(branch)

        assertEquals("Test Branch", dto.name)
        assertEquals("test@example.com", dto.email)
        assertEquals(6, dto.minimumAge)
        assertEquals(8, dto.maximumAge)
        assertEquals(Sex.MALE, dto.sex)
        assertEquals("Test description", dto.description)
        assertEquals("Test law", dto.law)
        assertEquals("test.jpg", dto.image)
        assertEquals(BranchStatus.ACTIVE, dto.status)
        assertEquals("Leader", dto.staffTitle)
    }

    @Test
    fun `BranchMapper should map DTO to entity correctly`() {
        val dto = BranchDTO(
            id = null,
            name = "DTO Branch",
            email = "dto@example.com",
            minimumAge = 9,
            maximumAge = 11,
            sex = Sex.FEMALE,
            description = "DTO description",
            law = "DTO law",
            image = "dto.jpg",
            status = BranchStatus.MEMBER,
            staffTitle = "Coach",
            staff = emptyList()
        )

        val entity = branchMapper.toEntity(dto)

        assertEquals("DTO Branch", entity.name)
        assertEquals("dto@example.com", entity.email)
        assertEquals(9, entity.minimumAge)
        assertEquals(11, entity.maximumAge)
        assertEquals(Sex.FEMALE, entity.sex)
        assertEquals("DTO description", entity.description)
        assertEquals("DTO law", entity.law)
        assertEquals("dto.jpg", entity.image)
        assertEquals(BranchStatus.MEMBER, entity.status)
        assertEquals("Coach", entity.staffTitle)
    }

    @Test
    fun `BranchMapper toBaseDto should map correctly`() {
        val branch = Branch().apply {
            name = "Base Branch"
            email = "base@example.com"
            minimumAge = 6
            maximumAge = 8
            sex = Sex.UNKNOWN
            description = "Base description"
            law = "Base law"
            image = "base.jpg"
            status = BranchStatus.ACTIVE
            staffTitle = "Leader"
        }

        val baseDto = branchMapper.toBaseDto(branch)

        assertEquals("Base Branch", baseDto.name)
        assertEquals("Base description", baseDto.description)
        assertEquals("base.jpg", baseDto.image)
        assertEquals(6, baseDto.minimumAge)
        assertEquals(8, baseDto.maximumAge)
        assertEquals(Sex.UNKNOWN, baseDto.sex)
    }

    @Test
    fun `mappers should be thread-safe for concurrent operations`() {
        val addresses = (1..10).map { i ->
            Address().apply {
                street = "Street $i"
                number = "$i"
                zipcode = "${1000 + i}"
                town = "Town $i"
                country = "BE"
            }
        }

        val dtos = addresses.parallelStream()
            .map { addressMapper.toDto(it) }
            .toList()

        assertEquals(10, dtos.size)
        dtos.forEachIndexed { index, dto ->
            assertEquals("Street ${index + 1}", dto.street)
            assertEquals("${index + 1}", dto.number)
        }
    }

    @Test
    fun `mappers should preserve data integrity in round-trip conversion`() {
        val originalAddress = Address().apply {
            street = "Round Trip Street"
            number = "999"
            subPremise = "Z"
            zipcode = "9999"
            town = "Round Trip City"
            country = "BE"
            description = "Round trip test"
        }

        val dto = addressMapper.toDto(originalAddress)
        val backToEntity = addressMapper.toEntity(dto)

        assertEquals(originalAddress.street, backToEntity.street)
        assertEquals(originalAddress.number, backToEntity.number)
        assertEquals(originalAddress.subPremise, backToEntity.subPremise)
        assertEquals(originalAddress.zipcode, backToEntity.zipcode)
        assertEquals(originalAddress.town, backToEntity.town)
        assertEquals(originalAddress.country, backToEntity.country)
        assertEquals(originalAddress.description, backToEntity.description)
    }
}
