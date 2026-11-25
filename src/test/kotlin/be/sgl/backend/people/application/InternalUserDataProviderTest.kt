package be.sgl.backend.people.application

import be.sgl.backend.people.domain.UserTestMother.user
import be.sgl.backend.people.domain.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class InternalUserDataProviderTest {

    @Mock
    lateinit var userRepository: UserRepository
    @InjectMocks
    lateinit var testee: InternalUserDataProvider

    @Nested
    inner class AcceptRegistration {

        @Test
        fun `should generate a username and save the user when the user has no username yet`() {
            // given
            val user = user().name("Doe").firstName("John").build()

            // when
            testee.acceptRegistration(user)

            // then
            assertThat(user.username).isEqualTo("Doe.John")
            then(userRepository).should().save(user)
        }

        @Test
        fun `should fail when the user already has a username`() {
            // given
            val user = user().username("jdoe").build()

            // when / then
            assertThatThrownBy { testee.acceptRegistration(user) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("Registration acceptance should not be performed on already known users!")
            then(userRepository).shouldHaveNoInteractions()
        }
    }

    @Test
    fun `should save the user when updating a user`() {
        // given
        val user = user().build()
        given(userRepository.save(user)).willReturn(user)

        // when
        val result = testee.updateUser(user)

        // then
        assertThat(result).isSameAs(user)
    }
}
