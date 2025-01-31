package be.sgl.backend.service.user

import be.sgl.backend.dto.UserDTO
import be.sgl.backend.repository.UserRepository
import be.sgl.backend.service.ImageService
import be.sgl.backend.service.mapper.UserMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile

@Service
class UserService {

    @Autowired
    private lateinit var mapper: UserMapper
    @Autowired
    private lateinit var userDataProvider: UserDataProvider
    @Autowired
    private lateinit var imageService: ImageService

    fun getProfile(username: String): UserDTO {
        return mapper.toDto(userDataProvider.getUser(username))
    }

    fun uploadProfilePicture(username: String, image: MultipartFile) {
        val user = userDataProvider.getUser(username)
        user.image = imageService.replace("profile", user.image, image)
        // userDataProvider.updateUser(user)
    }
}