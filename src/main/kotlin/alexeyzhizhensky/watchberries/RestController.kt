package alexeyzhizhensky.watchberries

import alexeyzhizhensky.watchberries.data.WatchberriesRepository
import alexeyzhizhensky.watchberries.data.requests.SkuRequest
import alexeyzhizhensky.watchberries.data.requests.TokenRequest
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class RestController {

    @GetMapping("/")
    fun index() = "Watchberries server is alive!"

    @GetMapping("/api/")
    fun api() = "Watchberries API"

    @GetMapping("/api/products")
    fun getProducts(
        @RequestParam(required = false) skus: List<Int>?,
        @RequestParam(name = "user_id", required = false) userId: Int?,
        @RequestParam(required = false) key: UUID?
    ) = when {
        skus != null -> WatchberriesRepository.getProducts(skus)
        userId != null && key != null && isAuthCorrect(userId, key) ->
            WatchberriesRepository.getProductsForUser(userId)
        userId == null && key == null -> WatchberriesRepository.getAllProducts() // TEST ONLY
        else -> emptyList()
    }

    @GetMapping("/api/products/{sku}")
    fun getProduct(@PathVariable sku: Int) = WatchberriesRepository.getProduct(sku)

    @PostMapping("/api/users")
    fun postUser(
        @RequestBody tokenRequest: TokenRequest
    ) = WatchberriesRepository.createUser(tokenRequest.token)

    @PutMapping("/api/users/{id}")
    fun updateUser(
        @PathVariable id: Int,
        @RequestParam key: UUID,
        @RequestBody tokenRequest: TokenRequest
    ) = if (isAuthCorrect(id, key)) {
        WatchberriesRepository.updateUser(id, tokenRequest.token)
    } else {
        null
    }

    @PostMapping("/api/users/{id}/skus")
    fun postSkuToUser(
        @PathVariable id: Int,
        @RequestParam key: UUID,
        @RequestBody sku: SkuRequest
    ) = if (isAuthCorrect(id, key)) WatchberriesRepository.addSkuToUser(sku.sku, id) else null

    @DeleteMapping("/api/users/{id}/skus")
    fun deleteSkuFromUser(
        @PathVariable id: Int,
        @RequestParam key: UUID,
        @RequestBody sku: SkuRequest
    ) = if (isAuthCorrect(id, key)) WatchberriesRepository.removeSkuFromUser(sku.sku, id) else null

    private fun isAuthCorrect(id: Int, key: UUID) = WatchberriesRepository.getUser(id)?.key == key
}
