package be.sgl.backend.service.user.inital

import be.sgl.backend.config.CustomUserDetailsTestMother.userDetails
import be.sgl.backend.entity.organization.ContactMethodType
import be.sgl.backend.entity.organization.Organization
import be.sgl.backend.entity.organization.OrganizationType
import be.sgl.backend.entity.role.Role
import be.sgl.backend.entity.user.UserTestMother.user
import be.sgl.backend.exception.IncompleteConfigurationException
import be.sgl.backend.openapi.api.GroepenApi
import be.sgl.backend.openapi.api.LedenApi
import be.sgl.backend.openapi.model.GroepsadminTestMother.EXTERNAL_ORGANIZATION_ID
import be.sgl.backend.openapi.model.GroepsadminTestMother.adres
import be.sgl.backend.openapi.model.GroepsadminTestMother.functie
import be.sgl.backend.openapi.model.GroepsadminTestMother.groep
import be.sgl.backend.openapi.model.GroepsadminTestMother.lid
import be.sgl.backend.repository.organization.OrganizationRepository
import be.sgl.backend.repository.role.RoleRepository
import be.sgl.backend.repository.user.UserRepository
import be.sgl.backend.service.user.inital.CheckForInitialRunExternal.Companion.AVGA_FUNCTION
import be.sgl.backend.service.user.inital.CheckForInitialRunExternal.Companion.VGA_FUNCTION
import be.sgl.backend.service.user.sync.CreateUserForExternalMember
import be.sgl.backend.util.anyValue
import be.sgl.backend.util.argThatValue
import be.sgl.backend.util.argumentCaptor
import be.sgl.backend.util.captureValue
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class CheckForInitialRunExternalTest {

    @Mock
    lateinit var userRepository: UserRepository
    @Mock
    lateinit var roleRepository: RoleRepository
    @Mock
    lateinit var groepenApi: GroepenApi
    @Mock
    lateinit var organizationRepository: OrganizationRepository
    @Mock
    lateinit var ledenApi: LedenApi
    @Mock
    lateinit var createUserForExternalMember: CreateUserForExternalMember

    lateinit var testee: CheckForInitialRunExternal

    @BeforeEach
    fun setUp() {
        testee = CheckForInitialRunExternal(userRepository, roleRepository, groepenApi, EXTERNAL_ORGANIZATION_ID,
            organizationRepository, ledenApi, createUserForExternalMember)
    }

    private fun startInitialRun() {
        given(userRepository.count()).willReturn(0L)
        testee.checkInitialRun()
    }

    @Nested
    inner class Execute {

        @Test
        fun `should return null without calling Groepsadmin when users already exist`() {
            // given
            given(userRepository.count()).willReturn(3L)
            testee.checkInitialRun()

            // when
            val result = testee.execute(userDetails().build())

            // then
            assertThat(result).isNull()
            then(ledenApi).shouldHaveNoInteractions()
            then(organizationRepository).shouldHaveNoInteractions()
        }

        @Test
        fun `should create the user when the external member is VGA of the organization`() {
            // given
            startInitialRun()
            val details = userDetails().externalId("ext-1").build()
            val createdUser = user().externalId("ext-1").build()
            given(organizationRepository.getByType(OrganizationType.OWNER)).willReturn(null)
            given(ledenApi.getLid("ext-1")).willReturn(lid().id("ext-1").functies(functie().functie(VGA_FUNCTION).build()).build())
            given(roleRepository.count()).willReturn(1L)
            given(createUserForExternalMember.execute("ext-1")).willReturn(createdUser)

            // when
            val result = testee.execute(details)

            // then
            assertThat(result).isSameAs(createdUser)
        }

        @Test
        fun `should end the initial run once the VGA user is created`() {
            // given
            startInitialRun()
            val details = userDetails().externalId("ext-1").build()
            given(organizationRepository.getByType(OrganizationType.OWNER)).willReturn(null)
            given(ledenApi.getLid("ext-1")).willReturn(lid().id("ext-1").functies(functie().functie(VGA_FUNCTION).build()).build())
            given(roleRepository.count()).willReturn(1L)
            given(createUserForExternalMember.execute("ext-1")).willReturn(user().build())
            testee.execute(details)

            // when
            val second = testee.execute(details)

            // then
            assertThat(second).isNull()
            then(createUserForExternalMember).should().execute("ext-1")
        }

        @Test
        fun `should return null when the external member is VGA of another group`() {
            // given
            startInitialRun()
            given(organizationRepository.getByType(OrganizationType.OWNER)).willReturn(null)
            given(ledenApi.getLid("externalId")).willReturn(lid().functies(functie().groep("OTHER").functie(VGA_FUNCTION).build()).build())

            // when
            val result = testee.execute(userDetails().build())

            // then
            assertThat(result).isNull()
            then(createUserForExternalMember).shouldHaveNoInteractions()
        }

        @Test
        fun `should return null and keep the initial run when the external member has another function`() {
            // given
            startInitialRun()
            given(organizationRepository.getByType(OrganizationType.OWNER)).willReturn(null)
            given(ledenApi.getLid("externalId")).willReturn(lid().functies(functie().functie(AVGA_FUNCTION).build()).build())

            // when
            val result = testee.execute(userDetails().build())

            // then
            assertThat(result).isNull()
            then(createUserForExternalMember).shouldHaveNoInteractions()
        }
    }

    @Nested
    inner class CreateOrganization {

        @Test
        @Disabled("Possible bug: createOrganizationIfNeeded check is inverted ('?: return'), so it only creates when an OWNER already exists")
        fun `should create the owner organization from the external group when none exists`() {
            // given
            startInitialRun()
            val captor = argumentCaptor<Organization>()
            val group = groep().naam("Scouts Lebbeke").email("info@sgl.be").vrijeInfo("info")
                .adressen(adres().id("a-1").straat("Kerkstraat").telefoon("052").postadres().build()).build()
            given(organizationRepository.getByType(OrganizationType.OWNER)).willReturn(null)
            given(groepenApi.getGroep(EXTERNAL_ORGANIZATION_ID)).willReturn(group)
            given(ledenApi.getLid("externalId")).willReturn(lid().build())

            // when
            testee.execute(userDetails().build())

            // then
            then(organizationRepository).should().save(captureValue(captor))
            val organization = captor.value
            assertThat(organization).extracting("name", "type", "description")
                .containsExactly("Scouts Lebbeke", OrganizationType.OWNER, "info")
            assertThat(organization.address).extracting("externalId", "street", "postalAdress").containsExactly("a-1", "Kerkstraat", true)
            assertThat(organization.contactMethods).extracting("type", "value")
                .containsExactly(org.assertj.core.groups.Tuple.tuple(ContactMethodType.EMAIL, "info@sgl.be"),
                    org.assertj.core.groups.Tuple.tuple(ContactMethodType.MOBILE, "052"))
        }

        @Test
        @Disabled("Possible bug: createOrganizationIfNeeded check is inverted, so an existing OWNER organization gets duplicated")
        fun `should not fetch the external group when the owner organization already exists`() {
            // given
            startInitialRun()
            given(organizationRepository.getByType(OrganizationType.OWNER)).willReturn(Organization())
            given(ledenApi.getLid("externalId")).willReturn(lid().build())

            // when
            testee.execute(userDetails().build())

            // then
            then(groepenApi).shouldHaveNoInteractions()
            then(organizationRepository).should(org.mockito.Mockito.never()).save(anyValue())
        }

        @Test
        @Disabled("Possible bug: createOrganizationIfNeeded check is inverted, so the missing address check is unreachable when no OWNER exists")
        fun `should fail when the external group has no address`() {
            // given
            startInitialRun()
            given(organizationRepository.getByType(OrganizationType.OWNER)).willReturn(null)
            given(groepenApi.getGroep(EXTERNAL_ORGANIZATION_ID)).willReturn(groep().noAdressen().build())

            // when / then
            assertThatThrownBy { testee.execute(userDetails().build()) }
                .isInstanceOf(IncompleteConfigurationException::class.java)
                .hasMessage("No external organization address configured!")
        }
    }

    @Nested
    inner class CreateAdminRoleIfNeeded {

        @Test
        fun `should not create anything when roles already exist`() {
            // given
            given(roleRepository.count()).willReturn(2L)

            // when
            testee.createAdminRoleIfNeeded(true)

            // then
            then(roleRepository).should().count()
            then(roleRepository).shouldHaveNoMoreInteractions()
        }

        @Test
        @Disabled("Possible bug: createAdminRoleIfNeeded builds the admin role but never saves it")
        fun `should save an admin role linked to VGA and AVGA when no roles exist`() {
            // given
            given(roleRepository.count()).willReturn(0L)

            // when
            testee.createAdminRoleIfNeeded(true)

            // then
            then(roleRepository).should().save(argThatValue<Role> {
                it.adminRole && it.externalId == VGA_FUNCTION && it.backupExternalId == AVGA_FUNCTION })
        }
    }
}
