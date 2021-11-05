package alexeyzhizhensky.watchberries.data

import java.util.UUID

data class User(
    val id: Int,
    val token: String,
    val key: UUID
)
