package be.sgl.backend.service.belcotax

import be.sgl.backend.dto.DeclarationFormDTO
import be.sgl.backend.entity.Organization
import org.springframework.stereotype.Service

@Service
class FormService {

    fun createForm(owner: Organization, certifier: Organization, form: DeclarationFormDTO): ByteArray {
        TODO("")
    }
}