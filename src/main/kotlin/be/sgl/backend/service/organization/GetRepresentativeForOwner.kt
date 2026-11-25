package be.sgl.backend.service.organization

import be.sgl.backend.dto.organization.Representative
import be.sgl.backend.setting.SettingId
import be.sgl.backend.people.domain.UserRepository
import be.sgl.backend.shared.image.ImageService
import be.sgl.backend.shared.image.ImageService.ImageDirectory.ORGANIZATION
import be.sgl.backend.setting.SettingApi
import be.sgl.backend.shared.exception.IncompleteConfigurationException
import be.sgl.backend.shared.util.Usecase
import org.springframework.cache.annotation.Cacheable

@Usecase
class GetRepresentativeForOwner(
    private val settingApi: SettingApi,
    private val userRepository: UserRepository,
    private val imageService: ImageService
) {

    @Cacheable("representative")
    fun execute(): Representative {
        val username = settingApi.get(SettingId.REPRESENTATIVE_USERNAME.name)
            ?: throw IncompleteConfigurationException("No representative configured for organization!")
        val user = userRepository.getByUsername(username)
        val title = settingApi.getOrDefault(SettingId.REPRESENTATIVE_TITLE.name, "Vertegenwoordiger")
        val signatureFile = settingApi.get(SettingId.REPRESENTATIVE_SIGNATURE.name)
            ?: throw IncompleteConfigurationException("No signature configured for organization!")
        val signature = imageService.get(signatureFile, ORGANIZATION)
            ?: throw IncompleteConfigurationException("No valid signature configured for organization!")
        return Representative(user, title, signature)
    }
}