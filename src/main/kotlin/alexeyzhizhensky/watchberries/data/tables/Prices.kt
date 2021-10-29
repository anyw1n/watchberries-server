package alexeyzhizhensky.watchberries.data.tables

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.datetime

object Prices : IntIdTable("db.prices", "id") {

    val sku = integer("sku").references(Products.sku, ReferenceOption.CASCADE)

    val timestamp = datetime("timestamp")

    val price = integer("price")

    init {
        uniqueIndex(sku, timestamp)
    }
}