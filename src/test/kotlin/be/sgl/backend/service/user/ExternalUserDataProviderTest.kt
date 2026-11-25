package be.sgl.backend.service.user

import be.sgl.backend.entity.AddressTestMother.address
import be.sgl.backend.entity.branch.BranchTestMother.branch
import be.sgl.backend.entity.role.RoleTestMother.adminRole
import be.sgl.backend.entity.role.RoleTestMother.memberRole
import be.sgl.backend.entity.role.RoleTestMother.staffRole
import be.sgl.backend.entity.role.UserRole
import be.sgl.backend.entity.role.UserRoleTestMother.userRole
import be.sgl.backend.entity.user.ContactRole
import be.sgl.backend.entity.user.ContactTestMother.contact
import be.sgl.backend.entity.user.Sex
import be.sgl.backend.entity.user.UserTestMother.user
import be.sgl.backend.shared.exception.LocalizedException
import be.sgl.backend.openapi.api.LedenApi
import be.sgl.backend.openapi.api.LidaanvragenApi
import be.sgl.backend.openapi.model.Adres
import be.sgl.backend.openapi.model.Contact.RolEnum
import be.sgl.backend.openapi.model.GroepsadminTestMother.EXTERNAL_ORGANIZATION_ID
import be.sgl.backend.openapi.model.Lid
import be.sgl.backend.openapi.model.LidAanvraag
import be.sgl.backend.openapi.model.PersoonsGegevens.GeslachtEnum
import be.sgl.backend.repository.role.UserRoleRepository
import be.sgl.backend.repository.user.ContactRepository
import be.sgl.backend.repository.user.UserRepository
import be.sgl.backend.service.user.ExternalUserDataProvider.Companion.toDto
import be.sgl.backend.service.user.sync.CreateExternalFunctions
import be.sgl.backend.service.user.sync.EndExternalFunctions
import be.sgl.backend.shared.util.anyValue
import be.sgl.backend.shared.util.argumentCaptor
import be.sgl.backend.shared.util.captureValue
import be.sgl.backend.shared.util.eqValue
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.test.util.ReflectionTestUtils

@ExtendWith(MockitoExtension::class)
class ExternalUserDataProviderTest {

    @Mock
    lateinit var lidaanvragenApi: LidaanvragenApi
    @Mock
    lateinit var ledenApi: LedenApi
    @Mock
    lateinit var createExternalFunctions: CreateExternalFunctions
    @Mock
    lateinit var endExternalFunctions: EndExternalFunctions
    @Mock
    lateinit var contactRepository: ContactRepository
    @Mock
    lateinit var userRepository: UserRepository
    @Mock
    lateinit var userRoleRepository: UserRoleRepository

    lateinit var testee: ExternalUserDataProvider

    @BeforeEach
    fun setUp() {
        // the organization id is a @Value String, so @InjectMocks can't build this class
        testee = ExternalUserDataProvider(EXTERNAL_ORGANIZATION_ID, lidaanvragenApi, ledenApi, createExternalFunctions,
            endExternalFunctions, contactRepository)
        ReflectionTestUtils.setField(testee, "userRepository", userRepository)
        ReflectionTestUtils.setField(testee, "userRoleRepository", userRoleRepository)
    }

    @Nested
    inner class AcceptRegistration {

        @Test
        fun `should post a membership request with the user data and first address`() {
            // given
            val firstAddress = address().externalId("addr-1").street("First").build()
            val user = user().firstName("John").name("Doe").email("john@doe.com").sex(Sex.FEMALE).mobile("0470")
                .hasReduction().addresses(firstAddress, address().street("Second").build()).build()
            val captor = argumentCaptor<LidAanvraag>()

            // when
            testee.acceptRegistration(user)

            // then
            then(lidaanvragenApi).should().postNieuweAanvragen(captureValue(captor))
            val request = captor.value
            assertThat(request).extracting("groepsnummer", "voornaam", "achternaam", "geboortedatum", "email", "verminderdlidgeld")
                .containsExactly(EXTERNAL_ORGANIZATION_ID, "John", "Doe", user.birthdate, "john@doe.com", true)
            assertThat(request.persoonsgegevens.geslacht).isEqualTo(GeslachtEnum.VROUW)
            assertThat(request.persoonsgegevens.gsm).isEqualTo("0470")
            assertThat(request.adres).extracting("id", "straat", "status").containsExactly("addr-1", "First", Adres.StatusEnum.NORMAAL)
        }

        @Test
        fun `should fail when the user has no address`() {
            // given
            val user = user().build()

            // when / then
            assertThatThrownBy { testee.acceptRegistration(user) }.isInstanceOf(NoSuchElementException::class.java)
            then(lidaanvragenApi).shouldHaveNoInteractions()
        }
    }

