package be.sgl.backend.service.branch

import be.sgl.backend.access.AccessApi
import be.sgl.backend.entity.user.StaffDataTestMother.staffData
import be.sgl.backend.entity.user.UserTestMother.user
import be.sgl.backend.organization.application.BranchService
import be.sgl.backend.organization.domain.BranchTestMother.branch
import be.sgl.backend.organization.web.BranchDTO
import be.sgl.backend.repository.user.UserRepository
import be.sgl.backend.setting.SettingApi
import be.sgl.backend.setting.SettingId
import be.sgl.backend.shared.exception.BranchNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Assertions.tuple
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class GetBranchWithStaffTest {

    @Mock
    lateinit var branchService: BranchService
    @Mock
    lateinit var userRepository: UserRepository
    @Mock
    lateinit var settingApi: SettingApi
    @Mock
    lateinit var accessApi: AccessApi
    @InjectMocks
    lateinit var testee: GetBranchWithStaff

    private val id = 1
    private val branch = branch().id(id).build()

    @Test
    fun `should embed staff with their nickname for the branch when staff is visible`() {
        // given
        val nickname = "Akela"
        val staff = user().name("staffName").staffData(staffData().totem("Bever").nickname(branch().id(id).build(), nickname).build()).build()
        given(branchService.getBranch(id)).willReturn(BranchDTO.of(branch))
        given(settingApi.getOrDefault(SettingId.STAFF_VISIBLE.name, true)).willReturn(true)
        given(accessApi.staffUserIdsOf(id)).willReturn(listOf(staff.id!!))
        given(userRepository.findAllById(listOf(staff.id!!))).willReturn(listOf(staff))

        // when
        val result = testee.execute(id)

        // then
        assertThat(result.staff).extracting("name", "nickname", "totem").containsExactly(
            tuple(staff.name, nickname, staff.staffData.totem)
        )
    }

    @Test
    fun `should not set a nickname when the staff has none for the branch`() {
        // given
        val staff = user().staffData(staffData().nickname(branch().id(id + 1).build(), "Other").build()).build()
        given(branchService.getBranch(id)).willReturn(BranchDTO.of(branch))
        given(settingApi.getOrDefault(SettingId.STAFF_VISIBLE.name, true)).willReturn(true)
        given(accessApi.staffUserIdsOf(id)).willReturn(listOf(staff.id!!))
        given(userRepository.findAllById(listOf(staff.id!!))).willReturn(listOf(staff))

        // when
        val result = testee.execute(id)

        // then
        assertThat(result.staff.single().nickname).isNull()
    }

    @Test
    fun `should omit staff without loading it when staff is hidden`() {
        // given
        given(branchService.getBranch(id)).willReturn(BranchDTO.of(branch))
        given(settingApi.getOrDefault(SettingId.STAFF_VISIBLE.name, true)).willReturn(false)

        // when
        val result = testee.execute(id)

        // then
        assertThat(result.staff).isEmpty()
        then(userRepository).shouldHaveNoInteractions()
        then(accessApi).shouldHaveNoInteractions()
    }

    @Test
    fun `should copy the branch fields when building the branch with staff`() {
        // given
        given(branchService.getBranch(id)).willReturn(BranchDTO.of(branch))
        given(settingApi.getOrDefault(SettingId.STAFF_VISIBLE.name, true)).willReturn(false)

        // when
        val result = testee.execute(id)

        // then
        assertThat(result).usingRecursiveComparison().ignoringFields("staff").isEqualTo(BranchDTO.of(branch))
    }

    @Test
    fun `should throw when the branch does not exist`() {
        // given
        given(branchService.getBranch(id)).willAnswer { throw BranchNotFoundException() }

        // when / then
        assertThatThrownBy { testee.execute(id) }
            .isInstanceOf(BranchNotFoundException::class.java)
        then(userRepository).shouldHaveNoInteractions()
    }
}
