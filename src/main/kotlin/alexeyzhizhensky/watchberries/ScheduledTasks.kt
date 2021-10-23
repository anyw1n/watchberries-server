package alexeyzhizhensky.watchberries

import alexeyzhizhensky.watchberries.data.WatchberriesDatabase
import alexeyzhizhensky.watchberries.data.tables.Prices
import alexeyzhizhensky.watchberries.data.tables.Products
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class ScheduledTasks {

    private val log = LoggerFactory.getLogger(ScheduledTasks::class.java)

    @Scheduled(fixedRate = UPDATE_INTERVAL)
    fun updateProductsPrices() {
        log.info("Start prices update...")

        transaction {
            Products.slice(Products.sku).selectAll().map { it[Products.sku] }
        }.forEach(WatchberriesDatabase::updatePriceForProduct)

        log.info("Prices updated.")
    }

    @Scheduled(cron = "0 3 0 * * *")
    fun deleteOldPrices() {
        log.info("Start deleting old prices...")

        val lastPossibleDateTime = LocalDateTime.now().minusMonths(3)

        transaction {
            Prices.deleteWhere { Prices.timestamp less lastPossibleDateTime }
        }

        log.info("Old prices deleted.")
    }

    private companion object {

        private const val UPDATE_INTERVAL = 3 * 60 * 60 * 1000L
    }
}