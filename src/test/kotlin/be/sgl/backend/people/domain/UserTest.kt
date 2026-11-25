package be.sgl.backend.people.domain

import be.sgl.backend.shared.address.AddressTestMother.address
import be.sgl.backend.people.domain.UserTestMother.user
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class UserTest {

    @Nested
    inner class GetAge {

        private val birthdate = LocalDate.of(2004, 1, 27)

        @Test
        fun `should not count the year yet when the reference date is the day before the birthday`() {
            // given
            val testee = user().birthdate(birthdate).build()

            // when
            val age = testee.getAge(birthdate.plusYears(10).minusDays(1))

            // then
            assertThat(age).isEqualTo(9)
        }

        @Test
        fun `should count the year when the reference date is the birthday`() {
            // given
            val testee = user().birthdate(birthdate).build()

            // when
            val age = testee.getAge(birthdate.plusYears(10))

            // then
            assertThat(age).isEqualTo(10)
        }

        @Test
        fun `should be zero when the reference date is the birthdate`() {
            // given
            val testee = user().birthdate(birthdate).build()

            // when
            val age = testee.getAge(birthdate)

            // then
            assertThat(age).isZero()
        }

        @Test
        fun `should handle a leap day birthday in a non leap year`() {
            // given
            val leapDay = LocalDate.of(2004, 2, 29)
            val testee = user().birthdate(leapDay).build()

            // when
            val ageOnFebruary28 = testee.getAge(LocalDate.of(2005, 2, 28))

            // then
            assertThat(ageOnFebruary28).isZero()
        }

        @Test
        fun `should count the year for a leap day birthday on the first of march in a non leap year`() {
            // given
            val leapDay = LocalDate.of(2004, 2, 29)
            val testee = user().birthdate(leapDay).build()

            // when
            val age = testee.getAge(LocalDate.of(2005, 3, 1))

            // then
            assertThat(age).isEqualTo(1)
        }

        @Test
        fun `should ignore the age deviation when calculating the age`() {
            // given
            val testee = user().birthdate(birthdate).ageDeviation(1).build()

            // when
            val age = testee.getAge(birthdate.plusYears(10))

            // then
            assertThat(age).isEqualTo(10)
        }

        @Test
        fun `should be negative when the reference date is before the birthdate`() {
            // given
            val testee = user().birthdate(birthdate).build()

            // when
            val age = testee.getAge(birthdate.minusYears(1))

            // then
            assertThat(age).isEqualTo(-1)
        }
    }

    @Nested
    inner class GetHomeAddress {

        @Test
        fun `should return the postal address when there is one`() {
            // given
            val postalAddress = address().street("Postal").postalAddress().build()
            val testee = user().addresses(address().build(), postalAddress).build()

            // when
            val homeAddress = testee.getHomeAddress()

            // then
            assertThat(homeAddress).isSameAs(postalAddress)
        }

        @Test
        fun `should return null when no address is a postal address`() {
            // given
            val testee = user().addresses(address().build()).build()

            // when
            val homeAddress = testee.getHomeAddress()

            // then
            assertThat(homeAddress).isNull()
        }
    }

    @Test
    fun `should combine first name and name when getting the full name`() {
        // given
        val testee = user().firstName("Jan").name("Janssens").build()

        // when
        val fullName = testee.getFullName()

        // then
        assertThat(fullName).isEqualTo("Jan Janssens")
    }
}
