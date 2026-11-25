package be.sgl.backend.controller.branch

import be.sgl.backend.access.domain.UserRoleRepository
import be.sgl.backend.access.domain.UserRoleTestMother.userRole
import be.sgl.backend.people.domain.StaffDataTestMother.staffData
import be.sgl.backend.people.domain.UserTestMother.user
import be.sgl.backend.organization.domain.Branch
import be.sgl.backend.organization.domain.BranchRepository
import be.sgl.backend.organization.domain.BranchTestMother.branch
import be.sgl.backend.organization.domain.RoleRepository
import be.sgl.backend.organization.domain.RoleTestMother.staffRole
import be.sgl.backend.people.domain.UserRepository
import be.sgl.backend.setting.SettingId
import be.sgl.backend.setting.domain.SettingRepository
import be.sgl.backend.setting.domain.SettingTestMother.setting
import be.sgl.backend.shared.util.IntegrationTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@IntegrationTest
class BranchStaffControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc
    @Autowired
    private lateinit var branchRepository: BranchRepository
    @Autowired
    private lateinit var roleRepository: RoleRepository
    @Autowired
    private lateinit var userRepository: UserRepository
    @Autowired
    private lateinit var settingRepository: SettingRepository
    @Autowired
    private lateinit var userRoleRepository: UserRoleRepository

    @AfterEach
    fun cleanUp() {
        userRoleRepository.deleteAll()
        userRepository.deleteAll()
        roleRepository.deleteAll()
        branchRepository.deleteAll()
        settingRepository.deleteAll()
    }

    private fun persistedBranchWithStaff(nickname: String): Branch {
        val branch = branchRepository.save(branch().id(null).name("Welpen").build())
        val role = roleRepository.save(staffRole(branch).id(null).build())
        val staff = userRepository.save(user().id(null).username("staff").firstName("Jan").staffData(staffData().totem("Bever").nickname(branch.id!!, nickname).build()).build())
        userRoleRepository.save(userRole().userId(staff.id!!).roleId(role.id!!).build())
        return branch
    }

    @Test
    fun `should embed staff when staff is visible`() {
        // given
        val nickname = "Akela"
        val branch = persistedBranchWithStaff(nickname)

        // when
        val result = mockMvc.perform(get("/branches/${branch.id}"))

        // then
        result.andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value(branch.name))
            .andExpect(jsonPath("$.staff.length()").value(1))
            .andExpect(jsonPath("$.staff[0].firstName").value("Jan"))
            .andExpect(jsonPath("$.staff[0].nickname").value(nickname))
            .andExpect(jsonPath("$.staff[0].totem").value("Bever"))
    }

    @Test
    fun `should omit staff when STAFF_VISIBLE is false`() {
        // given
        val branch = persistedBranchWithStaff("Akela")
        settingRepository.save(setting().id(SettingId.STAFF_VISIBLE).value(false).build())

        // when
        val result = mockMvc.perform(get("/branches/${branch.id}"))

        // then
        result.andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value(branch.name))
            .andExpect(jsonPath("$.staff.length()").value(0))
    }

    @Test
    fun `should return 404 when branch id is unknown`() {
        // given
        val unknownId = Int.MAX_VALUE

        // when
        val result = mockMvc.perform(get("/branches/$unknownId"))

        // then
        result.andExpect(status().isNotFound)
    }
}
