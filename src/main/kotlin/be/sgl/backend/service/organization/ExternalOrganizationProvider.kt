package be.sgl.backend.service.organization

import be.sgl.backend.dto.ExternalFunction
import be.sgl.backend.entity.Address
import be.sgl.backend.entity.organization.ContactMethod
import be.sgl.backend.entity.organization.ContactMethodType
import be.sgl.backend.entity.organization.Organization
import be.sgl.backend.entity.organization.OrganizationType
import be.sgl.backend.openapi.api.FunctiesApi
import be.sgl.backend.openapi.api.GroepenApi
import be.sgl.backend.openapi.model.Functie
import be.sgl.backend.openapi.model.Groep
import be.sgl.backend.service.exception.IncompleteConfigurationException
import be.sgl.backend.util.ForExternalOrganization
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
@ForExternalOrganization
class ExternalOrganizationProvider : InternalOrganizationProvider() {

    @Value("\${organization.external.id}")
    private lateinit var externalOrganizationId: String
    @Autowired
    private lateinit var groepenApi: GroepenApi
    @Autowired
    private lateinit var functiesApi: FunctiesApi

    @Cacheable("externalOwner")
    override fun getOwner(): Organization {
        var organization = organizationRepository.getByType(OrganizationType.OWNER)
        if (organization == null) {
            val group = groepenApi.getGroep(externalOrganizationId) ?: throw IncompleteConfigurationException("No valid external organization found!")
            organization = organizationRepository.save(translateGroup(group))
        }
        return organization
    }

    override fun getAllExternalFunctions(): List<ExternalFunction> {
        return functiesApi.getFuncties(externalOrganizationId)?.functies
            ?.map { ExternalFunction(it.id, it.beschrijving, it.type == Functie.TypeEnum.VERBOND) }
            ?: emptyList()
    }

    override fun getPaidExternalFunctions(): List<ExternalFunction> {
        return getAllExternalFunctions().filter { it.paid }
    }

    private fun translateGroup(group: Groep) = Organization().apply {
        name = group.naam
        type = OrganizationType.OWNER
        val externalAdress = group.adressen?.firstOrNull() ?: throw IncompleteConfigurationException("No external organization address configured!")
        address = Address().apply {
            externalId = externalAdress.id
            street = externalAdress.straat
            number = externalAdress.nummer
            subPremise = externalAdress.bus
            zipcode = externalAdress.postcode
            town = externalAdress.gemeente
            country = externalAdress.land
            description = externalAdress.omschrijving
            postalAdress = externalAdress.postadres
        }
        group.email?.let { contactMethods.add(ContactMethod(this, ContactMethodType.EMAIL, it)) }
        group.adressen[0].telefoon?.let { contactMethods.add(ContactMethod(this, ContactMethodType.MOBILE, it)) }
        description = group.vrijeInfo
    }
}