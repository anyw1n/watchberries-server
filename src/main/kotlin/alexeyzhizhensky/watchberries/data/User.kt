package alexeyzhizhensky.watchberries.data

import java.time.LocalDateTime
import java.util.UUID

data class User(
    val id: Int,
    val token: String,
    val key: UUID,
    val lastSync: LocalDateTime,
    val skus: List<Int>
)
