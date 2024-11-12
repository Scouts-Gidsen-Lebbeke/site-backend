package be.sgl.backend.dto

import be.sgl.backend.entity.User
import be.sgl.backend.util.UserData
import java.io.Serializable

class UserDTO(user: User, userData: UserData?) : UserData, Serializable {
    val username = user.username
    val name = user.name
    val firstName = user.firstName
    val image = user.image
    override val birthdate = userData?.birthdate
    override val emailAddress = userData?.emailAddress
    override val mobile = userData?.mobile
    override val hasReduction = userData?.hasReduction ?: false
    override val branch = userData?.branch
}