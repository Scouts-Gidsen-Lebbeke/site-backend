package be.sgl.backend.repository

import be.sgl.backend.entity.branch.Branch
import be.sgl.backend.entity.user.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : JpaRepository<User, Long> {
    fun getUserByUsernameEquals(username: String): User
    @Query("from User u join fetch u.userData where u.username = :username")
    fun getUserByUsernameEqualsAndUserData(username: String): User
    @Query("select distinct u from User u join fetch u.staffData join u.roles ur join ur.role r where r.staffBranch = :branch")
    fun getStaffForBranch(branch: Branch): List<User>
}