package be.sgl.backend.dto.organization

import be.sgl.backend.entity.user.User
import java.io.File

// read-only, internal
data class Representative(
    val user: User,
    val title: String,
    val signature: File
)