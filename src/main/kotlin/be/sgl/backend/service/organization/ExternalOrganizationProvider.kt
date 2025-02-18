package be.sgl.backend.service.organization

import be.sgl.backend.entity.organization.ContactMethod
import be.sgl.backend.entity.organization.ContactMethodType
import be.sgl.backend.entity.organization.Organization
import be.sgl.backend.entity.organization.OrganizationType
import be.sgl.backend.service.exception.IncompleteConfigurationException
import be.sgl.backend.service.user.ExternalOrganizationCondition
import be.sgl.backend.util.Groep
import be.sgl.backend.util.asAddress
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Conditional
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

@Service
@Conditional(ExternalOrganizationCondition::class)
class ExternalOrganizationProvider : InternalOrganizationProvider() {

    @Autowired
    private lateinit var webClientBuilder: WebClient.Builder
    @Value("\${rest.ga.url}")
    private lateinit var restGAUrl: String
    @Value("\${organization.external.id}")
    private lateinit var externalOrganizationId: String

    override fun getOwner(): Organization {
        return organizationRepository.getByType(OrganizationType.OWNER)
            ?: translateGroup(callWebClient().block() ?: throw IncompleteConfigurationException("No organization configured!"))
    }

    private fun translateGroup(group: Groep) = Organization().apply {
        externalId = group.groepsnummer
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