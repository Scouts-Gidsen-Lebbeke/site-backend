package be.sgl.backend.entity.user

enum class RoleLevel {
    GUEST,
    @Deprecated("Use the login validity")
    SCOUT,
    @Deprecated("Use the presence of an active membership")
    MEMBER,
    STAFF,
    ADMIN
}