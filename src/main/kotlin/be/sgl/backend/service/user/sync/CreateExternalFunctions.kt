package be.sgl.backend.service.user.sync

import be.sgl.backend.service.user.inital.InitialRunChecker.Companion.VGA_FUNCTION
import be.sgl.backend.openapi.api.LedenApi
import be.sgl.backend.openapi.model.FunctieInstantie
import be.sgl.backend.openapi.model.Lid
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import java.time.OffsetDateTime

@ExternalUsecase
class CreateExternalFunctions {

    private val logger = KotlinLogging.logger {}

    @Value("\${organization.external.id}")
    private lateinit var externalOrganizationId: String
    @Autowired
    private lateinit var ledenApi: LedenApi

    fun execute(externalId: String, vararg functionIds: String?) {
        // don't check if they exist, the internal roles should be correct and otherwise the api will throw an error
        val lidPatch = Lid().apply {
            // TODO: this VGA filtering will be incorrect on first assignment
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