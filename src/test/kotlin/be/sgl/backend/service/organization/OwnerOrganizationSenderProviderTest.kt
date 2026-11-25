package be.sgl.backend.service.organization

import be.sgl.backend.entity.organization.OrganizationTestMother.organization
import be.sgl.backend.entity.organization.OrganizationType
import be.sgl.backend.repository.organization.OrganizationRepository
import be.sgl.backend.shared.exception.IncompleteConfigurationException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class OwnerOrganizationSenderProviderTest {

    @Mock
    lateinit var organizationRepository: OrganizationRepository
    @InjectMocks
    lateinit var testee: OwnerOrganizationSenderProvider

    @Test
    fun `should return the owner email when the owner has an email`() {
        // given
        val email = "info@sgl.be"
        given(organizationRepository.getByType(OrganizationType.OWNER)).willReturn(organization().owner().email(email).build())

        // when
        val sender = testee.defaultSenderEmail()

        // then
        assertThat(sender).isEqualTo(email)
    }

    @Test
    fun `should throw when no owner organization is configured`() {
        // given
        given(organizationRepository.getByType(OrganizationType.OWNER)).willReturn(null)

        // when / then
        assertThatThrownBy { testee.defaultSenderEmail() }
            .isInstanceOf(IncompleteConfigurationException::class.java)
            .hasMessageContaining("No organization configured!")
    }

    @Test
    fun `should throw when the owner has no email`() {
        // given
        given(organizationRepository.getByType(OrganizationType.OWNER)).willReturn(organization().owner().build())

        // when / then
        assertThatThrownBy { testee.defaultSenderEmail() }
            .isInstanceOf(IncompleteConfigurationException::class.java)
            .hasMessageContaining("No organization email configured!")
    }
}
