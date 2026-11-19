package be.sgl.backend.service.user.sync

import be.sgl.backend.entity.branch.BranchTestMother.branch
import be.sgl.backend.entity.role.RoleTestMother.memberRole
import be.sgl.backend.entity.user.Contact
import be.sgl.backend.entity.user.ContactRole
import be.sgl.backend.entity.user.Sex
import be.sgl.backend.entity.user.User
import be.sgl.backend.openapi.api.LedenApi
import be.sgl.backend.openapi.model.Contact.RolEnum
import be.sgl.backend.openapi.model.GroepsadminTestMother.EXTERNAL_ORGANIZATION_ID
import be.sgl.backend.openapi.model.GroepsadminTestMother.adres
import be.sgl.backend.openapi.model.GroepsadminTestMother.contact
import be.sgl.backend.openapi.model.GroepsadminTestMother.functie
import be.sgl.backend.openapi.model.GroepsadminTestMother.lid
import be.sgl.backend.openapi.model.PersoonsGegevens.GeslachtEnum
import be.sgl.backend.repository.role.RoleRepository
import be.sgl.backend.repository.user.ContactRepository
import be.sgl.backend.repository.user.UserRepository
import be.sgl.backend.util.anyValue
import be.sgl.backend.util.argumentCaptor
import be.sgl.backend.util.captureValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class CreateUserForExternalMemberTest {

    @Mock
    lateinit var ledenApi: LedenApi
    @Mock
    lateinit var userRepository: UserRepository
    @Mock
    lateinit var roleRepository: RoleRepository
    @Mock
    lateinit var contactRepository: ContactRepository

    lateinit var testee: CreateUserForExternalMember

    @BeforeEach
    fun setUp() {
        testee = CreateUserForExternalMember(ledenApi, userRepository, EXTERNAL_ORGANIZATION_ID, roleRepository, contactRepository)
    }

    private fun stubSave() {
        given(userRepository.save(anyValue<User>())).willAnswer { it.arguments[0] }
    }

    @Test
    fun `should translate the external member data into a new user`() {
        // given
        val birthdate = LocalDate.of(2010, 5, 5)
        given(ledenApi.getLid("ext-1")).willReturn(lid().id("ext-1").gebruikersnaam("jdoe").geslacht(GeslachtEnum.VROUW)
            .gsm("0470").rekeningnummer("BE00").beperking().verminderdlidgeld().geboortedatum(birthdate).lidnummer("123")
            .adressen(adres().id("a-1").straat("Kerkstraat").bus("b").postadres().build()).build())
        stubSave()

        // when
        val result = testee.execute("ext-1")

        // then
        assertThat(result).extracting("sex", "username", "mobile", "hasHandicap", "hasReduction", "accountNo", "birthdate", "memberId")
            .containsExactly(Sex.FEMALE, "jdoe", "0470", true, true, "BE00", birthdate, "123")
        assertThat(result.addresses.single()).extracting("externalId", "street", "subPremise", "postalAdress")
            .containsExactly("a-1", "Kerkstraat", "b", true)
    }

    @ParameterizedTest
    @CsvSource("MAN,MALE", "VROUW,FEMALE", "ANDERE,UNKNOWN", "G,UNKNOWN")
    fun `should translate the sex`(external: GeslachtEnum, expected: Sex) {
        // given
        given(ledenApi.getLid("ext-1")).willReturn(lid().geslacht(external).build())
        stubSave()

        // when
        val result = testee.execute("ext-1")

        // then
        assertThat(result.sex).isEqualTo(expected)
    }

    @Test
    fun `should only add roles for known functions of the organization`() {
        // given
        val role = memberRole(branch().build()).externalId("known").build()
        given(ledenApi.getLid("ext-1")).willReturn(lid().functies(
            functie().functie("known").build(),
            functie().functie("unknown").build(),
            functie().functie("known").groep("OTHER").build()
        ).build())
        given(roleRepository.getRoleByExternalIdEquals("known")).willReturn(role)
        given(roleRepository.getRoleByExternalIdEquals("unknown")).willReturn(null)
        stubSave()

        // when
        val result = testee.execute("ext-1")

        // then
        assertThat(result.roles).extracting("role").containsExactly(role)
        assertThat(result.roles.single().user).isSameAs(result)
    }

    @Test
    fun `should save the contacts linked to the user and their address`() {
        // given
        val captor = argumentCaptor<Contact>()
        given(ledenApi.getLid("ext-1")).willReturn(lid()
            .adressen(adres().id("a-1").build())
            .contacten(
                contact().voornaam("Mama").achternaam("Doe").rol(RolEnum.MOEDER).adres("a-1").gsm("0470").email("m@doe.com").build(),
                contact().rol(RolEnum.OPVOEDINGSVERANTWOORDELIJKE).adres("unknown").build()
            ).build())
        stubSave()

        // when
        val result = testee.execute("ext-1")

        // then
        then(contactRepository).should(org.mockito.Mockito.times(2)).save(captureValue(captor))
        val (mother, responsible) = captor.allValues
        assertThat(mother).extracting("firstName", "name", "role", "mobile", "email")
            .containsExactly("Mama", "Doe", ContactRole.MOTHER, "0470", "m@doe.com")
        assertThat(mother.user).isSameAs(result)
        assertThat(mother.address).isSameAs(result.addresses.single())
        assertThat(responsible.role).isEqualTo(ContactRole.RESPONSIBLE)
        assertThat(responsible.address).isNull()
    }

    @ParameterizedTest
    @CsvSource("VADER,FATHER", "MOEDER,MOTHER", "VOOGD,GUARDIAN", "OPVOEDINGSVERANTWOORDELIJKE,RESPONSIBLE")
    fun `should translate the contact role`(external: RolEnum, expected: ContactRole) {
        // given
        val captor = argumentCaptor<Contact>()
        given(ledenApi.getLid("ext-1")).willReturn(lid().contacten(contact().rol(external).build()).build())
        stubSave()

        // when
        testee.execute("ext-1")

        // then
        then(contactRepository).should().save(captureValue(captor))
        assertThat(captor.value.role).isEqualTo(expected)
    }

    @Test
    @Disabled("Possible bug: name, firstName and email are never copied from the external member (lateinit fields stay uninitialized, DB insert fails)")
    fun `should copy the name and email of the external member`() {
        // given
        given(ledenApi.getLid("ext-1")).willReturn(lid().voornaam("John").achternaam("Doe").email("john@doe.com").build())
        stubSave()

        // when
        val result = testee.execute("ext-1")

        // then
        assertThat(result).extracting("firstName", "name", "email").containsExactly("John", "Doe", "john@doe.com")
    }

    @Test
    @Disabled("Possible bug: the external id is never stored on the new user, so every sync recreates the same member")
    fun `should link the new user to the external member`() {
        // given
        given(ledenApi.getLid("ext-1")).willReturn(lid().id("ext-1").build())
        stubSave()

        // when
        val result = testee.execute("ext-1")

        // then
        assertThat(result.externalId).isEqualTo("ext-1")
    }

    @Test
    @Disabled("Possible bug: ended external functions (einde set) are still translated into roles")
    fun `should not add roles for ended functions`() {
        // given
        given(ledenApi.getLid("ext-1")).willReturn(lid().functies(functie().functie("known").ended().build()).build())
        org.mockito.Mockito.lenient().`when`(roleRepository.getRoleByExternalIdEquals("known"))
            .thenReturn(memberRole(branch().build()).externalId("known").build())
        stubSave()

        // when
        val result = testee.execute("ext-1")

        // then
        assertThat(result.roles).isEmpty()
    }
}
