package be.sgl.backend.service.user.sync

import be.sgl.backend.shared.address.AddressTestMother.address
import be.sgl.backend.people.domain.Contact
import be.sgl.backend.people.domain.ContactRole
import be.sgl.backend.people.domain.ContactTestMother
import be.sgl.backend.people.domain.UserTestMother.user
import be.sgl.backend.openapi.api.LedenApi
import be.sgl.backend.openapi.model.Contact.RolEnum
import be.sgl.backend.openapi.model.GroepsadminTestMother.adres
import be.sgl.backend.openapi.model.GroepsadminTestMother.contact
import be.sgl.backend.openapi.model.GroepsadminTestMother.lid
import be.sgl.backend.people.domain.ContactRepository
import be.sgl.backend.people.domain.UserRepository
import be.sgl.backend.shared.util.argumentCaptor
import be.sgl.backend.shared.util.captureValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.lenient
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class FetchExternalDataTest {

    @Mock
    lateinit var userRepository: UserRepository
    @Mock
    lateinit var ledenApi: LedenApi
    @Mock
    lateinit var contactRepository: ContactRepository
    @InjectMocks
    lateinit var testee: FetchExternalData

    @Test
    fun `should do nothing when the user has no external id`() {
        // given
        val user = user().build()

        // when
        testee.execute(user)

        // then
        then(ledenApi).shouldHaveNoInteractions()
        then(userRepository).shouldHaveNoInteractions()
    }

    @Test
    fun `should update the contact data and member id when the user has an account`() {
        // given
        val user = user().externalId("ext-1").username("jdoe").memberId("old").build()
        given(ledenApi.getLid("ext-1")).willReturn(lid().gsm("0470").rekeningnummer("BE00").lidnummer("new").build())

        // when
        testee.execute(user)

        // then
        assertThat(user).extracting("mobile", "accountNo", "memberId").containsExactly("0470", "BE00", "new")
        then(userRepository).should().save(user)
    }

    @Test
    fun `should not update the member id when the user has no account yet`() {
        // given
        val user = user().externalId("ext-1").build()
        given(ledenApi.getLid("ext-1")).willReturn(lid().lidnummer("new").build())

        // when
        testee.execute(user)

        // then
        assertThat(user.memberId).isNull()
    }

    @Test
    fun `should add new external addresses with a blank bus as no sub premise`() {
        // given
        val user = user().externalId("ext-1").build()
        given(ledenApi.getLid("ext-1")).willReturn(lid().adressen(adres().id("a-1").straat("Nieuwstraat").bus(" ").postadres().build()).build())

        // when
        testee.execute(user)

        // then
        assertThat(user.addresses.single()).extracting("externalId", "street", "subPremise", "postalAdress")
            .containsExactly("a-1", "Nieuwstraat", null, true)
    }

    @Test
    fun `should update a known address in place`() {
        // given
        val known = address().externalId("a-1").street("Oud").build()
        val user = user().externalId("ext-1").addresses(known).build()
        given(ledenApi.getLid("ext-1")).willReturn(lid().adressen(adres().id("a-1").straat("Nieuw").bus("b").build()).build())

        // when
        testee.execute(user)

        // then
        assertThat(known.street).isEqualTo("Nieuw")
        assertThat(known.subPremise).isEqualTo("b")
    }

    @Test
    fun `should not duplicate a known address`() {
        // given
        val known = address().externalId("a-1").build()
        val user = user().externalId("ext-1").addresses(known).build()
        given(ledenApi.getLid("ext-1")).willReturn(lid().adressen(adres().id("a-1").build()).build())

        // when
        testee.execute(user)

        // then
        assertThat(user.addresses).containsExactly(known)
    }

    @Test
    fun `should save the external contacts linked to the user`() {
        // given
        val user = user().externalId("ext-1").build()
        val captor = argumentCaptor<Contact>()
        given(ledenApi.getLid("ext-1")).willReturn(lid().adressen(adres().id("a-1").build())
            .contacten(contact().voornaam("Papa").rol(RolEnum.VADER).adres("a-1").gsm("0470").email("p@doe.com").build()).build())

        // when
        testee.execute(user)

        // then
        then(contactRepository).should().save(captureValue(captor))
        assertThat(captor.value).extracting("firstName", "role", "mobile", "email", "user")
            .containsExactly("Papa", ContactRole.FATHER, "0470", "p@doe.com", user)
        assertThat(captor.value.address).isSameAs(user.addresses.single())
    }

    @Test
    fun `should not create a new contact when the contact is already known`() {
        // given
        val user = user().externalId("ext-1").build()
        val existing = ContactTestMother.contact().id(5).user(user).externalId("c-1").build()
        val captor = argumentCaptor<Contact>()
        lenient().`when`(contactRepository.findContactsByUser(user)).thenReturn(listOf(existing))
        given(ledenApi.getLid("ext-1")).willReturn(lid().contacten(contact().id("c-1").build()).build())

        // when
        testee.execute(user)

        // then
        then(contactRepository).should().save(captureValue(captor))
        assertThat(captor.value.id).isEqualTo(5)
    }

    @Test
    fun `should link a known contact without external id on its name instead of duplicating it`() {
        // given
        val user = user().externalId("ext-1").build()
        val unlinked = ContactTestMother.contact().id(5).user(user).firstName("Papa").name("Doe").build()
        val captor = argumentCaptor<Contact>()
        given(contactRepository.findContactsByUser(user)).willReturn(listOf(unlinked))
        given(ledenApi.getLid("ext-1")).willReturn(lid().contacten(contact().id("c-1").voornaam("Papa").achternaam("Doe").build()).build())

        // when
        testee.execute(user)

        // then
        then(contactRepository).should().save(captureValue(captor))
        assertThat(captor.value).isSameAs(unlinked)
        assertThat(unlinked.externalId).isEqualTo("c-1")
    }

    @Test
    fun `should delete a linked contact that was removed in Groepsadmin`() {
        // given
        val user = user().externalId("ext-1").build()
        val removed = ContactTestMother.contact().id(5).user(user).externalId("c-gone").build()
        given(contactRepository.findContactsByUser(user)).willReturn(listOf(removed))
        given(ledenApi.getLid("ext-1")).willReturn(lid().build())

        // when
        testee.execute(user)

        // then
        then(contactRepository).should().deleteAll(listOf(removed))
    }

    @Test
    fun `should keep a local contact that was not pushed to Groepsadmin yet`() {
        // given
        val user = user().externalId("ext-1").build()
        val local = ContactTestMother.contact().id(5).user(user).firstName("Oma").build()
        given(contactRepository.findContactsByUser(user)).willReturn(listOf(local))
        given(ledenApi.getLid("ext-1")).willReturn(lid().build())

        // when
        testee.execute(user)

        // then
        then(contactRepository).should().deleteAll(emptyList())
    }
}
