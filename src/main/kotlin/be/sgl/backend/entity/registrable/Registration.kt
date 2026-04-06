package be.sgl.backend.entity.registrable

import be.sgl.backend.entity.Payment
import jakarta.persistence.Lob
import jakarta.persistence.ManyToOne
import jakarta.persistence.MappedSuperclass
import tools.jackson.databind.json.JsonMapper

@MappedSuperclass
abstract class Registration<T : Registrable> : Payment() {
    @ManyToOne
    lateinit var subscribable: T
    var completed = false
    @Lob
    var additionalData: String? = null

    override fun getDescription(): String {
        return subscribable.name
    }

    fun getAdditionalDataMap() = additionalData?.let { JsonMapper().readValue(it, Map::class.java) }
}