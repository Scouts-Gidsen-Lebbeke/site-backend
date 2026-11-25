package be.sgl.backend.shared.mail

interface DefaultSenderProvider {
    fun defaultSenderEmail(): String
}
