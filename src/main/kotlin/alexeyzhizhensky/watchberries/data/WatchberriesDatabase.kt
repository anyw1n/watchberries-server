package alexeyzhizhensky.watchberries.data

import alexeyzhizhensky.watchberries.data.tables.Prices
import alexeyzhizhensky.watchberries.data.tables.Products
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.transactions.transaction
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory
import java.net.URI
import java.time.LocalDateTime

object WatchberriesDatabase {

    private val log = LoggerFactory.getLogger(WatchberriesDatabase::class.java)

    fun connect() {
        val dataSource = HikariDataSource().apply {
            val dbUri = URI(System.getenv(DATABASE_URL_KEY))
            val userInfo = dbUri.userInfo.split(":")

            jdbcUrl = JDBC_URL_START + dbUri.host + dbUri.path
            driverClassName = DRIVER_CLASS_NAME
            username = userInfo[0]
            password = userInfo[1]
        }

        Flyway.configure().dataSource(dataSource).load().apply {
            migrate()
        }

        Database.connect(dataSource)
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

    private fun addProduct(sku: Int) {
        val wbPage = parseWbPage(sku)

        if (wbPage == null) {
            log.error("Product $sku not added.")
            return
        }

        val dateTime = LocalDateTime.now()

        transaction {
            Products.insert {
                it[this.sku] = sku
                it[brand] = wbPage.brand
                it[title] = wbPage.title
            }

            Prices.insert {
                it[this.sku] = sku
                it[timestamp] = dateTime
                it[price] = wbPage.price
            }
        }

        addPricePoint(sku, dateTime.minusMonths(3))

        log.info("Product $sku added.")
    }

    private fun getAllSkus() = transaction { Products.selectAll().map { it[Products.sku] } }

    fun getAllProducts() = getProducts(getAllSkus())

    fun getProducts(skus: List<Int>) = skus.map { sku ->
        getProduct(sku) ?: addProduct(sku).run { getProduct(sku) }
    }

    private fun getProduct(sku: Int) = transaction {
        Products.select { Products.sku eq sku }.singleOrNull()?.let { resultRow ->
            val prices = Prices.select { Prices.sku eq sku }
                .orderBy(Prices.timestamp)
                .associate { it[Prices.timestamp] to it[Prices.price] }

            Product(
                sku = sku,
                brand = resultRow[Products.brand],
                title = resultRow[Products.title],
                prices = prices
            )
        }
    }

    fun updatePrices() = getAllSkus().forEach(WatchberriesDatabase::updatePrice)

    private fun updatePrice(sku: Int) {
        val lastPrice = transaction {
            Prices.select { Prices.sku eq sku }.orderBy(Prices.timestamp).last()[Prices.price]
        }

        val currentPrice = parseWbPage(sku)?.price

        if (currentPrice == null) {
            log.error("SKU: $sku - Error price updating.")
            return
        }

        if (lastPrice == currentPrice) {
            return
        }

        transaction {
            Prices.insert {
                it[this.sku] = sku
                it[timestamp] = LocalDateTime.now()
                it[price] = currentPrice
            }
        }

        log.info("SKU: $sku - Price updated.")
    }

    fun deleteOldPrices(lastDateTime: LocalDateTime) {
        getAllSkus().forEach { addPricePoint(it, lastDateTime) }

        transaction { Prices.deleteWhere { Prices.timestamp less lastDateTime } }
    }

    private fun addPricePoint(sku: Int, dateTime: LocalDateTime) = transaction {
        val whereCondition = (Prices.sku eq sku) and (Prices.timestamp less dateTime)
        val price = Prices.select(whereCondition).orderBy(Prices.timestamp).lastOrNull()?.get(Prices.price) ?: 0

        Prices.insert {
            it[this.sku] = sku
            it[timestamp] = dateTime
            it[Prices.price] = price
        }
    }

    private const val DATABASE_URL_KEY = "DATABASE_URL"
    private const val JDBC_URL_START = "jdbc:postgresql://"
    private const val DRIVER_CLASS_NAME = "org.postgresql.Driver"
}