package be.sgl.backend.service.user.sync

import be.sgl.backend.people.domain.UserTestMother.user
import be.sgl.backend.openapi.api.LidaanvragenApi
import be.sgl.backend.openapi.model.GroepsadminTestMother.lidAanvraag
import be.sgl.backend.openapi.model.LidAanvragen
import be.sgl.backend.people.domain.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class FetchUsersWithExternalOpenRegistrationsTest {

    @Mock
    lateinit var userRepository: UserRepository
    @Mock
    lateinit var lidaanvragenApi: LidaanvragenApi
    @InjectMocks
    lateinit var testee: FetchUsersWithExternalOpenRegistrations

    @Test
    fun `should map known users to their open request and skip unknown requests`() {
        // given
        val known = user().build()
        val requests = LidAanvragen().apply {
            aanvragen = mutableListOf(
                lidAanvraag().id("r1").voornaam("John").achternaam("Doe").email("john@doe.com").build(),
                lidAanvraag().id("r2").voornaam("Jane").achternaam("Doe").email("jane@doe.com").build())
        }
        given(lidaanvragenApi.aanvragen).willReturn(requests)
        given(userRepository.findByNameAndFirstNameAndEmail("Doe", "John", "john@doe.com")).willReturn(known)
        given(userRepository.findByNameAndFirstNameAndEmail("Doe", "Jane", "jane@doe.com")).willReturn(null)

        // when
        val result = testee.execute()

        // then
        assertThat(result).containsExactlyEntriesOf(mapOf(known to "r1"))
    }

    @Test
    fun `should return an empty map when there are no open requests`() {
        // given
        given(lidaanvragenApi.aanvragen).willReturn(LidAanvragen())

        // when
        val result = testee.execute()

        // then
        assertThat(result).isEmpty()
    }
}
