package be.sgl.backend.service

import be.sgl.backend.entity.setting.Setting
import be.sgl.backend.repository.SettingRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
class SettingService(
    private val settingRepository: SettingRepository
) {

    fun get(id: String): String? {
        return settingRepository.findByIdOrNull(id)?.value
    }

    fun getOrDefault(id: String, default: String): String {
        return get(id) ?: default
    }

    fun getOrDefault(id: String, default: Double): Double {
        return get(id)?.toDouble() ?: default
    }

    fun getOrDefault(id: String, default: Boolean): Boolean {
        return get(id)?.toBooleanStrictOrNull() ?: default
    }

    fun update(id: String, value: Any?) {
        value ?: return settingRepository.deleteById(id)
        val setting = settingRepository.findByIdOrNull(id) ?: Setting(id, value)
        setting.value = value.toString()
        settingRepository.save(setting)
    }
}