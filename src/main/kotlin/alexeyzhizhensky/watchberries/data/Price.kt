package alexeyzhizhensky.watchberries.data

import java.time.LocalDateTime

data class Price(
    val dateTime: LocalDateTime,
    val price: Int
)
