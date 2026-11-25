package be.sgl.backend.setting.domain


import be.sgl.backend.setting.SettingId
object SettingTestMother {

    fun setting() = SettingBuilder()

    class SettingBuilder {
        private var name = SettingId.CALENDAR_NAME.name
        private var value: Any = "settingValue"

        fun name(name: String) = apply { this.name = name }

        fun id(id: SettingId) = apply { this.name = id.name }

        fun value(value: Any) = apply { this.value = value }

        fun build() = Setting(name, value)
    }
}
