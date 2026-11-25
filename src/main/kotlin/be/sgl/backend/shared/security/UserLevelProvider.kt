package be.sgl.backend.shared.security

interface UserLevelProvider {
    fun levelOf(username: String): RoleLevel?
}
