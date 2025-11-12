package be.sgl.backend.service.user.inital

import be.sgl.backend.config.CustomUserDetails
import be.sgl.backend.entity.user.User

fun interface CheckForInitialRun {
    fun execute(userDetails: CustomUserDetails): User?
}