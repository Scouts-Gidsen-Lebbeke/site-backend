package be.sgl.backend.people.application

import be.sgl.backend.shared.config.CustomUserDetails
import be.sgl.backend.people.domain.User

fun interface CheckForInitialRun {
    fun execute(userDetails: CustomUserDetails): User?
}