    @Nested
    inner class UpdateUser {

        @Test
        fun `should save the user and patch the external member with its contacts`() {
            // given
            val user = user().externalId("ext-1").build()
            val contact = contact().user(user).address(address().externalId("a-1").build()).build()
            val captor = argumentCaptor<Lid>()
            given(userRepository.save(user)).willReturn(user)
            given(contactRepository.findContactsByUser(user)).willReturn(listOf(contact))

            // when
            val result = testee.updateUser(user)

            // then
            assertThat(result).isSameAs(user)
            then(userRepository).should().save(user)
            then(ledenApi).should().patchLid(eqValue("ext-1"), eqValue(true), captureValue(captor))
            assertThat(captor.value.contacten).extracting("voornaam").containsExactly(contact.firstName)
        }

        @Test
        fun `should only save the user when it is not linked to Groepsadmin`() {
            // given
            val user = user().build()
            given(userRepository.save(user)).willReturn(user)

            // when
            testee.updateUser(user)

            // then
            then(userRepository).should().save(user)
            then(ledenApi).shouldHaveNoInteractions()
        }

        @Test
        fun `should not save the user locally when Groepsadmin can't accept the update`() {
            // given
            val user = user().externalId("ext-1").build()
            val contact = contact().user(user).build()
            given(contactRepository.findContactsByUser(user)).willReturn(listOf(contact))

            // when / then
            assertThatThrownBy { testee.updateUser(user) }.isInstanceOf(LocalizedException::class.java)
            then(userRepository).shouldHaveNoInteractions()
            then(ledenApi).shouldHaveNoInteractions()
        }
    }

    @Nested
    inner class StartRole {

        @Test
        fun `should create the external functions when starting a staff role`() {
            // given
            val user = user().externalId("ext-1").build()
            val role = staffRole(branch().build()).externalId("func").backupExternalId("backup").build()
            given(userRoleRepository.save(anyValue<UserRole>())).willAnswer { it.arguments[0] }

            // when
            val result = testee.startRole(user, role)

            // then
            assertThat(result).isNotNull()
            then(createExternalFunctions).should().execute("ext-1", "func", "backup")
        }

        @Test
        fun `should skip external synchronization when starting a member role`() {
            // given
            val user = user().externalId("ext-1").build()
            val role = memberRole(branch().build()).build()
            given(userRoleRepository.save(anyValue<UserRole>())).willAnswer { it.arguments[0] }

            // when
            val result = testee.startRole(user, role)

            // then
            assertThat(result).isNotNull()
            then(createExternalFunctions).shouldHaveNoInteractions()
        }

        @Test
        fun `should skip external synchronization when the user already has the role`() {
            // given
            val user = user().externalId("ext-1").build()
            val role = adminRole().build()
            userRole().user(user).role(role).build()

            // when
            val result = testee.startRole(user, role)

            // then
            assertThat(result).isNull()
            then(createExternalFunctions).shouldHaveNoInteractions()
        }

        @Test
        fun `should not fail when the user has no external id`() {
            // given
            val user = user().build()
            val role = staffRole(branch().build()).externalId("func").build()
            given(userRoleRepository.save(anyValue<UserRole>())).willAnswer { it.arguments[0] }

            // when / then
            assertThatCode { testee.startRole(user, role) }.doesNotThrowAnyException()
        }
    }

    @Nested
    inner class EndRole {

        @Test
        fun `should delete the role and end the external functions`() {
            // given
            val user = user().externalId("ext-1").build()
            val userRole = userRole().user(user).role(staffRole(branch().build()).externalId("func").backupExternalId("backup").build()).build()

            // when
            testee.endRole(userRole)

            // then
            then(userRoleRepository).should().delete(userRole)
            then(endExternalFunctions).should().execute("ext-1", "func", "backup")
        }

        @Test
        fun `should also end the external functions when ending a member role`() {
            // given
            val user = user().externalId("ext-1").build()
            val userRole = userRole().user(user).role(memberRole(branch().build()).externalId("member").build()).build()

            // when
            testee.endRole(userRole)

            // then
            then(endExternalFunctions).should().execute("ext-1", "member", null)
        }

        @Test
        fun `should only delete the role when the user has no external id`() {
            // given
            val userRole = userRole().user(user().build()).role(staffRole(branch().build()).externalId("func").build()).build()

            // when
            testee.endRole(userRole)

            // then
            then(userRoleRepository).should().delete(userRole)
            then(endExternalFunctions).shouldHaveNoInteractions()
        }
    }

