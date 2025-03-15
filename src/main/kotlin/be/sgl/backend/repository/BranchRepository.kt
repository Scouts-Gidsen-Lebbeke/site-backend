package be.sgl.backend.repository

import be.sgl.backend.entity.branch.Branch
import be.sgl.backend.entity.user.Sex
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface BranchRepository : JpaRepository<Branch, Int> {
    @Query("from Branch where status = 'ACTIVE'")
    fun getBranchesWithCalendar(): List<Branch>
    @Query("from Branch where status != 'HIDDEN'")
    fun getVisibleBranches(): List<Branch>
    @Query("from Branch where status = 'PASSIVE'")
    fun getPassiveBranches(): List<Branch>
    @Query("from Branch where :age between minimumAge and maximumAge and (sex is null or sex = :sex) and status in ('ACTIVE', 'MEMBER') order by status")
    fun getPossibleBranchesForSexAndAge(sex: Sex, age: Int): List<Branch>
}