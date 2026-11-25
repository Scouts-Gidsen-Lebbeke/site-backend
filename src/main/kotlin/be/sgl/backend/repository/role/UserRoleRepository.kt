package be.sgl.backend.repository.role

import be.sgl.backend.organization.domain.Role
import be.sgl.backend.entity.role.UserRole
import jakarta.transaction.Transactional
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface UserRoleRepository : JpaRepository<UserRole, Int> {
    @Modifying
    @Transactional
    @Query("delete from UserRole where role.id = :roleId")
    fun deleteUserRolesByRoleId(roleId: Int)
    fun findByRole(role: Role): MutableList<UserRole>
}
