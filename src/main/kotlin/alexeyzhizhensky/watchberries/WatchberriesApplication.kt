package alexeyzhizhensky.watchberries

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@SpringBootApplication
class WatchberriesApplication {

    @GetMapping("/")
    fun index() = "Watchberries server is alive!"
    
    @GetMapping("/api/")
    fun api() = "Watchberries API"
}

fun main(args: Array<String>) {
    runApplication<WatchberriesApplication>(*args)
}