package alexeyzhizhensky.watchberries.data.tables

import org.jetbrains.exposed.dao.id.IntIdTable

object Skus : IntIdTable("db.skus") {

    val user = text("user").references(Users.id)

    val sku = integer("sku")

    init {
        uniqueIndex(user, sku)
    }
}
