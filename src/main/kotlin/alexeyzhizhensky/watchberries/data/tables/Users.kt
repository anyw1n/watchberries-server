package alexeyzhizhensky.watchberries.data.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

object Users : Table("db.users") {

    val id = text("id")

    val key = uuid("key").uniqueIndex()

    val token = text("token")

    val lastSync = datetime("last_sync")

    override val primaryKey = PrimaryKey(id)
}
