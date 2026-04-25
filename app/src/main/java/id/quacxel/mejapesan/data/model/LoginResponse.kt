package id.quacxel.mejapesan.data.model

data class LoginRequest(
    val idToken: String
)

data class LoginResponse(
    val success: Boolean,
    val token: String,
    val user: User
)

data class User(
    val id: Int,
    val name: String?,
    val email: String,
    val store: LoginStore?
)

data class LoginStore(
    val id: Int,
    val name: String,
    val logo: String?
)
