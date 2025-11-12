package be.sgl.backend.service.user

import be.sgl.backend.dto.UserRegistrationDTO
import be.sgl.backend.entity.user.MedicalRecord
import be.sgl.backend.entity.user.Role
import be.sgl.backend.entity.user.User
import be.sgl.backend.entity.user.UserRole
import be.sgl.backend.repository.user.MedicalRecordRepository
import be.sgl.backend.repository.user.UserRepository
import be.sgl.backend.repository.user.UserRoleRepository
import be.sgl.backend.mapper.AddressMapper
import be.sgl.backend.service.exception.UserNotFoundException
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired

/**
 * Gateway for all user data (contact info, medical data and roles).
 * Split up between an external and an internal way of working.
 */
abstract class UserDataProvider {

    private val logger = KotlinLogging.logger {}

    @Autowired
    protected lateinit var userRepository: UserRepository
    @Autowired
    private lateinit var addressMapper: AddressMapper
    @Autowired
    protected lateinit var userRoleRepository: UserRoleRepository
    @Autowired
    private lateinit var medicalRecordRepository: MedicalRecordRepository

    /**
     * Creates a user for the given [UserRegistrationDTO] and returns it.
     * This user has no username, denoting its registration isn't completed yet.
     * Its sole purpose is to link the payment and membership to it.
     */
    fun registerUser(registrationDTO: UserRegistrationDTO): User {
        val user = User()
        user.name = registrationDTO.name!!
        user.firstName = registrationDTO.firstName!!
        user.email = registrationDTO.email!!
        user.birthdate = registrationDTO.birthdate!!
        user.mobile = registrationDTO.mobile
        user.sex = registrationDTO.sex!!
        user.hasReduction = registrationDTO.hasReduction
        user.hasHandicap = registrationDTO.hasHandicap
        user.addresses.add(addressMapper.toEntity(registrationDTO.address!!))
        return userRepository.save(user)
    }

    /**
     * Mark the given user as completely registered.
     * Should be called when the payment after the initial registration is received.
     */
    abstract fun acceptRegistration(user: User)

    /**
     * Remove this user reference. Only valid if the user has no username yet.
     * Should be called when the payment after the initial registration didn't go through.
     */
    fun denyRegistration(user: User) {
        check(user.username == null) { "Registration denial should not be performed on already known users!" }
        userRepository.delete(user)
    }

    fun userExists(username: String?): Boolean {
        logger.debug { "Checking if user with username $username exists..." }
        return userRepository.existsByUsername(username ?: return false)
    }

    open fun findUser(username: String): User? {
        logger.debug { "Fetching user data for $username..." }
        return userRepository.findByUsername(username)
    }

    fun getUser(username: String): User {
        return findUser(username) ?: throw UserNotFoundException(username)
    }

    open fun findByNameAndEmail(name: String, firstName: String, email: String): User? {
        logger.debug { "Trying to find user with name $firstName $name and email $email..." }
        return userRepository.findByNameAndFirstNameAndEmail(name, firstName, email)
    }

    fun findByQuery(query: String): List<User> {
        logger.debug { "Trying to find users by query $query..." }
        return userRepository.findByQuery(query)
    }

    open fun updateUser(user: User): User {
        logger.debug { "Updating user data for ${user.username}..." }
        return userRepository.save(user)
    }

    open fun deleteUser(username: String) {
        logger.debug { "Deleting all user data for ${username}..." }
        userRepository.deleteByUsername(username)
    }

    open fun startRole(user: User, role: Role): UserRole? {
        logger.debug { "Starting role ${role.name} for ${user.username}..." }
        if (user.roles.any { it.role == role }) {
            logger.warn { "${user.username} already has the role ${role.name}! Starting aborted." }
            return null
        }
        val newRole = userRoleRepository.save(UserRole(user, role))
        user.roles.add(newRole)
        return newRole
    }

    open fun endRole(userRole: UserRole) {
        logger.debug { "Ending role ${userRole.role.name} for ${userRole.user.username}..." }
        userRoleRepository.delete(userRole)
    }

    fun getMedicalRecord(user: User): MedicalRecord? {
        logger.debug { "Fetching medical record for ${user.username}..." }
        return medicalRecordRepository.getMedicalRecordByUser(user)
    }

    fun updateMedicalRecord(medicalRecord: MedicalRecord) {
        logger.debug { "Updating medical record for ${medicalRecord.user.username}..." }
        medicalRecordRepository.save(medicalRecord)
    }
}