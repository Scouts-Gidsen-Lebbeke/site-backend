package be.sgl.backend.setting.domain

import be.sgl.backend.shared.persistence.Auditable
import jakarta.persistence.Entity
import jakarta.persistence.Id

@Entity
class Setting() : Auditable() {
    @Id
    lateinit var name: String
    lateinit var value: String

    constructor(id: String, value: Any) : this() {
        this.name = id
        this.value = value.toString()
    }
}