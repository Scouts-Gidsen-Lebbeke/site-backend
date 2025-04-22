package be.sgl.backend.repository.activity

import be.sgl.backend.entity.branch.Branch
import be.sgl.backend.entity.registrable.activity.Activity
import be.sgl.backend.entity.registrable.activity.ActivityRegistration
import be.sgl.backend.entity.registrable.activity.ActivityRestriction
import be.sgl.backend.entity.user.User
import be.sgl.backend.repository.PaymentRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface ActivityRegistrationRepository : JpaRepository<ActivityRegistration, Int>, PaymentRepository<ActivityRegistration> {
    @Query("from ActivityRegistration where start between :begin and :end and paid order by start")
    fun getPaidRegistrationsBetween(begin: LocalDateTime, end: LocalDateTime): List<ActivityRegistration>
    @Query("from ActivityRegistration where user = :user and start between :begin and :end and paid order by start")
    fun getPaidRegistrationsForUserBetween(user: User, begin: LocalDateTime, end: LocalDateTime): List<ActivityRegistration>
    @Query("from ActivityRegistration where subscribable = :activity and paid")
    fun getPaidRegistrationsByActivity(activity: Activity): List<ActivityRegistration>
    fun existsBySubscribable(subscribable: Activity): Boolean
    fun getByUser(user: User): List<ActivityRegistration>
    fun existsBySubscribableAndUser(subscribable: Activity, user: User): Boolean
    fun countByRestriction(restriction: ActivityRestriction): Int
    @Query("select count(*) from ActivityRegistration where subscribable = :activity and restriction.branch = :branch")
    fun countByActivityAndBranch(activity: Activity, branch: Branch): Int
    fun getByUserAndSubscribable(user: User, subscribable: Activity): ActivityRegistration?
}