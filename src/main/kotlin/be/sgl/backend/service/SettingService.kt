package be.sgl.backend.service

import be.sgl.backend.entity.setting.Setting
import be.sgl.backend.entity.setting.SettingId
import be.sgl.backend.repository.SettingRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
class SettingService {

    @Autowired
    private lateinit var settingRepository: SettingRepository

    fun get(id: SettingId): String? {
        return settingRepository.findByIdOrNull(id.name)?.value
    }

    fun getOrDefault(id: SettingId, default: String): String {
        return get(id) ?: default
    }

    fun getOrDefault(id: SettingId, default: Double): Double {
        return get(id)?.toDouble() ?: default
    }

    fun update(id: SettingId, value: Any?) {
        value ?: return settingRepository.deleteById(id.name)
        val setting = settingRepository.findByIdOrNull(id.name) ?: Setting(id, value)
        setting.value = value.toString()
        settingRepository.save(setting)
    }
}