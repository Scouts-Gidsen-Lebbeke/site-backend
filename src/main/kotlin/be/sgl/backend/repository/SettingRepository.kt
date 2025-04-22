package be.sgl.backend.repository

import be.sgl.backend.entity.setting.Setting
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SettingRepository : JpaRepository<Setting, String>