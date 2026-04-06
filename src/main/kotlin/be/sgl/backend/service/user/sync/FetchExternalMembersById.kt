package be.sgl.backend.service.user.sync

import be.sgl.backend.dto.user.ExternalMember
import be.sgl.backend.openapi.api.LedenlijstApi
import be.sgl.backend.openapi.model.Criteria
import be.sgl.backend.openapi.model.Filter
import be.sgl.backend.openapi.model.Ledenlijst
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value

@ExternalUsecase
class FetchExternalMembersById(
    @Value("\${organization.external.id}")
    private val externalOrganizationId: String,
    private val ledenlijstApi: LedenlijstApi
) {

    private val logger = KotlinLogging.logger {}

    fun execute(): MutableMap<String, ExternalMember> {
        logger.info { "Fetching external members for $externalOrganizationId..." }
        val filter = Filter().apply { // at least one is required
            kolommen = listOf(EXTERNAL_FIRST_NAME, EXTERNAL_LAST_NAME, EXTERNAL_EMAIL)
            criteria = Criteria().apply {
                // we don't filter on functions, unknown functions should always be unlinked anyway
                groepen = listOf(externalOrganizationId)
            }
        }
        val memberIds = mutableMapOf<String, ExternalMember>()
        var filterResult: Ledenlijst
        var offset = 0
        do {
            filterResult = ledenlijstApi.postFilterNu(filter, "$offset")
            logger.info { "Got filter result ${filterResult.aantal + offset}/${filterResult.totaal}" }
            memberIds += filterResult.leden.associate { it.id to ExternalMember.fromExternal(
                it.waarden[EXTERNAL_FIRST_NAME], it.waarden[EXTERNAL_LAST_NAME], it.waarden[EXTERNAL_EMAIL], it.id) }
            offset += filterResult.aantal
        } while (offset < filterResult.totaal)
        logger.info { "Fully fetched ${memberIds.size} external members for $externalOrganizationId from filter result(s)" }
        return memberIds
    }

    companion object {
        private const val EXTERNAL_FIRST_NAME = "be.vvksm.groepsadmin.model.column.VoornaamColumn"
        private const val EXTERNAL_LAST_NAME = "be.vvksm.groepsadmin.model.column.AchternaamColumn"
        private const val EXTERNAL_EMAIL = "be.vvksm.groepsadmin.model.column.EmailColumn"
    }
}