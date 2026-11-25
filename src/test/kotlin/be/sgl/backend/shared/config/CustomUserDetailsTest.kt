package be.sgl.backend.shared.config

import be.sgl.backend.shared.config.CustomUserDetailsTestMother.jwt
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.security.core.authority.SimpleGrantedAuthority

class CustomUserDetailsTest {

    @Test
    fun `should map all claims when the jwt contains every claim`() {
        // given
        val jwt = jwt().username("jdoe").firstName("John").lastName("Doe").email("john@doe.com").externalId("sub-1").buildJwt()

        // when
        val testee = CustomUserDetails(jwt)

        // then
        assertThat(testee).extracting("username", "firstName", "lastName", "email", "externalId", "password")
            .containsExactly("jdoe", "John", "Doe", "john@doe.com", "sub-1", null)
    }

    @Test
    fun `should map roles to granted authorities when the jwt contains roles`() {
        // given
        val jwt = jwt().roles("admin", "user").buildJwt()

        // when
        val testee = CustomUserDetails(jwt)

        // then
        assertThat(testee.authorities).containsExactly(SimpleGrantedAuthority("admin"), SimpleGrantedAuthority("user"))
    }

    @Test
    fun `should have no authorities when the jwt contains no roles claim`() {
        // given
        val jwt = jwt().buildJwt()

        // when
        val testee = CustomUserDetails(jwt)

        // then
        assertThat(testee.authorities).isEmpty()
    }

    @Test
    fun `should fail when the jwt has no preferred username`() {
        // given
        val jwt = jwt().username(null).buildJwt()

        // when / then
        assertThatThrownBy { CustomUserDetails(jwt) }.isInstanceOf(NullPointerException::class.java)
    }

    @Test
    fun `should fail when the jwt has no email`() {
        // given
        val jwt = jwt().email(null).buildJwt()

        // when / then
        assertThatThrownBy { CustomUserDetails(jwt) }.isInstanceOf(NullPointerException::class.java)
    }
}
