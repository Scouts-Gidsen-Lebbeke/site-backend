package be.sgl.backend.service.organization

import be.sgl.backend.dto.role.ExternalFunction
import be.sgl.backend.openapi.api.FunctiesApi
import be.sgl.backend.openapi.model.Functie
import be.sgl.backend.util.Usecase
import org.springframework.beans.factory.annotation.Value

@Usecase
class FindExternalFunctions(
    @Value("\${organization.external.id}")
    private val externalOrganizationId: String,
    private val functiesApi: FunctiesApi
) {

    fun execute(onlyPaid: Boolean): List<ExternalFunction> {
        val externalFunctions = functiesApi.getFuncties(externalOrganizationId)?.functies
            ?.map { ExternalFunction(it.id, it.beschrijving, it.type == Functie.TypeEnum.VERBOND) }
            ?: emptyList()
        if (onlyPaid) {
            return externalFunctions.filter { it.paid }
        }
        return externalFunctions
    }
}