package be.sgl.backend.people.application

import be.sgl.backend.shared.config.CustomUserDetailsTestMother.userDetails
import be.sgl.backend.people.domain.SiblingRelationTestMother.siblingRelation
import be.sgl.backend.people.domain.UserTestMother.user
import be.sgl.backend.shared.exception.UserNotFoundException
import be.sgl.backend.people.domain.SiblingRepository
import be.sgl.backend.people.domain.UserRepository
import be.sgl.backend.shared.image.ImageService
import be.sgl.backend.shared.image.ImageService.ImageDirectory.PROFILE_PICTURE
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.never
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Path

@ExtendWith(MockitoExtension::class)
class UserServiceTest {

    @Mock
    lateinit var checkForInitialRun: CheckForInitialRun
    @Mock
    lateinit var userRepository: UserRepository
    @Mock
    lateinit var imageService: ImageService
    @Mock
    lateinit var siblingRepository: SiblingRepository
    @Mock
    lateinit var image: MultipartFile
    @InjectMocks
    lateinit var testee: UserService

    @Nested
    inner class GetUserWithDetails {

        @Test
        fun `should return the user found by username without relinking`() {
            // given
            val details = userDetails().username("jdoe").build()
            val user = user().username("jdoe").build()
            given(userRepository.findByUsername("jdoe")).willReturn(user)

            // when
            val result = testee.getUserWithDetails(details)

            // then
            assertThat(result.username).isEqualTo("jdoe")
            then(userRepository).should().findByUsername("jdoe")
            then(userRepository).shouldHaveNoMoreInteractions()
            then(checkForInitialRun).shouldHaveNoInteractions()
        }

        @Test
        fun `should link the username when the user is found by name and email`() {
            // given
            val details = userDetails().username("jdoe").firstName("John").lastName("Doe").email("john@doe.com").build()
            val user = user().id(7).firstName("John").name("Doe").email("john@doe.com").build()
            given(userRepository.findByUsername("jdoe")).willReturn(null)
            given(userRepository.findByNameAndFirstNameAndEmail("Doe", "John", "john@doe.com")).willReturn(user)

            // when
            val result = testee.getUserWithDetails(details)

            // then
            then(userRepository).should().updateUsername(7, "jdoe")
            assertThat(result.firstName).isEqualTo("John")
            then(checkForInitialRun).shouldHaveNoInteractions()
        }

        @Test
        fun `should return the user created by the initial run when no user is found`() {
            // given
            val details = userDetails().username("jdoe").build()
            val initialUser = user().username("jdoe").build()
            given(userRepository.findByUsername("jdoe")).willReturn(null)
            given(userRepository.findByNameAndFirstNameAndEmail(details.lastName, details.firstName, details.email)).willReturn(null)
            given(checkForInitialRun.execute(details)).willReturn(initialUser)

            // when
            val result = testee.getUserWithDetails(details)

            // then
            assertThat(result.username).isEqualTo("jdoe")
            then(userRepository).should(never()).updateUsername(anyInt(), anyString())
        }

        @Test
        fun `should throw when no user is found and it is not the initial run`() {
            // given
            val details = userDetails().username("jdoe").build()
            given(userRepository.findByUsername("jdoe")).willReturn(null)
            given(userRepository.findByNameAndFirstNameAndEmail(details.lastName, details.firstName, details.email)).willReturn(null)
            given(checkForInitialRun.execute(details)).willReturn(null)

            // when / then
            assertThatThrownBy { testee.getUserWithDetails(details) }
                .isInstanceOf(UserNotFoundException::class.java)
                .hasMessage("User jdoe not found")
        }

        @Test
        fun `should not return a user linked to another username when matching on name and email`() {
            // given
            val details = userDetails().username("jdoe").build()
            val otherAccount = user().username("someoneElse").build()
            given(userRepository.findByUsername("jdoe")).willReturn(null)
            given(userRepository.findByNameAndFirstNameAndEmail(details.lastName, details.firstName, details.email)).willReturn(otherAccount)
            given(checkForInitialRun.execute(details)).willReturn(null)

            // when / then
            assertThatThrownBy { testee.getUserWithDetails(details) }.isInstanceOf(UserNotFoundException::class.java)
        }

        @Test
        fun `should return the newly linked username when the user is found by name and email`() {
            // given
            val details = userDetails().username("jdoe").build()
            val user = user().id(7).build()
            given(userRepository.findByUsername("jdoe")).willReturn(null)
            given(userRepository.findByNameAndFirstNameAndEmail(details.lastName, details.firstName, details.email)).willReturn(user)

            // when
            val result = testee.getUserWithDetails(details)

            // then
            assertThat(result.username).isEqualTo("jdoe")
        }
    }

    @Test
    fun `should store the new file name on the user when uploading a profile picture`() {
        // given
        val user = user().username("jdoe").image("old.png").build()
        val newPath = Path.of("images", "profile", "new.png")
        given(userRepository.getByUsername("jdoe")).willReturn(user)
        given(imageService.replace(PROFILE_PICTURE, "old.png", image)).willReturn(newPath)

        // when
        val result = testee.uploadProfilePicture("jdoe", image)

        // then
        assertThat(result).isEqualTo(newPath)
        assertThat(user.image).isEqualTo("new.png")
        then(userRepository).should().save(user)
    }

    @Test
    fun `should map the siblings of the user when getting siblings`() {
        // given
        val user = user().username("jdoe").build()
        val sibling = user().id(2).firstName("Sister").build()
        given(userRepository.getByUsername("jdoe")).willReturn(user)
        given(siblingRepository.getByUser(user)).willReturn(listOf(siblingRelation().user(user).sibling(sibling).build()))

        // when
        val result = testee.getSiblings("jdoe")

        // then
        assertThat(result).extracting("firstName").containsExactly("Sister")
    }
}
