package be.sgl.backend.service.user.sync

import be.sgl.backend.openapi.api.LedenlijstApi
import be.sgl.backend.openapi.model.Filter
import be.sgl.backend.openapi.model.GroepsadminTestMother.EXTERNAL_ORGANIZATION_ID
import be.sgl.backend.openapi.model.GroepsadminTestMother.ledenlijst
import be.sgl.backend.openapi.model.GroepsadminTestMother.lijstLid
import be.sgl.backend.util.anyValue
import be.sgl.backend.util.argumentCaptor
import be.sgl.backend.util.captureValue
import be.sgl.backend.util.eqValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class FetchExternalMembersByIdTest {

    @Mock
    lateinit var ledenlijstApi: LedenlijstApi

    lateinit var testee: FetchExternalMembersById

    @BeforeEach
    fun setUp() {
        testee = FetchExternalMembersById(EXTERNAL_ORGANIZATION_ID, ledenlijstApi)
    }

    @Test
    fun `should map the members of a single page by external id`() {
        // given
        val captor = argumentCaptor<Filter>()
        given(ledenlijstApi.postFilterNu(captureValue(captor), eqValue("0"))).willReturn(ledenlijst()
            .leden(lijstLid().id("e1").voornaam("John").achternaam("Doe").email("john@doe.com").build()).build())

        // when
        val result = testee.execute()

        // then
        assertThat(result).containsOnlyKeys("e1")
        assertThat(result["e1"]).extracting("firstName", "name", "email", "externalId", "userId", "syncState")
            .containsExactly("John", "Doe", "john@doe.com", "e1", null, null)
        assertThat(captor.value.criteria.groepen).containsExactly(EXTERNAL_ORGANIZATION_ID)
        assertThat(captor.value.criteria.oudleden).isFalse()
    }

    @Test
    fun `should fetch the next page with the offset when the result is paged`() {
        // given
        given(ledenlijstApi.postFilterNu(anyValue(), eqValue("0")))
            .willReturn(ledenlijst().leden(lijstLid().id("e1").build()).totaal(2).build())
        given(ledenlijstApi.postFilterNu(anyValue(), eqValue("1")))
            .willReturn(ledenlijst().leden(lijstLid().id("e2").email(null).build()).totaal(2).build())

        // when
        val result = testee.execute()

        // then
        assertThat(result).containsOnlyKeys("e1", "e2")
        assertThat(result["e2"]!!.email).isNull()
    }

    @Test
    fun `should return an empty map when the organization has no members`() {
        // given
        given(ledenlijstApi.postFilterNu(anyValue(), eqValue("0"))).willReturn(ledenlijst().build())

        // when
        val result = testee.execute()

        // then
        assertThat(result).isEmpty()
        then(ledenlijstApi).should().postFilterNu(anyValue(), eqValue("0"))
        then(ledenlijstApi).shouldHaveNoMoreInteractions()
    }
}
