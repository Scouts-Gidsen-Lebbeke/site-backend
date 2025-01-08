package be.sgl.backend.entity.user

enum class RoleLevel {
    ADMIN,
    STAFF,
    @Deprecated("Use the presence of an active membership")
    MEMBER,
    @Deprecated("Use the login validity")
    SCOUT,
    GUEST
}