package alexeyzhizhensky.watchberries.data

import com.google.gson.Gson
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class RestController {

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
}