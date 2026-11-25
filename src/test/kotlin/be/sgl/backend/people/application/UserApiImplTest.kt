package be.sgl.backend.people.application

import be.sgl.backend.people.StaffMember
import be.sgl.backend.people.domain.StaffDataTestMother.staffData
import be.sgl.backend.people.domain.UserRepository
import be.sgl.backend.people.domain.UserTestMother.user
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class UserApiImplTest {

    @Mock
    lateinit var userRepository: UserRepository
    @InjectMocks
    lateinit var testee: UserApiImpl

    private val branchId = 4

    @Test
    fun `should return id when username exists`() {
        // given
        val user = user().id(7).username(USERNAME).build()
        given(userRepository.findByUsername(USERNAME)).willReturn(user)

        // when
        val result = testee.findIdByUsername(USERNAME)

        // then
        assertThat(result).isEqualTo(user.id)
    }

    @Test
    fun `should return null when username is unknown`() {
        // given
        given(userRepository.findByUsername(USERNAME)).willReturn(null)

        // when
        val result = testee.findIdByUsername(USERNAME)

        // then
        assertThat(result).isNull()
    }

    @Test
    fun `should map staff with branch nickname when finding staff`() {
        // given
        val nickname = "Akela"
        val staffData = staffData().totem("Bever").nickname(branchId, nickname).nickname(branchId + 1, "Other").build()
        val staff = user().id(1).name("Doe").firstName("John").image("john.png").staffData(staffData).build()
        given(userRepository.findAllById(listOf(1))).willReturn(listOf(staff))

        // when
        val result = testee.findStaff(listOf(1), branchId)

        // then
        assertThat(result).containsExactly(StaffMember(staff.name, staff.firstName, staff.image, nickname, staffData.totem))
        then(userRepository).should().findAllById(listOf(1))
        then(userRepository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun `should skip users without staff data when finding staff`() {
        // given
        val staff = user().id(1).build()
        val withoutStaffData = user().id(2).withoutStaffData().build()
        given(userRepository.findAllById(listOf(1, 2))).willReturn(listOf(staff, withoutStaffData))

        // when
        val result = testee.findStaff(listOf(1, 2), branchId)

        // then
        assertThat(result).extracting("firstName").containsExactly(staff.firstName)
    }

    companion object {
        private const val USERNAME = "jdoe"
    }
}
