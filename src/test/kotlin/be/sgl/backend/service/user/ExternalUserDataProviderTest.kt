package be.sgl.backend.service.user

import be.sgl.backend.shared.address.AddressTestMother.address
import be.sgl.backend.people.domain.ContactRole
import be.sgl.backend.people.domain.ContactTestMother.contact
import be.sgl.backend.shared.Sex
import be.sgl.backend.people.domain.UserTestMother.user
import be.sgl.backend.shared.exception.LocalizedException
import be.sgl.backend.openapi.api.LedenApi
import be.sgl.backend.openapi.api.LidaanvragenApi
import be.sgl.backend.openapi.model.Adres
import be.sgl.backend.openapi.model.Contact.RolEnum
import be.sgl.backend.openapi.model.GroepsadminTestMother.EXTERNAL_ORGANIZATION_ID
import be.sgl.backend.openapi.model.Lid
import be.sgl.backend.openapi.model.LidAanvraag
import be.sgl.backend.openapi.model.PersoonsGegevens.GeslachtEnum
import be.sgl.backend.people.domain.ContactRepository
import be.sgl.backend.people.domain.UserRepository
import be.sgl.backend.service.user.ExternalUserDataProvider.Companion.toDto
import be.sgl.backend.shared.util.argumentCaptor
import be.sgl.backend.shared.util.captureValue
import be.sgl.backend.shared.util.eqValue
import org.assertj.core.api.Assertions.assertThat
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

@ExtendWith(MockitoExtension::class)
class ExternalUserDataProviderTest {

    @Mock
    lateinit var lidaanvragenApi: LidaanvragenApi
    @Mock
    lateinit var ledenApi: LedenApi
    @Mock
    lateinit var contactRepository: ContactRepository
    @Mock
    lateinit var userRepository: UserRepository

    lateinit var testee: ExternalUserDataProvider

    @BeforeEach
    fun setUp() {
        testee = ExternalUserDataProvider(EXTERNAL_ORGANIZATION_ID, lidaanvragenApi, ledenApi, contactRepository, userRepository)
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
