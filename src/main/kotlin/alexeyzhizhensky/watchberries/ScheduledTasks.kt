package alexeyzhizhensky.watchberries

import alexeyzhizhensky.watchberries.data.WatchberriesRepository
import alexeyzhizhensky.watchberries.utils.Constants.OLD_PERIOD_MONTHS
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class ScheduledTasks {

    private val log = LoggerFactory.getLogger(ScheduledTasks::class.java)

    @Scheduled(cron = "0 0 * * * *")
    fun updateProductsPrices() {
        log.info("Start prices update...")

        WatchberriesRepository.updatePrices()

        log.info("Prices updated.")
    }

    @Scheduled(cron = "0 0 0 * * *")
    fun deleteOldPrices() {
        log.info("Start deleting old prices...")

        val lastDateTime = LocalDateTime.now().minusMonths(OLD_PERIOD_MONTHS)

        WatchberriesRepository.deleteOldPrices(lastDateTime)

        log.info("Old prices deleted.")
    }

    @Scheduled(cron = "0 0 0 * * *")
    fun deleteOldUsers() {
        val lastDateTime = LocalDateTime.now().minusMonths(OLD_PERIOD_MONTHS)

        WatchberriesRepository.removeOldUsers(lastDateTime)
    }
}
