package alexeyzhizhensky.watchberries.data.tables

import org.jetbrains.exposed.dao.id.UUIDTable

object Users : UUIDTable("db.users", "id")// TODO: 10/28/2021 make user contain firebase inst id