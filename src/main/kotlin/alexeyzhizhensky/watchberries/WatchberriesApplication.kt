package alexeyzhizhensky.watchberries

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@SpringBootApplication
class WatchberriesApplication

fun main(args: Array<String>) {
    runApplication<WatchberriesApplication>(*args)
}
