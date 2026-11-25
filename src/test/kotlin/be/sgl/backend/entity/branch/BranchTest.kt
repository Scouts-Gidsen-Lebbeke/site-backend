package be.sgl.backend.entity.branch

import be.sgl.backend.entity.branch.BranchTestMother.branch
import be.sgl.backend.shared.Sex
import be.sgl.backend.entity.user.UserTestMother.user
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BranchTest {

    private val minimumAge = 8
    private val maximumAge = 10

    private fun userAged(age: Int, sex: Sex = Sex.MALE, ageDeviation: Int = 0) =
        user().birthdate(LocalDate.now().minusYears(age.toLong())).sex(sex).ageDeviation(ageDeviation).build()

    @Test
    fun `should match user when age equals the minimum age`() {
        // given
        val testee = branch().minimumAge(minimumAge).maximumAge(maximumAge).build()

        // when
        val matches = testee.matchesUser(userAged(minimumAge))

        // then
        assertThat(matches).isTrue()
    }

    @Test
    fun `should match user when age equals the maximum age`() {
        // given
        val testee = branch().minimumAge(minimumAge).maximumAge(maximumAge).build()

        // when
        val matches = testee.matchesUser(userAged(maximumAge))

        // then
        assertThat(matches).isTrue()
    }

    @Test
    fun `should not match user when younger than the minimum age`() {
        // given
        val testee = branch().minimumAge(minimumAge).maximumAge(maximumAge).build()

        // when
        val matches = testee.matchesUser(userAged(minimumAge - 1))

        // then
        assertThat(matches).isFalse()
    }

    @Test
    fun `should not match user when older than the maximum age`() {
        // given
        val testee = branch().minimumAge(minimumAge).maximumAge(maximumAge).build()

        // when
        val matches = testee.matchesUser(userAged(maximumAge + 1))

        // then
        assertThat(matches).isFalse()
    }

    @Test
    fun `should match user of any age above the minimum when there is no maximum age`() {
        // given
        val testee = branch().minimumAge(minimumAge).build()

        // when
        val matches = testee.matchesUser(userAged(99))

        // then
        assertThat(matches).isTrue()
    }

    @Test
    fun `should add the age deviation to the age of the user`() {
        // given
        val testee = branch().minimumAge(minimumAge).maximumAge(maximumAge).build()

        // when
        val matches = testee.matchesUser(userAged(minimumAge - 1, ageDeviation = 1))

        // then
        assertThat(matches).isTrue()
    }

    @Test
    fun `should not match user when the sex differs from the branch sex`() {
        // given
        val testee = branch().minimumAge(minimumAge).sex(Sex.MALE).build()

        // when
        val matches = testee.matchesUser(userAged(minimumAge, Sex.FEMALE))

        // then
        assertThat(matches).isFalse()
    }

    @Test
    fun `should match user of any sex when the branch has no sex`() {
        // given
        val testee = branch().minimumAge(minimumAge).anySex().build()

        // when
        val matches = testee.matchesUser(userAged(minimumAge, Sex.FEMALE))

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
