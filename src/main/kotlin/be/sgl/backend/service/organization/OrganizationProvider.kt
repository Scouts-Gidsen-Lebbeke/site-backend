package be.sgl.backend.service.organization

import be.sgl.backend.entity.organization.Organization
import be.sgl.backend.entity.organization.OrganizationType
import be.sgl.backend.repository.organization.OrganizationRepository
import be.sgl.backend.exception.IncompleteConfigurationException
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