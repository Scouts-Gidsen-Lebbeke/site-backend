package be.sgl.backend.service.user

import be.sgl.backend.dto.UserDTO
import be.sgl.backend.service.mapper.UserMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class UserService {

    @Autowired
    private lateinit var mapper: UserMapper
    @Autowired
    private lateinit var userDataProvider: UserDataProvider

    fun getProfile(username: String): UserDTO {
        return mapper.toDto(userDataProvider.getUser(username))
    }
}