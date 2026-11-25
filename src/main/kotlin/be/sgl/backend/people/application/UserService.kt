package be.sgl.backend.people.application

import be.sgl.backend.shared.config.CustomUserDetails
import be.sgl.backend.people.web.UserDTO
import be.sgl.backend.shared.image.ImageService
import be.sgl.backend.people.domain.SiblingRepository
import be.sgl.backend.people.domain.UserRepository
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import be.sgl.backend.shared.image.ImageService.ImageDirectory.PROFILE_PICTURE
import be.sgl.backend.shared.exception.UserNotFoundException
import be.sgl.backend.people.application.CheckForInitialRun
import java.nio.file.Path

@Service
class UserService(
    private val checkForInitialRun: CheckForInitialRun,
    private val userRepository: UserRepository,
    private val imageService: ImageService,
    private val siblingRepository: SiblingRepository
) {

    fun getProfile(username: String): UserDTO {
        return UserDTO.of(userRepository.getByUsername(username))
    }

    fun getUserWithDetails(userDetails: CustomUserDetails): UserDTO {
        val user = userRepository.findByUsername(userDetails.username)
            ?: userRepository.findByNameAndFirstNameAndEmail(userDetails.lastName, userDetails.firstName, userDetails.email)
                ?.takeIf { it.username == null }
            ?: checkForInitialRun.execute(userDetails)
            ?: throw UserNotFoundException(userDetails.username)
        if (user.username == null) {
            userRepository.updateUsername(user.id!!, userDetails.username)
            user.username = userDetails.username
        }
        return UserDTO.of(user)
    }

    fun uploadProfilePicture(username: String, image: MultipartFile): Path {
        val user = userRepository.getByUsername(username)
        val path = imageService.replace(PROFILE_PICTURE, user.image, image)
        user.image = path.fileName.toString()
        userRepository.save(user)
        return path
    }

    fun getByQuery(query: String): List<UserDTO> {
        return userRepository.findByQuery(query).map(UserDTO::of)
    }

    fun getSiblings(username: String): List<UserDTO> {
        val user = userRepository.getByUsername(username)
        return siblingRepository.getByUser(user).map { UserDTO.of(it.sibling) }
    }
}