package be.sgl.backend.repository.user

import be.sgl.backend.entity.user.Role
import be.sgl.backend.entity.user.UserRole
import jakarta.transaction.Transactional
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.stereotype.Repository

@Repository
interface UserRoleRepository : JpaRepository<UserRole, Int> {
    @Modifying
    @Transactional
    fun deleteUserRolesByRole(role: Role)
    fun findByRole(role: Role): MutableList<UserRole>
}