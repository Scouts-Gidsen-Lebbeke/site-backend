package be.sgl.backend.repository

import be.sgl.backend.entity.registrable.activity.Activity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ActivityRepository : JpaRepository<Activity, Int>