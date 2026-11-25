package be.sgl.backend.service.user.sync

import be.sgl.backend.access.AccessApi
import be.sgl.backend.organization.domain.BranchTestMother.branch
import be.sgl.backend.organization.domain.RoleTestMother.memberRole
import be.sgl.backend.people.domain.Contact
import be.sgl.backend.people.domain.ContactRole
import be.sgl.backend.shared.Sex
import be.sgl.backend.people.domain.User
import be.sgl.backend.openapi.api.LedenApi
import be.sgl.backend.openapi.model.Contact.RolEnum
import be.sgl.backend.openapi.model.GroepsadminTestMother.EXTERNAL_ORGANIZATION_ID
import be.sgl.backend.openapi.model.GroepsadminTestMother.adres
import be.sgl.backend.openapi.model.GroepsadminTestMother.contact
import be.sgl.backend.openapi.model.GroepsadminTestMother.functie
import be.sgl.backend.openapi.model.GroepsadminTestMother.lid
import be.sgl.backend.openapi.model.PersoonsGegevens.GeslachtEnum
import be.sgl.backend.organization.domain.RoleRepository
import be.sgl.backend.people.domain.ContactRepository
import be.sgl.backend.people.domain.UserRepository
import be.sgl.backend.shared.util.anyValue
import be.sgl.backend.shared.util.argumentCaptor
import be.sgl.backend.shared.util.captureValue
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.InOrder
import org.mockito.Mock
import org.mockito.Mockito.inOrder
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
    @Mock
    lateinit var accessApi: AccessApi
    @Mock
    lateinit var entityManager: EntityManager

    lateinit var testee: CreateUserForExternalMember

    @BeforeEach
    fun setUp() {
        testee = CreateUserForExternalMember(ledenApi, userRepository, EXTERNAL_ORGANIZATION_ID, roleRepository, contactRepository, accessApi, entityManager)
    }

    private fun stubSave() {
        given(userRepository.save(anyValue<User>())).willAnswer { (it.arguments[0] as User).apply { id = USER_ID } }
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
    fun `should only assign roles for known functions of the organization`() {
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
        testee.execute("ext-1")

        // then
        then(accessApi).should().assign(USER_ID, role.id!!)
        then(accessApi).shouldHaveNoMoreInteractions()
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
    fun `should not assign roles for ended functions`() {
        // given
        given(ledenApi.getLid("ext-1")).willReturn(lid().functies(functie().functie("known").ended().build()).build())
        org.mockito.Mockito.lenient().`when`(roleRepository.getRoleByExternalIdEquals("known"))
            .thenReturn(memberRole(branch().build()).externalId("known").build())
        stubSave()

        // when
        testee.execute("ext-1")

        // then
        then(accessApi).shouldHaveNoInteractions()
    }

    @Test
    fun `should flush and refresh the user when roles are assigned`() {
        // given
        val role = memberRole(branch().build()).externalId("known").build()
        given(ledenApi.getLid("ext-1")).willReturn(lid().functies(functie().functie("known").build()).build())
        given(roleRepository.getRoleByExternalIdEquals("known")).willReturn(role)
        stubSave()

        // when
        val result = testee.execute("ext-1")

        // then
        val order: InOrder = inOrder(accessApi, entityManager)
        order.verify(accessApi).assign(USER_ID, role.id!!)
        order.verify(entityManager).flush()
        order.verify(entityManager).refresh(result)
    }

    @Test
    fun `should not flush when no roles are assigned`() {
        // given
        given(ledenApi.getLid("ext-1")).willReturn(lid().build())
        stubSave()

        // when
        testee.execute("ext-1")

        // then
        then(entityManager).shouldHaveNoInteractions()
    }

    companion object {
        private const val USER_ID = 7
    }
}
