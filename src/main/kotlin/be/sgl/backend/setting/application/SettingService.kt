package be.sgl.backend.setting.application

import be.sgl.backend.setting.domain.Setting
import be.sgl.backend.setting.domain.SettingRepository
import be.sgl.backend.setting.SettingApi
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
class SettingService(
    private val settingRepository: SettingRepository
) : SettingApi {

    override fun get(id: String): String? {
        return settingRepository.findByIdOrNull(id)?.value
    }

    override fun getOrDefault(id: String, default: String): String {
        return get(id) ?: default
    }

    override fun getOrDefault(id: String, default: Double): Double {
        return get(id)?.toDoubleOrNull() ?: default
    }

    override fun getOrDefault(id: String, default: Boolean): Boolean {
        return get(id)?.toBooleanStrictOrNull() ?: default
    }

    fun update(id: String, value: Any?) {
        value ?: return settingRepository.deleteById(id)
        val setting = settingRepository.findByIdOrNull(id) ?: Setting(id, value)
        setting.value = value.toString()
        settingRepository.save(setting)
    }
}