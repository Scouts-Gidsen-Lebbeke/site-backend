package be.sgl.backend.service.organization

import be.sgl.backend.dto.organization.Representative
import be.sgl.backend.entity.setting.SettingId
import be.sgl.backend.repository.user.UserRepository
import be.sgl.backend.service.ImageService
import be.sgl.backend.service.ImageService.ImageDirectory.ORGANIZATION
import be.sgl.backend.service.SettingService
import be.sgl.backend.service.exception.IncompleteConfigurationException
import be.sgl.backend.util.Usecase
import org.springframework.cache.annotation.Cacheable

@Usecase
class GetRepresentativeForOwner(
    private val settingService: SettingService,
    private val userRepository: UserRepository,
    private val imageService: ImageService
) {

    @Cacheable("representative")
    fun execute(): Representative {
        val username = settingService.get(SettingId.REPRESENTATIVE_USERNAME.name)
            ?: throw IncompleteConfigurationException("No representative configured for organization!")
        val user = userRepository.getByUsername(username)
        val title = settingService.getOrDefault(SettingId.REPRESENTATIVE_TITLE.name, "Vertegenwoordiger")
        val signatureFile = settingService.get(SettingId.REPRESENTATIVE_SIGNATURE.name)
            ?: throw IncompleteConfigurationException("No signature configured for organization!")
        val signature = imageService.get(signatureFile, ORGANIZATION)
            ?: throw IncompleteConfigurationException("No valid signature configured for organization!")
        return Representative(user, title, signature)
    }
}