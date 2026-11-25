package be.sgl.backend.organization.application

import be.sgl.backend.organization.domain.Organization
import be.sgl.backend.organization.domain.OrganizationType
import be.sgl.backend.organization.domain.OrganizationRepository
import be.sgl.backend.shared.exception.IncompleteConfigurationException
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class OrganizationProvider(
    private val organizationRepository: OrganizationRepository
) {

    @Cacheable("owner")
    fun getOwner(): Organization {
        return organizationRepository.getByType(OrganizationType.OWNER)
            ?: throw IncompleteConfigurationException("No organization configured!")
    }

    @Cacheable("certifier")
    fun getCertifier(): Organization {
        return organizationRepository.getByType(OrganizationType.CERTIFIER)
            ?: throw IncompleteConfigurationException("No certifier configured!")
    }
}