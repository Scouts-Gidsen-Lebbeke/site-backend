package be.sgl.backend.repository.activity

import be.sgl.backend.entity.registrable.activity.Activity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface ActivityRepository : JpaRepository<Activity, Int> {
    @Query("from Activity order by start desc")
    fun findAllRecentFirst(): List<Activity>
    @Query("from Activity where now() < end and not cancelled order by start")
    fun findAllVisibleActivities(): List<Activity>
}