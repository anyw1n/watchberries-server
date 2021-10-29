package alexeyzhizhensky.watchberries.data

import alexeyzhizhensky.watchberries.data.tables.Prices
import alexeyzhizhensky.watchberries.data.tables.Products
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory
import java.net.URI
import java.time.LocalDateTime

object WatchberriesDatabase {

    private val log = LoggerFactory.getLogger(WatchberriesDatabase::class.java)

    fun connect() {
        val dbUri = URI(System.getenv(DATABASE_URL_KEY))

        val dataSource = HikariDataSource().apply {
            jdbcUrl = JDBC_URL_START + dbUri.host + dbUri.path
            driverClassName = DRIVER_CLASS_NAME
            username = dbUri.userInfo.split(":")[0]
            password = dbUri.userInfo.split(":")[1]
        }

        val flyway = Flyway.configure().dataSource(dataSource).load()
        flyway.migrate()

        Database.connect(dataSource)
    }

    private fun parseWbPage(sku: Int) = runCatching {
        val doc = Jsoup.connect("https://by.wildberries.ru/catalog/$sku/detail.aspx?targetUrl=WP").get()

        val brand = doc.select("span[data-link=text{:product^brandName}]").firstOrNull()?.text()
            ?: throw Exception("Brand not found.")
        val title = doc.select("span[data-link=text{:product^goodsName}]").firstOrNull()?.text()
            ?: throw Exception("Title not found.")
        val priceText = doc.select("span.price-block__final-price").firstOrNull()?.text() ?: "0"
        val price = """\D+""".toRegex().replace(priceText, "").toIntOrNull() ?: 0

        WbPage(brand, title, price)
    }.getOrElse {
        log.error("Error parsing WB! SKU: $sku. Error: ${it.localizedMessage}")

        null
    }

    fun updatePriceForProduct(sku: Int) {
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

    private fun addProduct(sku: Int) {
        log.info("Adding a $sku product...")

        val wbPage = parseWbPage(sku)

        if (wbPage == null) {
            log.error("Product $sku not added.")
            return
        }

        transaction {
            Products.insert {
                it[this.sku] = sku
                it[brand] = wbPage.brand
                it[title] = wbPage.title
            }

            Prices.insert {
                it[this.sku] = sku
                it[timestamp] = LocalDateTime.now()
                it[price] = wbPage.price
            }
        }

        log.info("Product $sku added.")
    }

    private fun getProduct(sku: Int) = transaction {
        Products.select { Products.sku eq sku }.firstOrNull()?.let { resultRow ->
            val prices = Prices
                .select { Prices.sku eq resultRow[Products.sku] }
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

    fun getProducts(skus: List<Int>) = skus.map { sku ->
        getProduct(sku) ?: addProduct(sku).run { getProduct(sku) }
    }

    fun getAllProducts() = transaction {
        Products.selectAll().map { resultRow ->
            val sku = resultRow[Products.sku]
            val prices = Prices
                .select { Prices.sku eq sku }
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

    private const val DATABASE_URL_KEY = "DATABASE_URL"
    private const val JDBC_URL_START = "jdbc:postgresql://"
    private const val DRIVER_CLASS_NAME = "org.postgresql.Driver"
}