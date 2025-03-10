package be.sgl.backend.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id

@Entity
class Setting() : Auditable() {
    @Id
    lateinit var name: String
    lateinit var value: String

    constructor(name: String, value: Any) : this() {
        this.name = name
        this.value = value.toString()
    }
}