package be.sgl.backend.repository

import be.sgl.backend.entity.branch.Branch
import be.sgl.backend.entity.branch.BranchStatus
import be.sgl.backend.entity.user.Sex
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface BranchRepository : JpaRepository<Branch, Int> {
    fun getByStatusIn(statuses: Collection<BranchStatus>): List<Branch>
    @Query("from Branch where :age between minimumAge and maximumAge and (sex is null or sex = :sex) and status != 'HIDDEN' order by status")
    fun getPossibleBranchesForSexAndAge(sex: Sex, age: Int): List<Branch>
}