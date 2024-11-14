package be.sgl.backend.dto

import java.io.Serializable

open class BranchBaseDTO(
    val name: String,
    val image: String
) : Serializable

class BranchDTO(
    val email: String,
    val minimumAge: Int,
    val maximumAge: Int?,
    val description: String,
    val law: String?,
    val staffTitle: String,
    val staff: List<StaffDTO>,
    name: String,
    image: String
) : BranchBaseDTO(name, image)