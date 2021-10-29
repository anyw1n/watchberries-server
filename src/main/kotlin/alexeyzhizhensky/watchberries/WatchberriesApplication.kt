package alexeyzhizhensky.watchberries

import alexeyzhizhensky.watchberries.data.WatchberriesDatabase
import com.google.gson.Gson
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import javax.annotation.PostConstruct

@EnableScheduling
@RestController
@SpringBootApplication
class WatchberriesApplication {

    private val gson = Gson()

    @GetMapping("/")
    fun index() = "Watchberries server is alive!"

    @GetMapping("/api/")
    fun api() = "Watchberries API"

    @GetMapping("/api/products")
    fun apiProducts(@RequestParam(required = false) sku: List<Int>?): String {
        val products = if (sku != null) {
            WatchberriesDatabase.getProducts(sku)
        } else {
            WatchberriesDatabase.getAllProducts()
        }

        return gson.toJson(products)
    }

    @PostConstruct
    fun init() {
        WatchberriesDatabase.connect()
    }
}

fun main(args: Array<String>) {
    runApplication<WatchberriesApplication>(*args)
}