package be.sgl.backend.entity.registrable.activity

import be.sgl.backend.entity.registrable.Registrable
import be.sgl.backend.entity.user.User
import jakarta.persistence.Entity
import jakarta.persistence.OneToMany

@Entity
class Activity : Registrable() {
    var siblingReduction: Double = 0.0
    @OneToMany
    var restrictions = mutableListOf<ActivityRestriction>()
}