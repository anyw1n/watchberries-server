package alexeyzhizhensky.watchberries.data

import alexeyzhizhensky.watchberries.utils.Constants.OLD_PERIOD_MONTHS
import alexeyzhizhensky.watchberries.utils.pricesOf
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

object WbRepository {

    private val log = LoggerFactory.getLogger(WbRepository::class.java)

    private val db = WbDatabase.getInstance()

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

        db.insertProduct(product)

        log.info("Product $sku added.")

        return product
    }

    fun getProduct(sku: Int) = db.getProduct(sku) ?: addProduct(sku)

    fun getProducts(skus: List<Int>, page: Int?, limit: Int?) = if (page != null && limit != null) {
        val pages = skus.chunked(limit)
        val pageList = pages.getOrNull(page) ?: emptyList()

        pages.count() to pageList.mapNotNull { getProduct(it) }
    } else {
        1 to skus.mapNotNull { getProduct(it) }
    }

    fun getProductsForUser(userId: Int, page: Int?, limit: Int?) =
        getProducts(db.getSkusForUser(userId), page, limit).also { updateUser(userId) }

    fun getAllProducts(page: Int?, limit: Int?) = getProducts(db.getAllSkus(), page, limit)

    fun createUser(token: String) = db.insertUser(token).also {
        val message = if (it != null) "User ${it.id} created." else "Error creating user."
        log.info(message)
    }

    fun updateUser(id: Int, token: String) = db.updateUser(id, token).also {
        val message = if (it != null) "User ${it.id} updated." else "Error updating user $id."
        log.info(message)
    }

    private fun updateUser(id: Int) = db.updateUser(id)

    fun getUser(id: Int) = db.getUser(id).also { updateUser(id) }

    fun addSkuToUser(sku: Int, userId: Int) = db.addSkuToUser(sku, userId).also {
        updateUser(userId)

        getProduct(sku)

        val message = if (it != null) {
            "Sku $sku added to user ${it.id}."
        } else {
            "Error adding sku $sku to user $userId."
        }

        log.info(message)
    }

    fun removeSkuFromUser(sku: Int, userId: Int) = db.deleteSkuFromUser(sku, userId).also {
        updateUser(userId)

        val message = if (it != null) {
            "Sku $sku removed from user ${it.id}."
        } else {
            "Error removing sku $sku from user $userId."
        }
        log.info(message)
    }

    fun updateProducts() = db.getAllSkus().forEach(WbRepository::updateProduct)

    private fun updateProduct(sku: Int) {
        val product = getProduct(sku)

        if (product == null) {
            log.error("Can't get product with sku $sku.")
            return
        }

        val wbPage = parseWbPage(sku)

        if (wbPage == null) {
            log.error("Can't parse page for sku $sku.")
            return
        }

        if (product.prices.lastOrNull()?.price != wbPage.price) {
            db.addPriceToProduct(sku, Price(LocalDateTime.now(), wbPage.price))
            log.info("Product $sku price updated.")
        }

        if (product.brand != wbPage.brand || product.title != wbPage.title) {
            db.updateProduct(Product(sku, wbPage.brand, wbPage.title, emptyList()))
            log.info("Product $sku info updated.")
        }
    }

    fun deleteOldPrices(lastDateTime: LocalDateTime) {
        db.getAllSkus().forEach { sku ->
            val lastPrice = getProduct(sku)?.prices?.findLast { it.dateTime < lastDateTime }?.price
                ?: return@forEach

            db.addPriceToProduct(sku, Price(lastDateTime, lastPrice))
        }

        db.deleteOldPrices(lastDateTime)
    }

    fun removeOldUsers(lastDateTime: LocalDateTime) {
        db.getOldUserIds(lastDateTime).forEach {
            log.info("User $it will be deleted.")
        }

        db.deleteOldUsers(lastDateTime)
    }

    private fun parseWbPage(sku: Int) = runCatching {
        val doc =
            Jsoup.connect("https://by.wildberries.ru/catalog/$sku/detail.aspx?targetUrl=WP").get()

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
