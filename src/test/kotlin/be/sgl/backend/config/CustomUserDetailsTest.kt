package be.sgl.backend.config

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt

class CustomUserDetailsTest {

    @Test
    fun `CustomUserDetails should extract username from JWT`() {
        val jwt = mock(Jwt::class.java)
        `when`(jwt.getClaim<String>("preferred_username")).thenReturn("john.doe")
        `when`(jwt.getClaim<String>("given_name")).thenReturn("John")
        `when`(jwt.getClaim<String>("family_name")).thenReturn("Doe")
        `when`(jwt.getClaim<String>("email")).thenReturn("john.doe@example.com")
        `when`(jwt.getClaim<String>("sub")).thenReturn("ext-12345")
        `when`(jwt.getClaimAsStringList("roles")).thenReturn(null)

        val userDetails = CustomUserDetails(jwt)

        assertEquals("john.doe", userDetails.username)
    }

    @Test
    fun `CustomUserDetails should extract firstName from JWT`() {
        val jwt = mock(Jwt::class.java)
        `when`(jwt.getClaim<String>("preferred_username")).thenReturn("john.doe")
        `when`(jwt.getClaim<String>("given_name")).thenReturn("John")
        `when`(jwt.getClaim<String>("family_name")).thenReturn("Doe")
        `when`(jwt.getClaim<String>("email")).thenReturn("john.doe@example.com")
        `when`(jwt.getClaim<String>("sub")).thenReturn("ext-12345")
        `when`(jwt.getClaimAsStringList("roles")).thenReturn(null)

        val userDetails = CustomUserDetails(jwt)

        assertEquals("John", userDetails.firstName)
    }

    @Test
    fun `CustomUserDetails should extract lastName from JWT`() {
        val jwt = mock(Jwt::class.java)
        `when`(jwt.getClaim<String>("preferred_username")).thenReturn("john.doe")
        `when`(jwt.getClaim<String>("given_name")).thenReturn("John")
        `when`(jwt.getClaim<String>("family_name")).thenReturn("Doe")
        `when`(jwt.getClaim<String>("email")).thenReturn("john.doe@example.com")
        `when`(jwt.getClaim<String>("sub")).thenReturn("ext-12345")
        `when`(jwt.getClaimAsStringList("roles")).thenReturn(null)

        val userDetails = CustomUserDetails(jwt)

        assertEquals("Doe", userDetails.lastName)
    }

    @Test
    fun `CustomUserDetails should extract email from JWT`() {
        val jwt = mock(Jwt::class.java)
        `when`(jwt.getClaim<String>("preferred_username")).thenReturn("john.doe")
        `when`(jwt.getClaim<String>("given_name")).thenReturn("John")
        `when`(jwt.getClaim<String>("family_name")).thenReturn("Doe")
        `when`(jwt.getClaim<String>("email")).thenReturn("john.doe@example.com")
        `when`(jwt.getClaim<String>("sub")).thenReturn("ext-12345")
        `when`(jwt.getClaimAsStringList("roles")).thenReturn(null)

        val userDetails = CustomUserDetails(jwt)

        assertEquals("john.doe@example.com", userDetails.email)
    }

    @Test
    fun `CustomUserDetails should extract externalId from JWT`() {
        val jwt = mock(Jwt::class.java)
        `when`(jwt.getClaim<String>("preferred_username")).thenReturn("john.doe")
        `when`(jwt.getClaim<String>("given_name")).thenReturn("John")
        `when`(jwt.getClaim<String>("family_name")).thenReturn("Doe")
        `when`(jwt.getClaim<String>("email")).thenReturn("john.doe@example.com")
        `when`(jwt.getClaim<String>("sub")).thenReturn("ext-12345")
        `when`(jwt.getClaimAsStringList("roles")).thenReturn(null)

        val userDetails = CustomUserDetails(jwt)

        assertEquals("ext-12345", userDetails.externalId)
    }

