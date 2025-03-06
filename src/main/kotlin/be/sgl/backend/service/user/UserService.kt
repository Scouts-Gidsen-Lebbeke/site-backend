package be.sgl.backend.service.user

import be.sgl.backend.dto.UserDTO
import be.sgl.backend.service.ImageService
import be.sgl.backend.mapper.UserMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import be.sgl.backend.service.ImageService.ImageDirectory.PROFILE_PICTURE

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

    fun uploadProfilePicture(username: String, image: MultipartFile): String {
        val user = userDataProvider.getUser(username)
        val path = imageService.replace(PROFILE_PICTURE, user.image, image)
        user.image = path.fileName.toString()
        userDataProvider.updateUser(user)
        return path.toString()
    }
}