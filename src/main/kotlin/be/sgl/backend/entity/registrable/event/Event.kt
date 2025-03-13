package be.sgl.backend.entity.registrable.event

import be.sgl.backend.entity.registrable.Registrable
import jakarta.persistence.Entity

@Entity
class Event : Registrable() {
    var needsMobile: Boolean = false
}