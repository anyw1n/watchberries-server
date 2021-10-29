package alexeyzhizhensky.watchberries

import alexeyzhizhensky.watchberries.data.WatchberriesDatabase
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class ScheduledTasks {

    private val log = LoggerFactory.getLogger(ScheduledTasks::class.java)

    @Scheduled(cron = "0 0 */3 * * *")
    fun updateProductsPrices() {
        log.info("Start prices update...")

        WatchberriesDatabase.updatePrices()

        log.info("Prices updated.")
    }

    @Scheduled(cron = "0 0 0 * * *")
    fun deleteOldPrices() {
        log.info("Start deleting old prices...")

        val lastDateTime = LocalDateTime.now().minusMonths(3)

        WatchberriesDatabase.deleteOldPrices(lastDateTime)

        log.info("Old prices deleted.")
    }
}