    @Test
    fun `CustomUserDetails should extract authorities from JWT roles`() {
        val jwt = mock(Jwt::class.java)
        `when`(jwt.getClaim<String>("preferred_username")).thenReturn("john.doe")
        `when`(jwt.getClaim<String>("given_name")).thenReturn("John")
        `when`(jwt.getClaim<String>("family_name")).thenReturn("Doe")
        `when`(jwt.getClaim<String>("email")).thenReturn("john.doe@example.com")
        `when`(jwt.getClaim<String>("sub")).thenReturn("ext-12345")
        `when`(jwt.getClaimAsStringList("roles")).thenReturn(listOf("ROLE_USER", "ROLE_ADMIN"))

        val userDetails = CustomUserDetails(jwt)

        val authorities = userDetails.authorities
        assertEquals(2, authorities.size)
        assertTrue(authorities.contains(SimpleGrantedAuthority("ROLE_USER")))
        assertTrue(authorities.contains(SimpleGrantedAuthority("ROLE_ADMIN")))
    }

    @Test
    fun `CustomUserDetails should have empty authorities when roles claim is null`() {
        val jwt = mock(Jwt::class.java)
        `when`(jwt.getClaim<String>("preferred_username")).thenReturn("john.doe")
        `when`(jwt.getClaim<String>("given_name")).thenReturn("John")
        `when`(jwt.getClaim<String>("family_name")).thenReturn("Doe")
        `when`(jwt.getClaim<String>("email")).thenReturn("john.doe@example.com")
        `when`(jwt.getClaim<String>("sub")).thenReturn("ext-12345")
        `when`(jwt.getClaimAsStringList("roles")).thenReturn(null)

        val userDetails = CustomUserDetails(jwt)

        val authorities = userDetails.authorities
        assertTrue(authorities.isEmpty())
    }

    @Test
    fun `CustomUserDetails should have empty authorities when roles claim is empty list`() {
        val jwt = mock(Jwt::class.java)
        `when`(jwt.getClaim<String>("preferred_username")).thenReturn("john.doe")
        `when`(jwt.getClaim<String>("given_name")).thenReturn("John")
        `when`(jwt.getClaim<String>("family_name")).thenReturn("Doe")
        `when`(jwt.getClaim<String>("email")).thenReturn("john.doe@example.com")
        `when`(jwt.getClaim<String>("sub")).thenReturn("ext-12345")
        `when`(jwt.getClaimAsStringList("roles")).thenReturn(emptyList())

        val userDetails = CustomUserDetails(jwt)

        val authorities = userDetails.authorities
        assertTrue(authorities.isEmpty())
    }

    @Test
    fun `CustomUserDetails getPassword should return null`() {
        val jwt = mock(Jwt::class.java)
        `when`(jwt.getClaim<String>("preferred_username")).thenReturn("john.doe")
        `when`(jwt.getClaim<String>("given_name")).thenReturn("John")
        `when`(jwt.getClaim<String>("family_name")).thenReturn("Doe")
        `when`(jwt.getClaim<String>("email")).thenReturn("john.doe@example.com")
        `when`(jwt.getClaim<String>("sub")).thenReturn("ext-12345")
        `when`(jwt.getClaimAsStringList("roles")).thenReturn(null)

        val userDetails = CustomUserDetails(jwt)

        assertNull(userDetails.password)
    }

    @Test
    fun `CustomUserDetails should implement UserDetails interface`() {
        val jwt = mock(Jwt::class.java)
        `when`(jwt.getClaim<String>("preferred_username")).thenReturn("john.doe")
        `when`(jwt.getClaim<String>("given_name")).thenReturn("John")
        `when`(jwt.getClaim<String>("family_name")).thenReturn("Doe")
        `when`(jwt.getClaim<String>("email")).thenReturn("john.doe@example.com")
        `when`(jwt.getClaim<String>("sub")).thenReturn("ext-12345")
        `when`(jwt.getClaimAsStringList("roles")).thenReturn(null)

        val userDetails = CustomUserDetails(jwt)

        assertTrue(userDetails is org.springframework.security.core.userdetails.UserDetails)
    }
}
