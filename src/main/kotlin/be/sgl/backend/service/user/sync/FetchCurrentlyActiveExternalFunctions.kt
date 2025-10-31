package be.sgl.backend.service.user.sync

import be.sgl.backend.openapi.api.LedenApi
import be.sgl.backend.openapi.model.FunctieInstantie
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value

@ExternalUsecase
class FetchCurrentlyActiveExternalFunctions {

    @Value("\${organization.external.id}")
    private lateinit var externalOrganizationId: String
    @Autowired
    private lateinit var ledenApi: LedenApi

    fun execute(externalId: String): MutableList<FunctieInstantie> {
        return ledenApi.getLid(externalId).functies
            .filter { it.groep == externalOrganizationId && it.einde == null }
            // we don't want to consider this function since we can't remove nor assign it
            .filter { it.functie != FV_FUNCTION }
            .toMutableList()
    }

    companion object {
        const val FV_FUNCTION = "d5f75b320b812440010b812553d5032e"
    }
}