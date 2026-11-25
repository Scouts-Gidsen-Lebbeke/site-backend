package be.sgl.backend.people.application

import be.sgl.backend.people.StaffMember
import be.sgl.backend.people.UserApi
import be.sgl.backend.people.domain.StaffData
import be.sgl.backend.people.domain.User
import be.sgl.backend.people.domain.UserRepository
import org.springframework.stereotype.Component

@Component
class UserApiImpl(private val userRepository: UserRepository) : UserApi {

    override fun findIdByUsername(username: String): Int? {
        return userRepository.findByUsername(username)?.id
    }

    override fun findStaff(userIds: Collection<Int>, branchId: Int): List<StaffMember> {
        return userRepository.findAllById(userIds).mapNotNull { toStaffMember(it, branchId) }
    }

    private fun toStaffMember(user: User, branchId: Int): StaffMember? {
        val staffData: StaffData? = user.staffData
        if (staffData == null) return null
        return StaffMember(user.name, user.firstName, user.image, staffData.nicknames[branchId], staffData.totem)
    }
}
