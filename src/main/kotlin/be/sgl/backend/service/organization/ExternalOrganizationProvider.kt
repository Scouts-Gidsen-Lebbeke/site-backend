package be.sgl.backend.service.organization

import be.sgl.backend.entity.organization.ContactMethod
import be.sgl.backend.entity.organization.ContactMethodType
import be.sgl.backend.entity.organization.Organization
import be.sgl.backend.entity.organization.OrganizationType
import be.sgl.backend.service.exception.IncompleteConfigurationException
import be.sgl.backend.util.ForExternalOrganization
import be.sgl.backend.util.Groep
import be.sgl.backend.util.asAddress
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

@Service
@ForExternalOrganization
class ExternalOrganizationProvider : InternalOrganizationProvider() {

    @Autowired
    private lateinit var webClientBuilder: WebClient.Builder
    @Value("\${rest.ga.url}")
    private lateinit var restGAUrl: String
    @Value("\${organization.external.id}")
    private lateinit var externalOrganizationId: String

    override fun getOwner(): Organization {
        var organization = organizationRepository.getByType(OrganizationType.OWNER)
        if (organization == null) {
            val group = callWebClient().block() ?: throw IncompleteConfigurationException("No valid external organization found!")
            organization = organizationRepository.save(translateGroup(group))
        }
        return organization
    }

    private fun translateGroup(group: Groep) = Organization().apply {
        name = group.naam
        type = OrganizationType.OWNER
        address = group.adressen?.get(0)?.asAddress() ?: throw IncompleteConfigurationException("No external organization address configured!")
        group.email?.let { contactMethods.add(ContactMethod(this, ContactMethodType.EMAIL, it)) }
        group.adressen[0].telefoon?.let { contactMethods.add(ContactMethod(this, ContactMethodType.MOBILE, it)) }
        description = group.vrijeInfo
    }

    private fun callWebClient() = webClientBuilder
        .baseUrl(restGAUrl)
        .build()
        .get()
        .uri { it.path("/groep/{id}").build(externalOrganizationId) }
        .retrieve()
        .bodyToMono(Groep::class.java)
}