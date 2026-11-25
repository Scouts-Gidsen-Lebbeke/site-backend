package be.sgl.backend.service.user.sync

import be.sgl.backend.access.AccessApi
import be.sgl.backend.organization.domain.RoleTestMother.adminRole
import be.sgl.backend.people.domain.UserTestMother.user
import be.sgl.backend.openapi.api.LedenApi
import be.sgl.backend.openapi.model.GroepsadminTestMother.EXTERNAL_ORGANIZATION_ID
import be.sgl.backend.openapi.model.GroepsadminTestMother.functie
import be.sgl.backend.openapi.model.GroepsadminTestMother.lid
import be.sgl.backend.openapi.model.Lid
import be.sgl.backend.organization.domain.RoleRepository
import be.sgl.backend.people.domain.UserRepository
import be.sgl.backend.service.user.inital.CheckForInitialRunExternal.Companion.VGA_FUNCTION
import be.sgl.backend.shared.util.anyValue
import be.sgl.backend.shared.util.argumentCaptor
import be.sgl.backend.shared.util.captureValue
import be.sgl.backend.shared.util.eqValue
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.security.authentication.TestingAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder

@ExtendWith(MockitoExtension::class)
class EndExternalFunctionsTest {

    @Mock
    lateinit var ledenApi: LedenApi
    @Mock
    lateinit var userRepository: UserRepository
    @Mock
    lateinit var roleRepository: RoleRepository
    @Mock
    lateinit var accessApi: AccessApi

    lateinit var testee: EndExternalFunctions

    @BeforeEach
    fun setUp() {
        testee = EndExternalFunctions(ledenApi, userRepository, roleRepository, accessApi, EXTERNAL_ORGANIZATION_ID)
    }

    @AfterEach
    fun clearSecurityContext() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `should only end the requested active functions of the organization`() {
        // given
        val captor = argumentCaptor<Lid>()
        given(ledenApi.getLid("ext-1")).willReturn(lid().functies(
            functie().functie("f1").build(),
            functie().functie("f1").groep("OTHER").build(),
            functie().functie("f2").ended().build(),
            functie().functie("keep").build()
        ).build())

        // when
        testee.execute("ext-1", "f1", "f2")

        // then
        then(ledenApi).should().patchLid(eqValue("ext-1"), eqValue(true), captureValue(captor))
        assertThat(captor.value.functies).hasSize(1)
        assertThat(captor.value.functies.single()).extracting("groep", "functie").containsExactly(EXTERNAL_ORGANIZATION_ID, "f1")
        assertThat(captor.value.functies.single().einde).isNotNull()
        assertThat(captor.value.adressen).isNull()
    }

    @Test
    fun `should not patch when none of the requested functions is active`() {
        // given
        given(ledenApi.getLid("ext-1")).willReturn(lid().functies(functie().functie("other").build()).build())

        // when
        testee.execute("ext-1", "f1", null)

        // then
        then(ledenApi).should(never()).patchLid(anyValue(), anyValue(), anyValue())
    }

    @Nested
    inner class RootAdminMigration {

        @Test
        fun `should neither migrate nor end VGA when nobody is authenticated`() {
            // given
            given(ledenApi.getLid("old")).willReturn(lid().functies(functie().functie(VGA_FUNCTION).build()).build())

            // when
            testee.execute("old", VGA_FUNCTION)

            // then
            then(ledenApi).should(never()).patchLid(anyValue(), anyValue(), anyValue())
            then(userRepository).shouldHaveNoInteractions()
        }

        @Test
        fun `should make the current user VGA when ending VGA of another member`() {
            // given
            SecurityContextHolder.getContext().authentication = TestingAuthenticationToken("me", null)
            val captor = argumentCaptor<Lid>()
            given(ledenApi.getLid("old")).willReturn(lid().functies(functie().functie(VGA_FUNCTION).build()).build())
            given(userRepository.getByUsername("me")).willReturn(user().externalId("mine").build())

            // when
            testee.execute("old", VGA_FUNCTION)

            // then
            then(ledenApi).should().patchLid(eqValue("mine"), eqValue(true), captureValue(captor))
            assertThat(captor.value.functies.single()).extracting("groep", "functie").containsExactly(EXTERNAL_ORGANIZATION_ID, VGA_FUNCTION)
            then(ledenApi).should(never()).patchLid(eqValue("old"), anyValue(), anyValue())
        }

        @Test
        fun `should make another admin VGA when the current user ends its own VGA function`() {
            // given
            SecurityContextHolder.getContext().authentication = TestingAuthenticationToken("me", null)
            val adminRole = adminRole().externalId(VGA_FUNCTION).build()
            val me = user().id(1).externalId("old").build()
            val other = user().id(2).externalId("other").build()
            given(ledenApi.getLid("old")).willReturn(lid().build())
            given(userRepository.getByUsername("me")).willReturn(me)
            given(roleRepository.getRoleByExternalIdEquals(VGA_FUNCTION)).willReturn(adminRole)
            given(accessApi.userIdsWithRole(adminRole.id!!)).willReturn(listOf(me.id!!, other.id!!))
            given(userRepository.findAllById(listOf(me.id!!, other.id!!))).willReturn(listOf(me, other))

            // when
            testee.execute("old", VGA_FUNCTION)

            // then
            then(ledenApi).should().patchLid(eqValue("other"), eqValue(true), anyValue())
        }

        @Test
        fun `should fail when the admin role for VGA does not exist`() {
            // given
            SecurityContextHolder.getContext().authentication = TestingAuthenticationToken("me", null)
            given(ledenApi.getLid("old")).willReturn(lid().build())
            given(userRepository.getByUsername("me")).willReturn(user().externalId("old").build())
            given(roleRepository.getRoleByExternalIdEquals(VGA_FUNCTION)).willReturn(null)

            // when / then
            assertThatThrownBy { testee.execute("old", VGA_FUNCTION) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("Admin role corresponding to VGA not found")
        }

        @Test
        fun `should fail when the current user ends its own VGA function and no other admin exists`() {
            // given
            SecurityContextHolder.getContext().authentication = TestingAuthenticationToken("me", null)
            val adminRole = adminRole().externalId(VGA_FUNCTION).build()
            val me = user().externalId("old").build()
            given(ledenApi.getLid("old")).willReturn(lid().build())
            given(userRepository.getByUsername("me")).willReturn(me)
            given(roleRepository.getRoleByExternalIdEquals(VGA_FUNCTION)).willReturn(adminRole)
            given(accessApi.userIdsWithRole(adminRole.id!!)).willReturn(listOf(me.id!!))
            given(userRepository.findAllById(listOf(me.id!!))).willReturn(listOf(me))

            // when / then
            assertThatThrownBy { testee.execute("old", VGA_FUNCTION) }.isInstanceOf(NoSuchElementException::class.java)
        }
    }
}
