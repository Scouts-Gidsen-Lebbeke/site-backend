package be.sgl.backend.service.user

import be.sgl.backend.dto.UserDTO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class UserService {

    @Autowired
    private lateinit var userDataProvider: UserDataProvider

    fun getProfile(username: String): UserDTO {
        return UserDTO(user, userDataProvider.getUserData(user))
    }
}