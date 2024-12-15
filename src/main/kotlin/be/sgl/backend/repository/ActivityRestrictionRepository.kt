package be.sgl.backend.repository

import be.sgl.backend.entity.registrable.activity.ActivityRestriction
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ActivityRestrictionRepository : JpaRepository<ActivityRestriction, Int> {
}