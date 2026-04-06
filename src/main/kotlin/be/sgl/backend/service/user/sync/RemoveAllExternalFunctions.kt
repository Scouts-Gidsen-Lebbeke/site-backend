package be.sgl.backend.service.user.sync

import be.sgl.backend.dto.user.ExternalMember
import be.sgl.backend.openapi.api.LedenApi
import be.sgl.backend.openapi.model.Lid
import be.sgl.backend.service.MailService
import mu.KotlinLogging
import java.time.OffsetDateTime

@ExternalUsecase
class RemoveAllExternalFunctions(
    private val fetchCurrentlyActiveExternalFunctions: FetchCurrentlyActiveExternalFunctions,
    private val ledenApi: LedenApi,
    private val mailService: MailService
) {

    private val logger = KotlinLogging.logger {}

    fun execute(externalMember: ExternalMember) {
        val externalId = externalMember.externalId ?: return
        logger.info { "Removing all external functions for user with external id $externalId..." }
        val lidPatch = Lid().apply {
            functies = fetchCurrentlyActiveExternalFunctions.execute(externalId).onEach {
                it.einde = OffsetDateTime.now()
            }
        }
        ledenApi.patchLid(externalId, true, lidPatch)
        val params = mapOf(
            "member" to externalMember.firstName
        )
        val email = externalMember.email
        if (email == null) {
            logger.info { "All external roles were removed, but no notification sent to user due to no known email." }
            return
        }
        mailService.builder()
            .to(email)
            .subject("Stopzetting lidmaatschap")
            .template("unsubscribe-confirmation.html", params)
            .send()
        logger.info { "All external functions were removed and user was notified via email." }
    }
}