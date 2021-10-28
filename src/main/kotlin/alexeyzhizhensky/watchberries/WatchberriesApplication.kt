package alexeyzhizhensky.watchberries

import alexeyzhizhensky.watchberries.data.WatchberriesDatabase
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import javax.annotation.PostConstruct

@EnableScheduling
@RestController
@SpringBootApplication
class WatchberriesApplication {

    @GetMapping("/")
    fun index() = "Watchberries server is alive!"

    @GetMapping("/api/")
    fun api() = "Watchberries API"

    @GetMapping("/api/products")
    fun apiProducts() = WatchberriesDatabase.getAllProducts()

    @PostConstruct
    fun init() {
        WatchberriesDatabase.connect()
    }
}

fun main(args: Array<String>) {
    runApplication<WatchberriesApplication>(*args)
}