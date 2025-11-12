package be.sgl.backend.config

import be.sgl.backend.entity.Address
import be.sgl.backend.entity.organization.ContactMethod
import be.sgl.backend.entity.organization.ContactMethodType
import be.sgl.backend.entity.organization.Organization
import be.sgl.backend.entity.organization.OrganizationType
import be.sgl.backend.entity.user.Role.Companion.adminRole
import be.sgl.backend.openapi.api.GroepenApi
import be.sgl.backend.openapi.model.Groep
import be.sgl.backend.repository.OrganizationRepository
import be.sgl.backend.repository.RoleRepository
import be.sgl.backend.repository.user.UserRepository
import be.sgl.backend.service.exception.IncompleteConfigurationException
import be.sgl.backend.service.organization.OrganizationProvider
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class InitialRunChecker(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val organizationProvider: OrganizationProvider,
    private val groepenApi: GroepenApi,
    @Value("\${organization.external.id}")
    private val externalOrganizationId: String,
    private val organizationRepository: OrganizationRepository
) {

    private var isInitialRun = false

    @PostConstruct
    fun checkInitialRun() {
        isInitialRun = userRepository.count() == 0L
    }

    private fun createOrganizationIfNeeded() {
        try {
            organizationProvider.getOwner()
        } catch (e: IncompleteConfigurationException) {
            val group = groepenApi.getGroep(externalOrganizationId) ?: throw IncompleteConfigurationException("No valid external organization found!")
            organizationRepository.save(translateGroup(group))
        }
    }

    private fun createAdminRoleIfNeeded(externalSync: Boolean) {
        if (roleRepository.count() == 0L) {
            adminRole("Admin", VGA_FUNCTION.takeIf { externalSync }, AVGA_FUNCTION.takeIf { externalSync })
        }
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

    companion object {
        const val VGA_FUNCTION = "d5f75b320b812440010b812555970393"
        const val AVGA_FUNCTION = "8a95af9385ad9b880185c035ee740010"
    }
}