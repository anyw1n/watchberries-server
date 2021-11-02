package alexeyzhizhensky.watchberries.data

import alexeyzhizhensky.watchberries.data.tables.Prices
import alexeyzhizhensky.watchberries.data.tables.Products
import alexeyzhizhensky.watchberries.data.tables.Skus
import alexeyzhizhensky.watchberries.data.tables.Users
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.net.URI
import java.time.LocalDateTime
import java.util.UUID

class WatchberriesDatabase private constructor() {

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

    fun getAllSkus() = transaction { Products.selectAll().map { it[Products.sku] } }

    fun insertProduct(product: Product) = transaction {
        Products.insert {
            it[this.sku] = product.sku
            it[brand] = product.brand
            it[title] = product.title
        }

        Prices.batchInsert(
            data = product.prices.asIterable(),
            shouldReturnGeneratedValues = false
        ) {
            this[Prices.sku] = product.sku
            this[Prices.timestamp] = it.dateTime
            this[Prices.price] = it.price
        }

        product
    }

    fun getProduct(sku: Int) = transaction {
        Products.select { Products.sku eq sku }.singleOrNull()?.let { resultRow ->
            val prices = Prices.select { Prices.sku eq sku }
                .orderBy(Prices.timestamp)
                .map { Price(it[Prices.timestamp], it[Prices.price]) }

            Product(
                sku = sku,
                brand = resultRow[Products.brand],
                title = resultRow[Products.title],
                prices = prices
            )
        }
    }

    fun insertUser(token: String): User? {
        val userId = transaction {
            Users.insertAndGetId {
                it[key] = UUID.randomUUID()
                it[this.token] = token
                it[lastSync] = LocalDateTime.now()
            }.value
        }

        return getUser(userId)
    }

    fun deleteOldUsers(lastDateTime: LocalDateTime) = transaction {
        Users.deleteWhere { Users.lastSync less lastDateTime }
    }

    fun updateUser(id: Int, token: String): User? {
        transaction {
            Users.update({ Users.id eq id }) {
                it[this.token] = token
                it[lastSync] = LocalDateTime.now()
            }
        }

        return getUser(id)
    }

    fun getUser(id: Int): User? {
        val skus = getSkusForUser(id)

        return transaction {
            Users.select { Users.id eq id }.singleOrNull()?.let {
                User(
                    id = it[Users.id].value,
                    token = it[Users.token],
                    key = it[Users.key],
                    lastSync = it[Users.lastSync],
                    skus = skus
                )
            }
        }
    }

    fun getOldUserIds(lastDateTime: LocalDateTime) = transaction {
        Users.select { Users.lastSync less lastDateTime }.map { it[Users.id].value }
    }

    fun addSkuToUser(sku: Int, userId: Int): User? {
        transaction {
            Skus.insert {
                it[this.userId] = userId
                it[this.sku] = sku
            }
        }

        return getUser(userId)
    }

    fun deleteSkuFromUser(sku: Int, userId: Int): User? {
        transaction {
            Skus.deleteWhere { (Skus.userId eq userId) and (Skus.sku eq sku) }
        }

        return getUser(userId)
    }

    fun getSkusForUser(id: Int) = transaction {
        Skus.select { Skus.userId eq id }.map { it[Skus.sku] }
    }

    fun addPriceToProduct(sku: Int, price: Price) = transaction {
        Prices.insert {
            it[this.sku] = sku
            it[timestamp] = price.dateTime
            it[this.price] = price.price
        }
    }

    fun deleteOldPrices(lastDateTime: LocalDateTime) = transaction {
        Prices.deleteWhere { Prices.timestamp less lastDateTime }
    }

    companion object {

        @Volatile
        private var instance: WatchberriesDatabase? = null

        fun getInstance(): WatchberriesDatabase {
            return instance ?: synchronized(this) {
                instance ?: WatchberriesDatabase().also { instance = it }
            }
        }

        private const val DATABASE_URL_KEY = "DATABASE_URL"
        private const val JDBC_URL_START = "jdbc:postgresql://"
        private const val DRIVER_CLASS_NAME = "org.postgresql.Driver"
    }
}
