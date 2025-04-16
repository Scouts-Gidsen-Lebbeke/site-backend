package be.sgl.backend.service.organization

import be.sgl.backend.dto.ExternalFunction
import be.sgl.backend.dto.Representative
import be.sgl.backend.entity.organization.Organization
import be.sgl.backend.entity.organization.OrganizationType
import be.sgl.backend.repository.OrganizationRepository
import be.sgl.backend.repository.user.UserRepository
import be.sgl.backend.service.ImageService
import be.sgl.backend.service.ImageService.ImageDirectory.ORGANIZATION
import be.sgl.backend.service.SettingService
import be.sgl.backend.service.exception.IncompleteConfigurationException
import be.sgl.backend.util.ForInternalOrganization
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
@ForInternalOrganization
class InternalOrganizationProvider : OrganizationProvider {

    @Autowired
    protected lateinit var organizationRepository: OrganizationRepository
    @Autowired
    private lateinit var userRepository: UserRepository
    @Autowired
    private lateinit var settingService: SettingService
    @Autowired
    private lateinit var imageService: ImageService

    @Cacheable("internalOwner")
    override fun getOwner(): Organization {
        return organizationRepository.getByType(OrganizationType.OWNER)
            ?: throw IncompleteConfigurationException("No organization configured!")
    }

    @Cacheable("representative")
    override fun getRepresentative(): Representative {
        val username = settingService.getRepresentativeUsername()
            ?: throw IncompleteConfigurationException("No representative configured for organization!")
        val user = userRepository.getByUsername(username)
        val title = settingService.getRepresentativeTitle()
        val sigantureFile = settingService.getSignatureFile()
            ?: throw IncompleteConfigurationException("No signature configured for organization!")
        val signature = imageService.get(sigantureFile, ORGANIZATION)
            ?: throw IncompleteConfigurationException("No valid signature configured for organization!")
        return Representative(user, title, signature)
    }

    @Cacheable("certifier")
    override fun getCertifier(): Organization {
        return organizationRepository.getByType(OrganizationType.CERTIFIER)
            ?: throw IncompleteConfigurationException("No certifier configured!")
    }

    override fun getAllExternalFunctions() = emptyList<ExternalFunction>()

    override fun getPaidExternalFunctions() = emptyList<ExternalFunction>()
}