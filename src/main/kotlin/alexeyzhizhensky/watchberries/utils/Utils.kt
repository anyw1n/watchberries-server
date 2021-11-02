package alexeyzhizhensky.watchberries.utils

import alexeyzhizhensky.watchberries.data.Price
import java.time.LocalDateTime

fun pricesOf(vararg elements: Pair<LocalDateTime, Int>) =
    elements.map { Price(it.first, it.second) }
