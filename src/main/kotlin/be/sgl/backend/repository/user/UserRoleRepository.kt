package be.sgl.backend.repository.user

import be.sgl.backend.entity.user.RoleLevel
import be.sgl.backend.entity.user.UserRole
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserRoleRepository : JpaRepository<UserRole, Long> {
    fun findByRole_Level(roleLevel: RoleLevel): List<UserRole>
}