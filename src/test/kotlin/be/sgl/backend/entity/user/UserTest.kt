package be.sgl.backend.entity.user

import be.sgl.backend.shared.security.RoleLevel
import be.sgl.backend.entity.AddressTestMother.address
import be.sgl.backend.entity.branch.BranchTestMother.branch
import be.sgl.backend.entity.role.RoleTestMother.adminRole
import be.sgl.backend.entity.role.RoleTestMother.memberRole
import be.sgl.backend.entity.role.RoleTestMother.staffRole
import be.sgl.backend.entity.role.UserRoleTestMother.userRole
import be.sgl.backend.entity.user.UserTestMother.user
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
    inner class Level {

        @Test
        fun `should be guest when the user has no roles`() {
            // given
            val testee = user().build()

            // when
            val level = testee.level

            // then
            assertThat(level).isEqualTo(RoleLevel.GUEST)
        }

        @Test
        fun `should be the highest level of all roles when the user has multiple roles`() {
            // given
            val testee = user().build()
            userRole().user(testee).role(memberRole(branch().build()).id(1).build()).build()
            userRole().user(testee).role(adminRole().id(2).build()).build()
            userRole().user(testee).role(staffRole(branch().build()).id(3).build()).build()

            // when
            val level = testee.level

            // then
            assertThat(level).isEqualTo(RoleLevel.ADMIN)
        }

        @Test
        fun `should be staff when the only role is a staff role`() {
            // given
            val testee = user().build()
            userRole().user(testee).role(staffRole(branch().build()).build()).build()

            // when
            val level = testee.level

            // then
            assertThat(level).isEqualTo(RoleLevel.STAFF)
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
