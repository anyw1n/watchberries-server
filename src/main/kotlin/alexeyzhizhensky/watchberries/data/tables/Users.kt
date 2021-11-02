package alexeyzhizhensky.watchberries.data.tables

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.datetime

object Users : IntIdTable("db.users") {

    val key = uuid("key").uniqueIndex()

    val token = text("token")

    val lastSync = datetime("last_sync")
}
