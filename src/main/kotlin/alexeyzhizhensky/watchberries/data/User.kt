package alexeyzhizhensky.watchberries.data

import java.time.LocalDateTime
import java.util.UUID

data class User(
    val id: String,
    val token: String,
    val key: UUID = UUID.randomUUID(),
    val lastSync: LocalDateTime = LocalDateTime.now(),
    val skus: List<Int> = emptyList()
)
