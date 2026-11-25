package be.sgl.backend.setting

interface SettingApi {
    fun get(id: String): String?
    fun getOrDefault(id: String, default: String): String
    fun getOrDefault(id: String, default: Double): Double
    fun getOrDefault(id: String, default: Boolean): Boolean
}
