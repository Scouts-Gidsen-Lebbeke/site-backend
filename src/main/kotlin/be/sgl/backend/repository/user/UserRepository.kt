package be.sgl.backend.repository.user

import be.sgl.backend.entity.branch.Branch
import be.sgl.backend.entity.user.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : JpaRepository<User, Int> {
    fun findByUsername(username: String): User?
    fun getByUsername(username: String): User
    @Query("select distinct u from User u join fetch u.staffData join u.roles ur join ur.role r where r.staffBranch = :branch")
    fun getStaffForBranch(branch: Branch): List<User>
    fun deleteByUsername(username: String)
    fun findByNameAndFirstNameAndEmail(name: String, firstName: String, email: String): User?
}