package alexeyzhizhensky.watchberries.data

import alexeyzhizhensky.watchberries.data.requests.TokenRequest
import alexeyzhizhensky.watchberries.data.requests.UserRequest
import alexeyzhizhensky.watchberries.data.tables.Prices
import alexeyzhizhensky.watchberries.data.tables.Products
import alexeyzhizhensky.watchberries.data.tables.Skus
import alexeyzhizhensky.watchberries.data.tables.Users
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.net.URI
import java.time.LocalDateTime

object WatchberriesDatabase {

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

    fun insertUser(userRequest: UserRequest) = transaction {
        val user = User(userRequest.id, userRequest.token)

        Users.insert {
            it[id] = user.id
            it[this.key] = user.key
            it[token] = user.token
            it[lastSync] = user.lastSync
        }

        user
    }

    fun deleteOldUsers(lastDateTime: LocalDateTime) = transaction {
        Users.deleteWhere { Users.lastSync eq lastDateTime }
    }

    fun updateUser(id: String, tokenRequest: TokenRequest) = transaction {
        Users.update({ Users.id eq id }) {
            it[token] = tokenRequest.token
            it[lastSync] = LocalDateTime.now()
        }

        getUser(id)
    }

    fun getUser(id: String): User? {
        val skus = getSkusForUser(id)

        return transaction {
            Users.select { Users.id eq id }.singleOrNull()?.let {
                User(
                    id = it[Users.id],
                    token = it[Users.token],
                    key = it[Users.key],
                    lastSync = it[Users.lastSync],
                    skus = skus
                )
            }
        }
    }

    fun getOldUserIds(lastDateTime: LocalDateTime) = transaction {
        Users.select { Users.lastSync less lastDateTime }.map { it[Users.id] }
    }

    fun addSkuToUser(sku: Int, userId: String): User? {
        transaction {
            Skus.insert {
                it[user] = userId
                it[this.sku] = sku
            }
        }

        return getUser(userId)
    }

    fun deleteSkuFromUser(sku: Int, userId: String): User? {
        transaction {
            Skus.deleteWhere { (Skus.user eq userId) and (Skus.sku eq sku) }
        }

        return getUser(userId)
    }

    fun getSkusForUser(id: String) = transaction {
        Skus.select { Skus.user eq id }.map { it[Skus.sku] }
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

    private const val DATABASE_URL_KEY = "DATABASE_URL"
    private const val JDBC_URL_START = "jdbc:postgresql://"
    private const val DRIVER_CLASS_NAME = "org.postgresql.Driver"
}
