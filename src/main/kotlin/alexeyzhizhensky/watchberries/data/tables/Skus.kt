package alexeyzhizhensky.watchberries.data.tables

import org.jetbrains.exposed.dao.id.IntIdTable

object Skus : IntIdTable("db.skus", "id") {

    val user = uuid("user").references(Users.id)

    val sku = integer("sku")

    init {
        uniqueIndex("user_sku", user, sku)
    }
}