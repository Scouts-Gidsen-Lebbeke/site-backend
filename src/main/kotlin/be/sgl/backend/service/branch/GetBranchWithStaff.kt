package be.sgl.backend.service.branch

import be.sgl.backend.access.AccessApi
import be.sgl.backend.dto.branch.BranchWithStaff
import be.sgl.backend.dto.user.StaffDTO
import be.sgl.backend.people.domain.StaffData
import be.sgl.backend.people.domain.User
import be.sgl.backend.organization.application.BranchService
import be.sgl.backend.people.domain.UserRepository
import be.sgl.backend.setting.SettingApi
import be.sgl.backend.setting.SettingId
import be.sgl.backend.shared.util.Usecase

@Usecase
class GetBranchWithStaff(
    private val branchService: BranchService,
    private val userRepository: UserRepository,
    private val settingApi: SettingApi,
    private val accessApi: AccessApi
) {

    fun execute(id: Int): BranchWithStaff {
        val branch = branchService.getBranch(id)
        val staff = if (settingApi.getOrDefault(SettingId.STAFF_VISIBLE.name, true)) {
            userRepository.findAllById(accessApi.staffUserIdsOf(id)).filter(::hasStaffData).map { StaffDTO.of(it, id) }
        } else {
            emptyList()
        }
        return BranchWithStaff.of(branch, staff)
    }

    private fun hasStaffData(user: User): Boolean {
        val staffData: StaffData? = user.staffData
        return staffData != null
    }
}
