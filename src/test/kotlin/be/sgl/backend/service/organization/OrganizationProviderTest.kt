package be.sgl.backend.service.organization

import be.sgl.backend.entity.organization.OrganizationTestMother.organization
import be.sgl.backend.entity.organization.OrganizationType
import be.sgl.backend.shared.exception.IncompleteConfigurationException
import be.sgl.backend.repository.organization.OrganizationRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class OrganizationProviderTest {

    @Mock
    lateinit var organizationRepository: OrganizationRepository
    @InjectMocks
    lateinit var testee: OrganizationProvider

    @Test
    fun `should return the owner when it is configured`() {
        // given
        val owner = organization().owner().build()
        given(organizationRepository.getByType(OrganizationType.OWNER)).willReturn(owner)

        // when
        val result = testee.getOwner()

        // then
        assertThat(result).isSameAs(owner)
    }

    @Test
    fun `should throw when no owner is configured`() {
        // given
        given(organizationRepository.getByType(OrganizationType.OWNER)).willReturn(null)

        // when / then
        assertThatThrownBy { testee.getOwner() }
            .isInstanceOf(IncompleteConfigurationException::class.java)
            .hasMessage("No organization configured!")
    }

    @Test
    fun `should return the certifier when it is configured`() {
        // given
        val certifier = organization().certifier().build()
        given(organizationRepository.getByType(OrganizationType.CERTIFIER)).willReturn(certifier)

        // when
        val result = testee.getCertifier()

        // then
        assertThat(result).isSameAs(certifier)
    }

    @Test
    fun `should throw when no certifier is configured`() {
        // given
        given(organizationRepository.getByType(OrganizationType.CERTIFIER)).willReturn(null)

        // when / then
        assertThatThrownBy { testee.getCertifier() }
            .isInstanceOf(IncompleteConfigurationException::class.java)
            .hasMessage("No certifier configured!")
    }
}
