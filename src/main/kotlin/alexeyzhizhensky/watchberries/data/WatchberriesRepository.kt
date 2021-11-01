package alexeyzhizhensky.watchberries.data

import alexeyzhizhensky.watchberries.data.requests.TokenRequest
import alexeyzhizhensky.watchberries.data.requests.UserRequest
import alexeyzhizhensky.watchberries.utils.OLD_PERIOD_MONTHS
import alexeyzhizhensky.watchberries.utils.pricesOf
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

object WatchberriesRepository {

    private val log = LoggerFactory.getLogger(WatchberriesRepository::class.java)

    private fun addProduct(sku: Int): Product? {
        val wbPage = parseWbPage(sku)

        if (wbPage == null) {
            log.error("Product $sku not added.")
            return null
        }

        val dateTime = LocalDateTime.now()

        val product = Product(
            sku = sku,
            brand = wbPage.brand,
            title = wbPage.title,
            prices = pricesOf(
                dateTime.minusMonths(OLD_PERIOD_MONTHS) to 0,
                dateTime to wbPage.price
            )
        )

        WatchberriesDatabase.insertProduct(product)

        log.info("Product $sku added.")

        return product
    }

    fun getProduct(sku: Int) = WatchberriesDatabase.getProduct(sku) ?: addProduct(sku)

    fun getProducts(skus: List<Int>) = skus.map { getProduct(it) }

    fun getProductsForUser(userId: String) = getProducts(WatchberriesDatabase.getSkusForUser(userId))

    fun getAllProducts() = getProducts(WatchberriesDatabase.getAllSkus())

    fun createUser(userRequest: UserRequest) = WatchberriesDatabase.insertUser(userRequest).also {
        log.info("User ${it.id} created.")
    }

    fun updateUser(id: String, tokenRequest: TokenRequest) = WatchberriesDatabase.updateUser(id, tokenRequest).also {
        val message = if (it != null) "User ${it.id} updated." else "Error updating user $id."
        log.info(message)
    }

    fun getUser(id: String) = WatchberriesDatabase.getUser(id)

    fun addSkuToUser(sku: Int, userId: String) = WatchberriesDatabase.addSkuToUser(sku, userId).also {
        val message = if (it != null) "Sku $sku added to user ${it.id}." else "Error adding sku $sku to user $userId."
        // TODO: subscribe to topic
        log.info(message)
    }

    fun removeSkuFromUser(sku: Int, userId: String) = WatchberriesDatabase.deleteSkuFromUser(sku, userId).also {
        val message =
            if (it != null) "Sku $sku removed from user ${it.id}." else "Error removing sku $sku from user $userId."
        // TODO: unsubscribe to topic
        log.info(message)
    }

    fun updatePrices() = WatchberriesDatabase.getAllSkus().forEach(WatchberriesRepository::updatePrice)

    private fun updatePrice(sku: Int) {
        val lastPrice = getProduct(sku)?.prices?.lastOrNull()?.price

        val currentPrice = parseWbPage(sku)?.price

        if (currentPrice == null) {
            log.error("SKU: $sku - Error price updating.")
            return
        }

        if (lastPrice == currentPrice) {
            return
        }

        WatchberriesDatabase.addPriceToProduct(sku, Price(LocalDateTime.now(), currentPrice))
    }

    fun deleteOldPrices(lastDateTime: LocalDateTime) {
        WatchberriesDatabase.getAllSkus().forEach { sku ->
            val lastPrice = getProduct(sku)?.prices?.findLast { it.dateTime < lastDateTime }?.price ?: return@forEach

            WatchberriesDatabase.addPriceToProduct(sku, Price(lastDateTime, lastPrice))
        }

        WatchberriesDatabase.deleteOldPrices(lastDateTime)
    }

    fun removeOldUsers(lastDateTime: LocalDateTime) {
        WatchberriesDatabase.getOldUserIds(lastDateTime).forEach { userId ->
            WatchberriesDatabase.getSkusForUser(userId).forEach {
                removeSkuFromUser(it, userId)
            }

            log.info("User $userId will be deleted.")
        }

        WatchberriesDatabase.deleteOldUsers(lastDateTime)
    }

    private fun parseWbPage(sku: Int) = runCatching {
        val doc = Jsoup.connect("https://by.wildberries.ru/catalog/$sku/detail.aspx?targetUrl=WP").get()

        val brand = doc.select("span[data-link=text{:product^brandName}]").firstOrNull()?.text()
            ?: throw Exception("Brand not found.")
        val title = doc.select("span[data-link=text{:product^goodsName}]").firstOrNull()?.text()
            ?: throw Exception("Title not found.")
        val priceText = doc.select("span.price-block__final-price").firstOrNull()?.text()
        val price = priceText?.let { """\D+""".toRegex().replace(it, "").toIntOrNull() } ?: 0

        WbPage(brand, title, price)
    }.getOrElse {
        log.error("Error parsing WB! SKU: $sku. Error: ${it.localizedMessage}")

        null
    }
}
