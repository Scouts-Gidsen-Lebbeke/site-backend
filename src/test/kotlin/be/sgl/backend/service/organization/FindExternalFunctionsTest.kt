package be.sgl.backend.service.organization

import be.sgl.backend.dto.role.ExternalFunction
import be.sgl.backend.openapi.api.FunctiesApi
import be.sgl.backend.openapi.model.Functie
import be.sgl.backend.openapi.model.Functies
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class FindExternalFunctionsTest {

    @Mock
    lateinit var functiesApi: FunctiesApi

    // the external organization id is a @Value constructor parameter, so the testee is built by hand
    lateinit var testee: FindExternalFunctions

    private val externalOrganizationId = "O1234G"

    private val federationFunction = Functie().id("1").beschrijving("Verbondsfunctie").type(Functie.TypeEnum.VERBOND)
    private val groupFunction = Functie().id("2").beschrijving("Groepsfunctie").type(Functie.TypeEnum.GROEP)
    private val untypedFunction = Functie().id("3").beschrijving("Zonder type")

    @BeforeEach
    fun createTestee() {
        testee = FindExternalFunctions(externalOrganizationId, functiesApi)
    }

    @Test
    fun `should map every function and mark federation functions as paid`() {
        // given
        given(functiesApi.getFuncties(externalOrganizationId))
            .willReturn(Functies().functies(listOf(federationFunction, groupFunction, untypedFunction)))

        // when
        val result = testee.execute(false)

        // then
        assertThat(result).containsExactly(
            ExternalFunction("1", "Verbondsfunctie", true),
            ExternalFunction("2", "Groepsfunctie", false),
            ExternalFunction("3", "Zonder type", false)
        )
    }

    @Test
    fun `should only return paid functions when only paid ones are requested`() {
        // given
        given(functiesApi.getFuncties(externalOrganizationId))
            .willReturn(Functies().functies(listOf(federationFunction, groupFunction)))

        // when
        val result = testee.execute(true)

        // then
        assertThat(result).containsExactly(ExternalFunction("1", "Verbondsfunctie", true))
    }

    @Test
    fun `should return no functions when the api returns nothing`() {
        // given
        given(functiesApi.getFuncties(externalOrganizationId)).willReturn(null)

        // when
        val result = testee.execute(false)

        // then
        assertThat(result).isEmpty()
    }

    @Test
    fun `should return no functions when the api response has no function list`() {
        // given
        given(functiesApi.getFuncties(externalOrganizationId)).willReturn(Functies())

        // when
        val result = testee.execute(false)

        // then
        assertThat(result).isEmpty()
    }
}
