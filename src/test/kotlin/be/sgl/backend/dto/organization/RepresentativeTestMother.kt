package be.sgl.backend.dto.organization

import be.sgl.backend.entity.user.User
import be.sgl.backend.entity.user.UserTestMother
import java.io.File

object RepresentativeTestMother {

    fun representative() = RepresentativeBuilder()

    class RepresentativeBuilder {
        private var user = UserTestMother.user().name("Peeters").firstName("Jan").build()
        private var title = "Vertegenwoordiger"
        private var signature = File("signature.png")

        fun user(user: User) = apply { this.user = user }

        fun title(title: String) = apply { this.title = title }

        fun signature(signature: File) = apply { this.signature = signature }

        fun build() = Representative(user, title, signature)
    }
}
