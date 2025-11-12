package be.sgl.backend.service.user.sync

import be.sgl.backend.service.user.inital.InitialRunChecker.Companion.VGA_FUNCTION
import be.sgl.backend.openapi.api.LedenApi
import be.sgl.backend.openapi.model.Lid
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import java.time.OffsetDateTime

@ExternalUsecase
class EndExternalFunctions {

    private val logger = KotlinLogging.logger {}

    @Value("\${organization.external.id}")
    private lateinit var externalOrganizationId: String
    @Autowired
    private lateinit var ledenApi: LedenApi

    fun execute(externalId: String, vararg functionIds: String?) {
        val existingLid = ledenApi.getLid(externalId)
        val lidPatch = Lid().apply {
            functies = existingLid.functies
                .filter { it.groep == externalOrganizationId && it.functie in functionIds && it.einde == null }
                .map {
                    if (it.functie == VGA_FUNCTION) {
                        TODO("VGA migration is not yet implemented, root admins can't be removed")
                        // We should make the current user VGA instead, which will automatically stop the external VGA role
                        // If the current user removes its own admin role, we should pick another admin
                    }
                    logger.debug { "Ending external function $it..." }
                    it.apply { einde = OffsetDateTime.now() }
                }
        }
        if (lidPatch.functies.isNotEmpty()) {
            ledenApi.patchLid(externalId, true, lidPatch)
        }
    }
}