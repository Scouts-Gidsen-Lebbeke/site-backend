package be.sgl.backend.service.user

import be.sgl.backend.config.CustomUserDetails
import be.sgl.backend.dto.BranchDTO
import be.sgl.backend.dto.MedicalRecordDTO
import be.sgl.backend.dto.UserDTO
import be.sgl.backend.service.ImageService
import be.sgl.backend.mapper.UserMapper
import be.sgl.backend.repository.user.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import be.sgl.backend.service.ImageService.ImageDirectory.PROFILE_PICTURE
import be.sgl.backend.service.exception.UserNotFoundException
import java.nio.file.Path

@Service
class UserService {

    @Autowired
    private lateinit var mapper: UserMapper
    @Autowired
    private lateinit var userDataProvider: UserDataProvider
    @Autowired
    private lateinit var userRepository: UserRepository
    @Autowired
    private lateinit var imageService: ImageService

    fun getProfile(username: String): UserDTO {
        return mapper.toDto(userDataProvider.getUser(username))
    }

    /**
     * When a user creates an account externally (but linked to the correct organization),
     * we didn't receive the username thus cannot find the user with the normal flow.
     * As a workaround, we can try to use the other user details to fetch the correct user.
     * When we find a user in this way, we should update it too before returning the profile,
     * since there is simply no other option for the organization to know the correct link
     * (except for manual lookup and linking on database).
     */
    fun getUserWithDetails(userDetails: CustomUserDetails): UserDTO {
        val user = userDataProvider.findUser(userDetails.username)
            ?: userDataProvider.findByNameAndEmail(userDetails.lastName, userDetails.firstName, userDetails.email)
            ?: throw UserNotFoundException(userDetails.username)
        if (user.username == null) {
            userRepository.updateUsername(user.id!!, userDetails.username)
        }
        return mapper.toDto(user)
    }

    fun uploadProfilePicture(username: String, image: MultipartFile): Path {
        val user = userDataProvider.getUser(username)
        val path = imageService.replace(PROFILE_PICTURE, user.image, image)
        user.image = path.fileName.toString()
        userDataProvider.updateUser(user)
        return path
    }

    fun getByQuery(query: String): List<UserDTO> {
        return userDataProvider.findByQuery(query).map(mapper::toDto)
    }

    fun getStaffBranch(username: String): BranchDTO? {
        return userDataProvider.getUser(username).getStaffBranch()?.run(mapper::toDto)
    }

    fun getMedicalRecord(username: String): MedicalRecordDTO? {
        val user = userDataProvider.getUser(username)
        return userDataProvider.getMedicalRecord(user)?.run(mapper::toDto)
    }
}