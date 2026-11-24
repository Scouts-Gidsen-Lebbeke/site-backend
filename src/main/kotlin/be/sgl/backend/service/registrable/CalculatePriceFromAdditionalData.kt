package be.sgl.backend.service.registrable

import be.sgl.backend.entity.registrable.Registrable
import com.dashjoin.jsonata.Jsonata.jsonata
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper

@Component
class CalculatePriceFromAdditionalData(
    private val jsonMapper: JsonMapper
) {

    fun execute(registrable: Registrable, additionalData: String?): Double {
        registrable.additionalFormRule ?: return 0.0
        additionalData ?: return 0.0
        val values = jsonMapper
            .readerFor(Map::class.java)
            .readValue<Map<String, Any?>>(additionalData)
            // form values can be json numbers or numeric strings
            .mapValues { (_, value) -> (value as? String)?.toDoubleOrNull() ?: value }
        // an undefined result (e.g. a missing field) evaluates to null
        val result: Any? = jsonata(registrable.additionalFormRule).evaluate(values)
        return result?.toString()?.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    }
}
