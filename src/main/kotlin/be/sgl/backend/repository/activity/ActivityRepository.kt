package be.sgl.backend.repository.activity

import be.sgl.backend.entity.registrable.activity.Activity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface ActivityRepository : JpaRepository<Activity, Int> {
    fun findAllByEndAfterOrderByStart(end: LocalDateTime): List<Activity>
}