package alexeyzhizhensky.watchberries.data

import java.time.LocalDateTime

data class Product(
    val sku: Int,
    val brand: String,
    val title: String,
    val prices: Map<LocalDateTime, Int>
)