package be.sgl.backend.service.user.sync

import be.sgl.backend.service.user.inital.CheckForInitialRunExternal.Companion.VGA_FUNCTION
import be.sgl.backend.openapi.api.LedenApi
import be.sgl.backend.openapi.model.FunctieInstantie
import be.sgl.backend.openapi.model.Lid
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import java.time.OffsetDateTime

@ExternalUsecase
class CreateExternalFunctions(
    @Value("\${organization.external.id}")
    private val externalOrganizationId: String,
    private val ledenApi: LedenApi
) {

    private val logger = KotlinLogging.logger {}

    fun execute(externalId: String, vararg functionIds: String?) {
        // don't check if they exist, the internal roles should be correct and otherwise the api will throw an error
        val lidPatch = Lid().apply {
            // The VGA function is assumed to be assigned to the first user on initial run
            // every new admin assignment should only have effect by assigning its backup AVGA
            // VGA thus only needs to be considered when deleting the root admin role and syncing, not at creation
            functies = functionIds.filter { it != VGA_FUNCTION }.mapNotNull {
                logger.debug { "Creating external function $it..." }
                FunctieInstantie().apply {
                    groep = externalOrganizationId
                    functie = it
                    begin = OffsetDateTime.now()
                }
            }
        }
        if (lidPatch.functies.isNotEmpty()) {
            ledenApi.patchLid(externalId, true, lidPatch)
        }
    }
}