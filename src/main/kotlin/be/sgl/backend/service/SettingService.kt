package be.sgl.backend.service

import be.sgl.backend.entity.Setting
import be.sgl.backend.repository.SettingRepository
import be.sgl.backend.service.exception.IncompleteConfigurationException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
class SettingService {

    @Autowired
    private lateinit var settingRepository: SettingRepository

    fun getRateForFiscalYear(fiscalYear: Int): Double {
        return getOrCreateDefault("DISPATCH_RATE_${fiscalYear}", 14.4).toDouble()
    }

    fun getRepresentativeTitle(): String {
        return getOrCreateDefault("REPRESENTATIVE_TITLE", "Vertegenwoordiger")
    }

    fun getRepresentativeUsername(): String? {
        return settingRepository.findByIdOrNull("REPRESENTATIVE_USERNAME")?.value
    }

    fun getSignatureFile(): String? {
        return settingRepository.findByIdOrNull("REPRESENTATIVE_SIGNATURE")?.value
    }

    private fun getOrCreateDefault(id: String, default: Any): String {
        val setting = settingRepository.findByIdOrNull(id)
            ?: return settingRepository.save(Setting(id, default)).value
        return setting.value
    }
}