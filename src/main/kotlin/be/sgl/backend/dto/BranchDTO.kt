package be.sgl.backend.dto

import java.io.Serializable

open class BranchBaseDTO(
    val id: Int?,
    val name: String,
    val image: String
) : Serializable

class BranchDTO(
    id: Int?,
    name: String,
    image: String,
    val email: String,
    val minimumAge: Int,
    val maximumAge: Int?,
    val description: String?,
    val law: String?,
    val staffTitle: String?,
    val staff: List<StaffDTO>
) : BranchBaseDTO(id, name, image)