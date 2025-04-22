package be.sgl.backend.entity.setting

import be.sgl.backend.entity.Auditable
import jakarta.persistence.Entity
import jakarta.persistence.Id

@Entity
class Setting() : Auditable() {
    @Id
    lateinit var name: String
    lateinit var value: String

    constructor(id: SettingId, value: Any) : this() {
        this.name = id.name
        this.value = value.toString()
    }
}