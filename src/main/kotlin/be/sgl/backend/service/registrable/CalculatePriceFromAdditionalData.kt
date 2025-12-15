package be.sgl.backend.service.registrable

import be.sgl.backend.entity.registrable.Registrable
import com.dashjoin.jsonata.Jsonata.jsonata
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component

@Component
class CalculatePriceFromAdditionalData(
    private val objectMapper: ObjectMapper
) {

    fun execute(registrable: Registrable, additionalData: String?): Double {
        registrable.additionalFormRule ?: return 0.0
        additionalData ?: return 0.0
        val values = objectMapper
            .readerFor(Map::class.java)
            .readValue<Map<String, String>>(additionalData)
            .mapValues { it.value.toDoubleOrNull() ?: it.value }
        return jsonata(registrable.additionalFormRule)
            .evaluate(values)
            .toString().toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    }
}
