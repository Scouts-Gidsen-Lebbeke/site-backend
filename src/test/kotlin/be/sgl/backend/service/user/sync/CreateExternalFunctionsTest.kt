package be.sgl.backend.service.user.sync

import be.sgl.backend.openapi.api.LedenApi
import be.sgl.backend.openapi.model.GroepsadminTestMother.EXTERNAL_ORGANIZATION_ID
import be.sgl.backend.openapi.model.Lid
import be.sgl.backend.service.user.inital.CheckForInitialRunExternal.Companion.AVGA_FUNCTION
import be.sgl.backend.service.user.inital.CheckForInitialRunExternal.Companion.VGA_FUNCTION
import be.sgl.backend.shared.util.argumentCaptor
import be.sgl.backend.shared.util.captureValue
import be.sgl.backend.shared.util.eqValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.then
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class CreateExternalFunctionsTest {

    @Mock
    lateinit var ledenApi: LedenApi

    lateinit var testee: CreateExternalFunctions

    @BeforeEach
    fun setUp() {
        testee = CreateExternalFunctions(EXTERNAL_ORGANIZATION_ID, ledenApi)
    }

    @Test
    fun `should patch the member with a started function per id in the organization`() {
        // given
        val captor = argumentCaptor<Lid>()

        // when
        testee.execute("ext-1", "f1", AVGA_FUNCTION)

        // then
        then(ledenApi).should().patchLid(eqValue("ext-1"), eqValue(true), captureValue(captor))
        val patch = captor.value
        assertThat(patch.functies).extracting("groep", "functie")
            .containsExactly(org.assertj.core.groups.Tuple.tuple(EXTERNAL_ORGANIZATION_ID, "f1"),
                org.assertj.core.groups.Tuple.tuple(EXTERNAL_ORGANIZATION_ID, AVGA_FUNCTION))
        assertThat(patch.functies).allSatisfy { assertThat(it.begin).isNotNull(); assertThat(it.einde).isNull() }
        assertThat(patch.adressen).isNull()
    }

    @Test
    fun `should never create the VGA function`() {
        // given
        val captor = argumentCaptor<Lid>()

        // when
        testee.execute("ext-1", VGA_FUNCTION, "f1")

        // then
        then(ledenApi).should().patchLid(eqValue("ext-1"), eqValue(true), captureValue(captor))
        assertThat(captor.value.functies).extracting("functie").containsExactly("f1")
    }

    @Test
    fun `should not call Groepsadmin when only the VGA function is given`() {
        // given

        // when
        testee.execute("ext-1", VGA_FUNCTION)

        // then
        then(ledenApi).shouldHaveNoInteractions()
    }

    @Test
    fun `should not call Groepsadmin when no functions are given`() {
        // given

        // when
        testee.execute("ext-1")

        // then
        then(ledenApi).shouldHaveNoInteractions()
    }

    @Test
    fun `should skip null function ids`() {
        // given
        val captor = argumentCaptor<Lid>()

        // when
        testee.execute("ext-1", "f1", null)

        // then
        then(ledenApi).should().patchLid(eqValue("ext-1"), eqValue(true), captureValue(captor))
        assertThat(captor.value.functies).extracting("functie").containsExactly("f1")
    }
}
