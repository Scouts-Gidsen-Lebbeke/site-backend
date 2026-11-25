package be.sgl.backend.people.application

import be.sgl.backend.people.domain.BloodGroup
import be.sgl.backend.people.domain.MedicalRecordTestMother.medicalRecord
import be.sgl.backend.people.domain.UserTestMother.user
import be.sgl.backend.shared.exception.UserNotFoundException
import be.sgl.backend.people.domain.MedicalRecordRepository
import be.sgl.backend.people.domain.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class MedicalRecordServiceTest {

    @Mock
    lateinit var userRepository: UserRepository
    @Mock
    lateinit var medicalRecordRepository: MedicalRecordRepository
    @InjectMocks
    lateinit var testee: MedicalRecordService

    @Test
    fun `should return the medical record when the user has one`() {
        // given
        val user = user().username("jdoe").build()
        val record = medicalRecord().id(4).user(user).allergies("nuts").bloodGroup(BloodGroup.ON).build()
        given(userRepository.findByUsername("jdoe")).willReturn(user)
        given(medicalRecordRepository.findMedicalRecordByUser(user)).willReturn(record)

        // when
        val result = testee.getMedicalRecord("jdoe")

        // then
        assertThat(result).extracting("id", "allergies", "bloodGroup").containsExactly(4, "nuts", BloodGroup.ON)
    }

    @Test
    fun `should return null when the user has no medical record`() {
        // given
        val user = user().username("jdoe").build()
        given(userRepository.findByUsername("jdoe")).willReturn(user)
        given(medicalRecordRepository.findMedicalRecordByUser(user)).willReturn(null)

        // when
        val result = testee.getMedicalRecord("jdoe")

        // then
        assertThat(result).isNull()
    }

    @Test
    fun `should throw when the user does not exist`() {
        // given
        given(userRepository.findByUsername("jdoe")).willReturn(null)

        // when / then
        assertThatThrownBy { testee.getMedicalRecord("jdoe") }
            .isInstanceOf(UserNotFoundException::class.java)
            .hasMessage("User jdoe not found")
        then(medicalRecordRepository).shouldHaveNoInteractions()
    }
}
