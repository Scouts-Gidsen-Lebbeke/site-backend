package be.sgl.backend.service.user.sync

import be.sgl.backend.service.user.inital.CheckForInitialRunExternal.Companion.VGA_FUNCTION
import be.sgl.backend.openapi.api.LedenApi
import be.sgl.backend.openapi.model.FunctieInstantie
import be.sgl.backend.openapi.model.Lid
import be.sgl.backend.repository.RoleRepository
import be.sgl.backend.repository.user.UserRepository
import be.sgl.backend.repository.user.UserRoleRepository
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.context.SecurityContextHolder
import java.time.OffsetDateTime

@ExternalUsecase
class EndExternalFunctions(
    private val ledenApi: LedenApi,
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val userRoleRepository: UserRoleRepository,
    @Value("\${organization.external.id}")
    private val externalOrganizationId: String
) {

    private val logger = KotlinLogging.logger {}

    fun execute(externalId: String, vararg functionIds: String?) {
        val existingLid = ledenApi.getLid(externalId)
        if (functionIds.contains(VGA_FUNCTION)) {
            migrateRootAdmin(externalId)
        }
        val lidPatch = Lid().apply {
            functies = existingLid.functies
                .filter { it.groep == externalOrganizationId && it.functie in functionIds && it.einde == null && it.functie != VGA_FUNCTION }
                .map {
                    logger.debug { "Ending external function $it..." }
                    it.apply { einde = OffsetDateTime.now() }
                }
        }
        if (lidPatch.functies.isNotEmpty()) {
            ledenApi.patchLid(externalId, true, lidPatch)
        }
    }

    private fun migrateRootAdmin(externalIdOfOldRootAdmin: String) {
        val usernameOfCurrentUser = SecurityContextHolder.getContext().authentication?.name ?: return
        // We should make the current user VGA instead, which will automatically stop the external VGA role
        var externalIdOfNewRootAdmin = userRepository.getByUsername(usernameOfCurrentUser).externalId
        if (externalIdOfNewRootAdmin == externalIdOfOldRootAdmin) {
            // If the current user removes its own admin role, we should pick another admin
            val adminRole = roleRepository.getRoleByExternalIdEquals(VGA_FUNCTION)
                ?: throw IllegalStateException("Admin role corresponding to VGA not found")
            externalIdOfNewRootAdmin = userRoleRepository.findByRole(adminRole)
                .first { it.user.externalId != externalIdOfOldRootAdmin }.user.externalId
        }
        val vgaFunction = FunctieInstantie().apply {
            groep = externalOrganizationId
            functie = VGA_FUNCTION
            begin = OffsetDateTime.now()
        }
        val lidPatch = Lid().apply {
            functies = listOf(vgaFunction)
        }
        ledenApi.patchLid(externalIdOfNewRootAdmin, true, lidPatch)
    }
}