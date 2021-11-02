package alexeyzhizhensky.watchberries.data.tables

import org.jetbrains.exposed.dao.id.IntIdTable

object Skus : IntIdTable("db.skus") {

    val userId = integer("user_id").references(Users.id)

    val sku = integer("sku")

    init {
        uniqueIndex(userId, sku)
    }
}
