package be.sgl.backend.service.user.sync

import be.sgl.backend.entity.branch.BranchTestMother.branch
import be.sgl.backend.entity.role.RoleTestMother.memberRole
import be.sgl.backend.entity.role.RoleTestMother.staffRole
import be.sgl.backend.entity.role.UserRoleTestMother.userRole
import be.sgl.backend.entity.user.UserTestMother.user
import be.sgl.backend.openapi.api.LedenApi
import be.sgl.backend.openapi.api.LidaanvragenApi
import be.sgl.backend.openapi.model.GroepsadminTestMother.EXTERNAL_ORGANIZATION_ID
import be.sgl.backend.openapi.model.GroepsadminTestMother.lid
import be.sgl.backend.openapi.model.Lid
import be.sgl.backend.repository.user.UserRepository
import be.sgl.backend.util.anyValue
import be.sgl.backend.util.argumentCaptor
import be.sgl.backend.util.captureValue
import be.sgl.backend.util.eqValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.isNull
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class AcceptExternalMembershipRequestTest {

    @Mock
    lateinit var userRepository: UserRepository
    @Mock
    lateinit var lidaanvragenApi: LidaanvragenApi
    @Mock
    lateinit var ledenApi: LedenApi
    @Mock
    lateinit var createExternalFunctions: CreateExternalFunctions

    lateinit var testee: AcceptExternalMembershipRequest

    @BeforeEach
    fun setUp() {
        testee = AcceptExternalMembershipRequest(EXTERNAL_ORGANIZATION_ID, userRepository, lidaanvragenApi, ledenApi, createExternalFunctions)
    }

    @Test
    fun `should not accept when the user has no role for external sync`() {
        // given
        val user = user().build()
        userRole().user(user).role(staffRole(branch().build()).externalId("staff").build()).build()

        // when
        val result = testee.execute(user, "r1")

        // then
        assertThat(result).isFalse()
        then(ledenApi).shouldHaveNoInteractions()
        then(lidaanvragenApi).shouldHaveNoInteractions()
    }

    @Test
    fun `should create the external member with the member function and accept the request`() {
        // given
        val user = user().firstName("John").build()
        userRole().user(user).role(memberRole(branch().build()).externalId("member").build()).build()
        val captor = argumentCaptor<Lid>()
        given(ledenApi.postLid(eqValue(false), captureValue(captor), isNull())).willReturn(lid().id("new-ext").build())

        // when
        val result = testee.execute(user, "r1")

        // then
        assertThat(result).isTrue()
        assertThat(user.externalId).isEqualTo("new-ext")
        assertThat(captor.value.vgagegevens.voornaam).isEqualTo("John")
        assertThat(captor.value.functies.single()).extracting("groep", "functie").containsExactly(EXTERNAL_ORGANIZATION_ID, "member")
        then(userRepository).should().save(user)
        then(lidaanvragenApi).should().deleteAanvraag("r1", "ja", false)
        then(createExternalFunctions).shouldHaveNoInteractions()
    }

    @Test
    fun `should add the backup function afterwards when the role has one`() {
        // given
        val user = user().build()
        userRole().user(user).role(memberRole(branch().build()).externalId("member").backupExternalId("backup").build()).build()
        given(ledenApi.postLid(eqValue(false), anyValue(), isNull())).willReturn(lid().id("new-ext").build())

        // when
        testee.execute(user, "r1")

        // then
        then(createExternalFunctions).should().execute("new-ext", "backup")
    }
}
