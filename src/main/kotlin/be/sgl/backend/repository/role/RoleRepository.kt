package be.sgl.backend.repository.role

import be.sgl.backend.entity.branch.Branch
import be.sgl.backend.entity.role.Role
import be.sgl.backend.entity.user.RoleLevel
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface RoleRepository : JpaRepository<Role, Int> {
    fun getRoleByExternalIdEquals(id: String): Role?
    @Query("from Role where branch = :branch")
    fun getRoleToSyncByBranch(branch: Branch): Role?
    @Query("from Role where staffBranch = :branch")
    fun getStaffRoleToSyncByBranch(branch: Branch): Role?
    fun findByLevel(level: RoleLevel): MutableList<Role>
}