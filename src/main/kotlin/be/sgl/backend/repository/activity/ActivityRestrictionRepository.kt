package be.sgl.backend.repository.activity

import be.sgl.backend.entity.branch.Branch
import be.sgl.backend.entity.registrable.activity.ActivityRestriction
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ActivityRestrictionRepository : JpaRepository<ActivityRestriction, Int> {
    fun findAllByBranch(branch: Branch): List<ActivityRestriction>
}