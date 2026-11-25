package be.sgl.backend.organization.domain

import be.sgl.backend.organization.domain.OrganizationTestMother.organization
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OrganizationTest {

    @Test
    fun `should return the first email when there are several contact methods`() {
        // given
        val email = "info@group.com"
        val testee = organization().mobile("+32470000000").email(email).email("other@group.com").build()

        // when
        val result = testee.getEmail()

        // then
        assertThat(result).isEqualTo(email)
    }

    @Test
    fun `should return no email when there is no email contact method`() {
        // given
        val testee = organization().mobile("+32470000000").build()

        // when
        val result = testee.getEmail()

        // then
        assertThat(result).isNull()
    }

    @Test
    fun `should return the first mobile when there are several contact methods`() {
        // given
        val mobile = "+32470000000"
        val testee = organization().email("info@group.com").mobile(mobile).mobile("+32471111111").build()

        // when
        val result = testee.getMobile()

        // then
        assertThat(result).isEqualTo(mobile)
    }

    @Test
    fun `should return no mobile when there is no mobile contact method`() {
        // given
        val testee = organization().email("info@group.com").build()

        // when
        val result = testee.getMobile()

        // then
        assertThat(result).isNull()
    }
}
