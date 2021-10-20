package alexeyzhizhensky.watchberries

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class WatchberriesApplication

fun main(args: Array<String>) {
	runApplication<WatchberriesApplication>(*args)
}
