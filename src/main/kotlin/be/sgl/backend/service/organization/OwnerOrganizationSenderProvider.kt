package be.sgl.backend.service.organization

import be.sgl.backend.entity.organization.OrganizationType
import be.sgl.backend.repository.organization.OrganizationRepository
import be.sgl.backend.shared.exception.IncompleteConfigurationException
import be.sgl.backend.shared.mail.DefaultSenderProvider
import org.springframework.stereotype.Component

@Component
class OwnerOrganizationSenderProvider(private val organizationRepository: OrganizationRepository) : DefaultSenderProvider {

    override fun defaultSenderEmail(): String {
        val organization = organizationRepository.getByType(OrganizationType.OWNER)
            ?: throw IncompleteConfigurationException("No organization configured!")
        return organization.getEmail()
            ?: throw IncompleteConfigurationException("No organization email configured!")
    }
}
