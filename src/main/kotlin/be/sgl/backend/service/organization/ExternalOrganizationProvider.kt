package be.sgl.backend.service.organization

import be.sgl.backend.dto.ExternalFunction
import be.sgl.backend.openapi.api.FunctiesApi
import be.sgl.backend.openapi.model.Functie
import be.sgl.backend.util.ForExternalOrganization
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
@ForExternalOrganization
class ExternalOrganizationProvider : InternalOrganizationProvider() {

    @Value("\${organization.external.id}")
    private lateinit var externalOrganizationId: String
    @Autowired
    private lateinit var functiesApi: FunctiesApi

    override fun getAllExternalFunctions(): List<ExternalFunction> {
        return functiesApi.getFuncties(externalOrganizationId)?.functies
            ?.map { ExternalFunction(it.id, it.beschrijving, it.type == Functie.TypeEnum.VERBOND) }
            ?: emptyList()
    }

    override fun getPaidExternalFunctions(): List<ExternalFunction> {
        return getAllExternalFunctions().filter { it.paid }
    }


}