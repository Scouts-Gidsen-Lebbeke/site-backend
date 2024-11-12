package be.sgl.backend.util

import be.sgl.backend.entity.Branch
import java.time.LocalDate

interface UserData {
    val birthdate: LocalDate?
    val emailAddress: String?
    val mobile: String?
    val hasReduction: Boolean
    val branch: Branch?
}