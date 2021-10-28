package alexeyzhizhensky.watchberries.data

import alexeyzhizhensky.watchberries.data.tables.Prices
import alexeyzhizhensky.watchberries.data.tables.Products
import alexeyzhizhensky.watchberries.data.tables.Skus
import alexeyzhizhensky.watchberries.data.tables.Users
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory
import java.net.URI
import java.time.LocalDateTime
import java.util.*

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

        arrayListOf(
            24718720,
            24797691,
            12530538,
            8389067,
            26819460,
            5091950,
            16738270,
            25535704,
            17514615,
            6690908,
            25656533
        ).forEach(this::addProductIfNeeded)
    }

    fun updatePriceForProduct(sku: Int) {
        val currentPrice = parseWbPage(sku)?.price ?: return

        val lastPrice = transaction {
            Prices.select { Prices.sku eq sku }.orderBy(Prices.timestamp).last()[Prices.price]
        }

        if (currentPrice == lastPrice) {
            return
        }

        transaction {
            Prices.insert {
                it[Prices.sku] = sku
                it[timestamp] = LocalDateTime.now()
                it[price] = currentPrice
            }
        }

        log.info("SKU: $sku - Price updated.")
    }

    fun addUser(newUserId: UUID) = transaction {
        Users.insertAndGetId {
            it[id] = newUserId
        }.value
    }.also {
        log.info("User $it added to db.")
    }

    fun addSkuToUser(newUserSku: Int, userId: UUID) {
        transaction {
            Skus.insert {
                it[user] = userId
                it[sku] = newUserSku
            }
        }

        log.info("Sku $newUserSku added to user $userId.")

        addProductIfNeeded(newUserSku)
    }

    private fun addProductIfNeeded(newSku: Int) {
        val query = transaction { Products.select { Products.sku eq newSku }.toList() }

        if (query.isNotEmpty()) {
            return
        }

        log.info("Adding a $newSku product...")

        val wbPage = parseWbPage(newSku) ?: return

        transaction {
            Products.insert {
                it[sku] = newSku
                it[brand] = wbPage.brand
                it[title] = wbPage.title
            }

            Prices.insert {
                it[sku] = newSku
                it[timestamp] = LocalDateTime.now()
                it[price] = wbPage.price
            }
        }

        log.info("Product $newSku added.")
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

    private const val DATABASE_URL_KEY = "DATABASE_URL"
    private const val JDBC_URL_START = "jdbc:postgresql://"
    private const val DRIVER_CLASS_NAME = "org.postgresql.Driver"
}