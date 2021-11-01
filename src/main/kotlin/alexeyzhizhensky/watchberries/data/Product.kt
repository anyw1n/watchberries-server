package alexeyzhizhensky.watchberries.data

data class Product(
    val sku: Int,
    val brand: String,
    val title: String,
    val prices: List<Price>
)
