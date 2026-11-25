package be.sgl.backend.organization.application

import be.sgl.backend.shared.address.AddressDTO
import be.sgl.backend.organization.web.ContactMethodDTO
import be.sgl.backend.organization.web.OrganizationDTO
import be.sgl.backend.shared.address.AddressTestMother.address
import be.sgl.backend.organization.domain.ContactMethodType
import be.sgl.backend.organization.domain.Organization
import be.sgl.backend.organization.domain.OrganizationTestMother.organization
import be.sgl.backend.organization.domain.OrganizationType
import be.sgl.backend.shared.exception.OrganizationNotFoundException
import be.sgl.backend.shared.address.AddressRepository
import be.sgl.backend.organization.domain.OrganizationRepository
import be.sgl.backend.shared.image.ImageService
import be.sgl.backend.shared.image.ImageService.ImageDirectory.ORGANIZATION
import be.sgl.backend.shared.image.ImageService.ImageDirectory.TEMPORARY
import be.sgl.backend.shared.util.anyValue
import be.sgl.backend.shared.util.argumentCaptor
import be.sgl.backend.shared.util.captureValue
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class OrganizationServiceTest {

    @Mock
    lateinit var organizationRepository: OrganizationRepository
    @Mock
    lateinit var organizationProvider: OrganizationProvider
    @Mock
    lateinit var addressRepository: AddressRepository
    @Mock
    lateinit var imageService: ImageService
    @InjectMocks
    lateinit var testee: OrganizationService

    private val id = 1

    @Test
    fun `should map the owner of the provider`() {
        // given
        val owner = organization().owner().build()
        given(organizationProvider.getOwner()).willReturn(owner)

        // when
        val result = testee.getOwner()

        // then
        assertThat(result).usingRecursiveComparison().isEqualTo(OrganizationDTO.of(owner))
    }

    @Test
    fun `should map the certifier of the provider`() {
        // given
        val certifier = organization().certifier().build()
        given(organizationProvider.getCertifier()).willReturn(certifier)

        // when
        val result = testee.getCertifier()

        // then
        assertThat(result).usingRecursiveComparison().isEqualTo(OrganizationDTO.of(certifier))
    }

    @Nested
    inner class CreateOrganization {

        @Test
        fun `should throw when an organization of the same type exists`() {
            // given
            val dto = OrganizationDTO.of(organization().owner().build())
            given(organizationRepository.getByType(OrganizationType.OWNER)).willReturn(organization().owner().build())

            // when / then
            assertThatThrownBy { testee.createOrganization(dto) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("This organization already exists!")
            then(organizationRepository).should(org.mockito.Mockito.never()).save(anyValue<Organization>())
        }

        @Test
        fun `should move the image when the organization has one`() {
            // given
            val image = "logo.png"
            val dto = OrganizationDTO.of(organization().image(image).build())
            given(organizationRepository.getByType(OrganizationType.OWNER)).willReturn(null)
            given(organizationRepository.save(anyValue<Organization>())).willReturn(organization().image(image).build())

            // when
            testee.createOrganization(dto)

            // then
            then(imageService).should().move(image, TEMPORARY, ORGANIZATION)
        }

        @Test
        fun `should store no image when the image is blank`() {
            // given
            val dto = OrganizationDTO.of(organization().image(" ").build())
            val captor = argumentCaptor<Organization>()
            given(organizationRepository.getByType(OrganizationType.OWNER)).willReturn(null)
            given(organizationRepository.save(anyValue<Organization>())).willReturn(organization().build())

            // when
            testee.createOrganization(dto)

            // then
            then(organizationRepository).should().save(captureValue(captor))
            assertThat(captor.value.image).isNull()
            then(imageService).shouldHaveNoInteractions()
        }
    }

    @Nested
    inner class UpdateOrganization {

        @Test
        fun `should throw when the organization does not exist`() {
            // given
            given(organizationRepository.findById(id)).willReturn(Optional.empty())

            // when / then
            assertThatThrownBy { testee.updateOrganization(id, OrganizationDTO.of(organization().build())) }
                .isInstanceOf(OrganizationNotFoundException::class.java)
                .hasMessage("Organization not found")
        }

        @Test
        fun `should update the name, kbo and description`() {
            // given
            val organization = organization().name("old").kbo("0000000000").build()
            val dto = OrganizationDTO.of(organization().name("new").kbo("0123456789").description("new description").build())
            given(organizationRepository.findById(id)).willReturn(Optional.of(organization))
            given(organizationRepository.save(organization)).willReturn(organization)

            // when
            testee.updateOrganization(id, dto)

            // then
            assertThat(organization.name).isEqualTo(dto.name)
            assertThat(organization.kbo).isEqualTo(dto.kbo)
            assertThat(organization.description).isEqualTo(dto.description)
        }

        @Test
        fun `should replace the address when the given address is new`() {
            // given
            val oldAddress = address().street("Oude straat").build()
            val organization = organization().address(oldAddress).build()
            val dto = OrganizationDTO.of(organization().address(address().street("Nieuwe straat").build()).build())
            given(organizationRepository.findById(id)).willReturn(Optional.of(organization))
            given(organizationRepository.save(organization)).willReturn(organization)

            // when
            testee.updateOrganization(id, dto)

            // then
            then(addressRepository).should().delete(oldAddress)
            assertThat(organization.address.street).isEqualTo("Nieuwe straat")
        }

        @Test
        fun `should keep the address when the given address already exists`() {
            // given
            val oldAddress = address().build()
            val organization = organization().address(oldAddress).build()
            val existingAddress = AddressDTO.of(address().build())
            existingAddress.id = 5
            val dto = OrganizationDTO.of(organization().build()).copy(address = existingAddress)
            given(organizationRepository.findById(id)).willReturn(Optional.of(organization))
            given(organizationRepository.save(organization)).willReturn(organization)

            // when
            testee.updateOrganization(id, dto)

            // then
            then(addressRepository).shouldHaveNoInteractions()
            assertThat(organization.address).isSameAs(oldAddress)
        }

        @Test
        fun `should replace the image when it changed`() {
            // given
            val organization = organization().image("old.png").build()
            val dto = OrganizationDTO.of(organization().image("new.png").build())
            given(organizationRepository.findById(id)).willReturn(Optional.of(organization))
            given(organizationRepository.save(organization)).willReturn(organization)

            // when
            testee.updateOrganization(id, dto)

            // then
            then(imageService).should().delete(ORGANIZATION, "old.png")
            then(imageService).should().move("new.png", TEMPORARY, ORGANIZATION)
            assertThat(organization.image).isEqualTo("new.png")
        }

        @Test
        fun `should add the image when there was none`() {
            // given
            val organization = organization().build()
            val dto = OrganizationDTO.of(organization().image("new.png").build())
            given(organizationRepository.findById(id)).willReturn(Optional.of(organization))
            given(organizationRepository.save(organization)).willReturn(organization)

            // when
            testee.updateOrganization(id, dto)

            // then
            then(imageService).should().move("new.png", TEMPORARY, ORGANIZATION)
            then(imageService).shouldHaveNoMoreInteractions()
        }

        @Test
        fun `should remove the image when the new image is blank`() {
            // given
            val organization = organization().image("old.png").build()
            val dto = OrganizationDTO.of(organization().image("").build())
            given(organizationRepository.findById(id)).willReturn(Optional.of(organization))
            given(organizationRepository.save(organization)).willReturn(organization)

            // when
            testee.updateOrganization(id, dto)

            // then
            then(imageService).should().delete(ORGANIZATION, "old.png")
            then(imageService).shouldHaveNoMoreInteractions()
            assertThat(organization.image).isNull()
        }

        @Test
        fun `should keep the image when it did not change`() {
            // given
            val organization = organization().image("same.png").build()
            val dto = OrganizationDTO.of(organization().image("same.png").build())
            given(organizationRepository.findById(id)).willReturn(Optional.of(organization))
            given(organizationRepository.save(organization)).willReturn(organization)

            // when
            testee.updateOrganization(id, dto)

            // then
            then(imageService).shouldHaveNoInteractions()
        }

        @Test
        fun `should replace the contact methods`() {
            // given
            val organization = organization().email("old@group.com").build()
            val dto = OrganizationDTO.of(organization().build())
                .copy(contactMethods = listOf(ContactMethodDTO(null, "new@group.com", ContactMethodType.EMAIL)))
            given(organizationRepository.findById(id)).willReturn(Optional.of(organization))
            given(organizationRepository.save(organization)).willReturn(organization)

            // when
            testee.updateOrganization(id, dto)

            // then
            assertThat(organization.getEmail()).isEqualTo("new@group.com")
        }
    }
}
