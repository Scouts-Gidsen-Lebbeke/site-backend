package be.sgl.backend.service.organization

import be.sgl.backend.entity.organization.Organization
import be.sgl.backend.entity.organization.OrganizationType
import be.sgl.backend.repository.OrganizationRepository
import be.sgl.backend.service.exception.IncompleteConfigurationException
import be.sgl.backend.service.user.InternalOrganizationCondition
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Conditional
import org.springframework.stereotype.Service

@Service
@Conditional(InternalOrganizationCondition::class)
class InternalOrganizationProvider : OrganizationProvider {

    @Autowired
    protected lateinit var organizationRepository: OrganizationRepository

    override fun getOwner(): Organization {
        return organizationRepository.getByType(OrganizationType.OWNER)
            ?: throw IncompleteConfigurationException("No organization configured!")
    }

    override fun getCertifier(): Organization {
        return organizationRepository.getByType(OrganizationType.CERTIFIER)
            ?: throw IncompleteConfigurationException("No certifier configured!")
    }
}