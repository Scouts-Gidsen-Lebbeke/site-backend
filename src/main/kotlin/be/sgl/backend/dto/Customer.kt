package be.sgl.backend.dto

import be.sgl.backend.entity.user.User

data class Customer(val name: String, val email: String, val id: String? = null) {
    constructor(user: User): this(user.getFullName(), user.email, user.customerId)
}
