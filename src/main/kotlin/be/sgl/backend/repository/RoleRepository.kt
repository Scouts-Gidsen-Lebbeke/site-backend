package be.sgl.backend.repository

import be.sgl.backend.entity.branch.Branch
import be.sgl.backend.entity.user.Role
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface RoleRepository : JpaRepository<Role, Int> {
    fun getRoleByExternalIdEquals(id: String): Role?
    @Query("from Role where branch = :branch and staffBranch is null")
    fun getRoleToSyncByBranch(branch: Branch): Role?
}