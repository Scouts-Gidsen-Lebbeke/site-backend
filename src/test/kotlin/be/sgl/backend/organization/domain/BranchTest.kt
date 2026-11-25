package be.sgl.backend.organization.domain

import be.sgl.backend.organization.domain.BranchTestMother.branch
import be.sgl.backend.shared.Sex
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BranchTest {

    private val minimumAge = 8
    private val maximumAge = 10

    @Test
    fun `should match when age equals the minimum age`() {
        // given
        val testee = branch().minimumAge(minimumAge).maximumAge(maximumAge).build()

        // when
        val matches = testee.matches(minimumAge, Sex.MALE)

        // then
        assertThat(matches).isTrue()
    }

    @Test
    fun `should match when age equals the maximum age`() {
        // given
        val testee = branch().minimumAge(minimumAge).maximumAge(maximumAge).build()

        // when
        val matches = testee.matches(maximumAge, Sex.MALE)

        // then
        assertThat(matches).isTrue()
    }

    @Test
    fun `should not match when younger than the minimum age`() {
        // given
        val testee = branch().minimumAge(minimumAge).maximumAge(maximumAge).build()

        // when
        val matches = testee.matches(minimumAge - 1, Sex.MALE)

        // then
        assertThat(matches).isFalse()
    }

    @Test
    fun `should not match when older than the maximum age`() {
        // given
        val testee = branch().minimumAge(minimumAge).maximumAge(maximumAge).build()

        // when
        val matches = testee.matches(maximumAge + 1, Sex.MALE)

        // then
        assertThat(matches).isFalse()
    }

    @Test
    fun `should match any age above the minimum when there is no maximum age`() {
        // given
        val testee = branch().minimumAge(minimumAge).build()

        // when
        val matches = testee.matches(99, Sex.MALE)

        // then
        assertThat(matches).isTrue()
    }

    @Test
    fun `should not match when the sex differs from the branch sex`() {
        // given
        val testee = branch().minimumAge(minimumAge).sex(Sex.MALE).build()

        // when
        val matches = testee.matches(minimumAge, Sex.FEMALE)

        // then
        assertThat(matches).isFalse()
    }

    @Test
    fun `should match any sex when the branch has no sex`() {
        // given
        val testee = branch().minimumAge(minimumAge).anySex().build()

        // when
        val matches = testee.matches(minimumAge, Sex.FEMALE)

        // then
        assertThat(matches).isTrue()
    }

    @Test
    fun `should use the name as string representation`() {
        // given
        val testee = branch().name("Kapoenen").build()

        // when
        val result = testee.toString()

        // then
        assertThat(result).isEqualTo("Kapoenen")
    }
}
