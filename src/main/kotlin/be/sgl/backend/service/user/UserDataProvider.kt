package be.sgl.backend.service.user

import be.sgl.backend.dto.UserRegistrationDTO
import be.sgl.backend.entity.user.MedicalRecord
import be.sgl.backend.entity.user.Role
import be.sgl.backend.entity.user.User
import be.sgl.backend.repository.user.MedicalRecordRepository
import be.sgl.backend.repository.RoleRepository
import be.sgl.backend.repository.user.UserRepository
import be.sgl.backend.repository.user.UserRoleRepository
import be.sgl.backend.mapper.AddressMapper
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
    protected lateinit var roleRepository: RoleRepository
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
        user.name = registrationDTO.name
        user.firstName = registrationDTO.firstName
        user.email = registrationDTO.email
        user.birthdate = registrationDTO.birthdate
        user.mobile = registrationDTO.mobile
        user.sex = registrationDTO.sex
        user.hasReduction = registrationDTO.hasReduction
        user.hasHandicap = registrationDTO.hasHandicap
        user.addresses.add(addressMapper.toEntity(registrationDTO.address))
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

    abstract fun findUser(username: String): User?

    abstract fun getUser(username: String): User

    abstract fun findByNameAndEmail(name: String, firstName: String, email: String) : User?

    abstract fun updateUser(user: User): User

    open fun deleteUser(username: String) {
        logger.debug { "Deleting all user data for ${username}..." }
        userRepository.deleteByUsername(username)
    }

    abstract fun startRole(user: User, role: Role)

    abstract fun endRole(user: User, role: Role)

    open fun getMedicalRecord(user: User): MedicalRecord? {
        logger.debug { "Fetching medical record for ${user.username}..." }
        return medicalRecordRepository.getMedicalRecordByUser(user)
    }

    open fun updateMedicalRecord(medicalRecord: MedicalRecord) {
        logger.debug { "Updating medical record for ${medicalRecord.user.username}..." }
        medicalRecordRepository.save(medicalRecord)
    }
}