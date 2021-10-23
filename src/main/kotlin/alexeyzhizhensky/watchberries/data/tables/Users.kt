package alexeyzhizhensky.watchberries.data.tables

import org.jetbrains.exposed.dao.id.UUIDTable

object Users : UUIDTable("db.users", "id")