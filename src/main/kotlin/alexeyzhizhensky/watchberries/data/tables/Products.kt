package alexeyzhizhensky.watchberries.data.tables

import org.jetbrains.exposed.sql.Table

object Products : Table("db.products") {

    val sku = integer("sku")

    val brand = text("brand")

    val title = text("title")

    override val primaryKey = PrimaryKey(sku)
}