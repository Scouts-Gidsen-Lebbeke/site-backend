package be.sgl.backend.service.user.sync

import be.sgl.backend.openapi.api.LedenApi
import be.sgl.backend.openapi.model.Lid
import be.sgl.backend.service.MailService
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import java.time.OffsetDateTime

@ExternalUsecase
class RemoveAllExternalFunctions {

    private val logger = KotlinLogging.logger {}

    @Autowired
    private lateinit var fetchCurrentlyActiveExternalFunctions: FetchCurrentlyActiveExternalFunctions
    @Autowired
    private lateinit var ledenApi: LedenApi
    @Autowired
    protected lateinit var mailService: MailService

    fun execute(externalId: String, externalMember: ExternalMember) {
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