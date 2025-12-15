package be.sgl.backend.service.user.inital

import be.sgl.backend.config.CustomUserDetails
import be.sgl.backend.entity.Address
import be.sgl.backend.entity.organization.ContactMethod
import be.sgl.backend.entity.organization.ContactMethodType
import be.sgl.backend.entity.organization.Organization
import be.sgl.backend.entity.organization.OrganizationType
import be.sgl.backend.entity.role.Role.Companion.adminRole
import be.sgl.backend.entity.user.User
import be.sgl.backend.openapi.api.GroepenApi
import be.sgl.backend.openapi.api.LedenApi
import be.sgl.backend.openapi.model.Groep
import be.sgl.backend.repository.organization.OrganizationRepository
import be.sgl.backend.repository.role.RoleRepository
import be.sgl.backend.repository.user.UserRepository
import be.sgl.backend.exception.IncompleteConfigurationException
import be.sgl.backend.service.user.sync.CreateUserForExternalMember
import be.sgl.backend.service.user.sync.ExternalUsecase
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value

@ExternalUsecase
class CheckForInitialRunExternal(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val groepenApi: GroepenApi,
    @Value("\${organization.external.id}")
    private val externalOrganizationId: String,
    private val organizationRepository: OrganizationRepository,
    private val ledenApi: LedenApi,
    private val createUserForExternalMember: CreateUserForExternalMember
) : CheckForInitialRun {

    private var isInitialRun = false

    @PostConstruct
    fun checkInitialRun() {
        isInitialRun = userRepository.count() == 0L
    }

    override fun execute(userDetails: CustomUserDetails): User? {
        if (!isInitialRun) return null
        createOrganizationIfNeeded()
        val externalUser = ledenApi.getLid(userDetails.externalId)
        if (externalUser != null && externalUser.functies.any { it.groep == externalOrganizationId && it.functie == VGA_FUNCTION }) {
            createAdminRoleIfNeeded(true)
            val user = createUserForExternalMember.execute(externalUser.id)
            isInitialRun = false
            return user
        }
        return null
    }

    private fun createOrganizationIfNeeded() {
        organizationRepository.getByType(OrganizationType.OWNER) ?: return
        val group = groepenApi.getGroep(externalOrganizationId)
            ?: throw IncompleteConfigurationException("No valid external organization found!")
        organizationRepository.save(translateGroup(group))
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

    fun createAdminRoleIfNeeded(externalSync: Boolean) {
        if (roleRepository.count() == 0L) {
            adminRole("Admin", VGA_FUNCTION.takeIf { externalSync }, AVGA_FUNCTION.takeIf { externalSync })
        }
    }

    companion object {
        const val VGA_FUNCTION = "d5f75b320b812440010b812555970393"
        const val AVGA_FUNCTION = "8a95af9385ad9b880185c035ee740010"
    }
}