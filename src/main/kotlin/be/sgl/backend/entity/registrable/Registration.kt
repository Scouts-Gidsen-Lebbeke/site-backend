package be.sgl.backend.entity.registrable

import be.sgl.backend.entity.Payment
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.Lob
import jakarta.persistence.ManyToOne
import jakarta.persistence.MappedSuperclass

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

    fun getAdditionalDataMap() = additionalData?.let { ObjectMapper().readValue(it, Map::class.java) }
}