package be.sgl.backend.service.user.sync

import be.sgl.backend.openapi.api.LedenApi
import be.sgl.backend.openapi.model.GroepsadminTestMother.EXTERNAL_ORGANIZATION_ID
import be.sgl.backend.openapi.model.GroepsadminTestMother.functie
import be.sgl.backend.openapi.model.GroepsadminTestMother.lid
import be.sgl.backend.service.user.sync.FetchCurrentlyActiveExternalFunctions.Companion.FV_FUNCTION
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class FetchCurrentlyActiveExternalFunctionsTest {

    @Mock
    lateinit var ledenApi: LedenApi

    lateinit var testee: FetchCurrentlyActiveExternalFunctions

    @BeforeEach
    fun setUp() {
        testee = FetchCurrentlyActiveExternalFunctions(EXTERNAL_ORGANIZATION_ID, ledenApi)
    }

    @Test
    fun `should only keep active functions of the organization except FV`() {
        // given
        val active = functie().functie("active").build()
        given(ledenApi.getLid("ext-1")).willReturn(lid().functies(
            active,
            functie().functie("ended").ended().build(),
            functie().functie("otherGroup").groep("OTHER").build(),
            functie().functie(FV_FUNCTION).build()
        ).build())

        // when
        val result = testee.execute("ext-1")

        // then
        assertThat(result).containsExactly(active)
    }

    @Test
    fun `should return an empty list when the member has no functions`() {
        // given
        given(ledenApi.getLid("ext-1")).willReturn(lid().build())

        // when
        val result = testee.execute("ext-1")

        // then
        assertThat(result).isEmpty()
    }
}