    @Nested
    inner class ToDto {

        @ParameterizedTest
        @CsvSource("MALE,MAN", "FEMALE,VROUW", "UNKNOWN,ANDERE")
        fun `should translate the sex of the user`(sex: Sex, expected: GeslachtEnum) {
            // given
            val user = user().sex(sex).build()

            // when
            val lid = user.toDto(emptyList())

            // then
            assertThat(lid.persoonsgegevens.geslacht).isEqualTo(expected)
        }

        @ParameterizedTest
        @CsvSource("FATHER,VADER", "MOTHER,MOEDER", "GUARDIAN,VOOGD", "RESPONSIBLE,OPVOEDINGSVERANTWOORDELIJKE")
        fun `should translate the contact role`(role: ContactRole, expected: RolEnum) {
            // given
            val user = user().build()
            val contact = contact().user(user).role(role).address(address().externalId("a-1").build()).build()

            // when
            val lid = user.toDto(listOf(contact))

            // then
            assertThat(lid.contacten.single().rol).isEqualTo(expected)
        }

        @Test
        fun `should map the user data to the external member`() {
            // given
            val address = address().externalId("addr-1").subPremise("b").description("home").postalAddress().build()
            val user = user().firstName("John").name("Doe").email("john@doe.com").mobile("0470").accountNo("BE00")
                .nis("nis").hasHandicap().hasReduction().addresses(address).build()

            // when
            val lid = user.toDto(emptyList())

            // then
            assertThat(lid.persoonsgegevens).extracting("gsm", "rekeningnummer", "rijksregisternummer")
                .containsExactly("0470", "BE00", "nis")
            assertThat(lid.vgagegevens).extracting("voornaam", "achternaam", "beperking", "verminderdlidgeld", "geboortedatum", "verhoogdekinderbijslag")
                .containsExactly("John", "Doe", true, true, user.birthdate, false)
            assertThat(lid.email).isEqualTo("john@doe.com")
            assertThat(lid.adressen.single()).extracting("id", "land", "postcode", "gemeente", "straat", "nummer", "bus", "postadres", "omschrijving")
                .containsExactly("addr-1", address.country, address.zipcode, address.town, address.street, address.number, "b", true, "home")
        }

        @Test
        fun `should mark the contact as dependent and link its address when the contact has a nis`() {
            // given
            val user = user().build()
            val contact = contact().user(user).externalId("c-1").nis("nis").mobile("0470").email("c@corp.com")
                .address(address().externalId("addr-1").build()).build()

            // when
            val lid = user.toDto(listOf(contact))

            // then
            assertThat(lid.contacten.single()).extracting("id", "voornaam", "achternaam", "gsm", "email", "rijksregisternummer", "lidtenlaste", "adres")
                .containsExactly("c-1", contact.firstName, contact.name, "0470", "c@corp.com", "nis", true, "addr-1")
        }

        @Test
        fun `should not mark the contact as dependent when the contact has no nis`() {
            // given
            val user = user().build()
            val contact = contact().user(user).address(address().externalId("a-1").build()).build()

            // when
            val lid = user.toDto(listOf(contact))

            // then
            assertThat(lid.contacten.single().lidtenlaste).isFalse()
        }

        @Test
        fun `should use the postal address of the user when the contact has no address`() {
            // given
            val postalAddress = address().externalId("postal").postalAddress().build()
            val user = user().addresses(address().externalId("other").build(), postalAddress).build()
            val contact = contact().user(user).build()

            // when
            val lid = user.toDto(listOf(contact))

            // then
            assertThat(lid.contacten.single().adres).isEqualTo(postalAddress.externalId)
        }

        @Test
        fun `should fail when neither the contact nor the user has an address known in Groepsadmin`() {
            // given
            val user = user().build()
            val contact = contact().user(user).build()

            // when / then
            assertThatThrownBy { user.toDto(listOf(contact)) }
                .isInstanceOf(LocalizedException::class.java)
                .hasMessage("user.contact.address.unknown")
        }

        @Test
        fun `should fall back to a known address when the contact's address is not in Groepsadmin yet`() {
            // given
            val knownAddress = address().externalId("known").build()
            val newAddress = address().postalAddress().build()
            val user = user().addresses(newAddress, knownAddress).build()
            val contact = contact().user(user).address(newAddress).build()

            // when
            val lid = user.toDto(listOf(contact))

            // then
            assertThat(lid.contacten.single().adres).isEqualTo(knownAddress.externalId)
        }
    }
}
