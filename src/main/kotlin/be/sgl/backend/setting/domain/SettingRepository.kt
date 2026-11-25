package be.sgl.backend.setting.domain

import be.sgl.backend.setting.domain.Setting
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SettingRepository : JpaRepository<Setting, String>