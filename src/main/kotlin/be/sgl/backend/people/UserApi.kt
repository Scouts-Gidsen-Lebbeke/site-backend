package be.sgl.backend.people

interface UserApi {
    fun findIdByUsername(username: String): Int?
    fun findStaff(userIds: Collection<Int>, branchId: Int): List<StaffMember>
}
