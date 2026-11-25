package be.sgl.backend.access.domain

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserRoleRepository : JpaRepository<UserRole, UserRole.Key> {
    fun findByUserId(userId: Int): List<UserRole>
    fun findByRoleId(roleId: Int): List<UserRole>
    fun findByRoleIdIn(roleIds: Collection<Int>): List<UserRole>
    fun deleteByRoleId(roleId: Int)
}
