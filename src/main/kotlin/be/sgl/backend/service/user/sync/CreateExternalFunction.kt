package be.sgl.backend.service.user.sync

import be.sgl.backend.openapi.api.LedenApi
import be.sgl.backend.openapi.model.FunctieInstantie
import be.sgl.backend.openapi.model.Lid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import java.time.OffsetDateTime

@ExternalUsecase
class CreateExternalFunction {

    @Value("\${organization.external.id}")
    private lateinit var externalOrganizationId: String
    @Autowired
    private lateinit var ledenApi: LedenApi

    fun execute(externalId: String, functionId: String) {
        val externalFunction = FunctieInstantie().apply {
            groep = externalOrganizationId
            functie = functionId
            begin = OffsetDateTime.now()
        }
        val lidPatch = Lid().apply {
            functies = mutableListOf(externalFunction)
        }
        ledenApi.patchLid(externalId, true, lidPatch)
    }
}