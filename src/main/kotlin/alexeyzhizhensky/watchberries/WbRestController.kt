package alexeyzhizhensky.watchberries

import alexeyzhizhensky.watchberries.data.Product
import alexeyzhizhensky.watchberries.data.WatchberriesRepository
import alexeyzhizhensky.watchberries.data.requests.SkuRequest
import alexeyzhizhensky.watchberries.data.requests.TokenRequest
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
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
class WbRestController {

    @GetMapping("/")
    fun index() = "Watchberries server is alive!"

    @GetMapping("/api/")
    fun api() = "Watchberries API"

    @GetMapping("/api/products")
    fun getProducts(
        @RequestParam(required = false) skus: List<Int>?,
        @RequestParam(name = "user_id", required = false) userId: Int?,
        @RequestParam(required = false) key: UUID?,
        @RequestParam(required = false) page: Int?,
        @RequestParam(required = false) limit: Int?
    ): ResponseEntity<List<Product?>> {
        val (pages, body) = when {
            skus != null -> WbRepository.getProducts(skus, page, limit)
            userId != null && key != null && isAuthCorrect(userId, key) ->
                WbRepository.getProductsForUser(userId, page, limit)
            userId == null && key == null -> WbRepository.getAllProducts(page, limit)
            else -> 0 to emptyList()
        }

        val headers = HttpHeaders().apply {
            set("Pagination-Pages", pages.toString())
        }

        return ResponseEntity(body, headers, HttpStatus.OK)
    }

    @GetMapping("/api/products/{sku}")
    fun getProduct(@PathVariable sku: Int) = WbRepository.getProduct(sku)

    @PostMapping("/api/users")
    fun postUser(
        @RequestBody tokenRequest: TokenRequest
    ) = WbRepository.createUser(tokenRequest.token)

    @PutMapping("/api/users/{id}")
    fun updateUser(
        @PathVariable id: Int,
        @RequestParam key: UUID,
        @RequestBody tokenRequest: TokenRequest
    ) = if (isAuthCorrect(id, key)) WbRepository.updateUser(id, tokenRequest.token) else null

    @PostMapping("/api/users/{id}/skus")
    fun postSkuToUser(
        @PathVariable id: Int,
        @RequestParam key: UUID,
        @RequestBody sku: SkuRequest
    ) = if (isAuthCorrect(id, key)) WbRepository.addSkuToUser(sku.sku, id) else null

    @DeleteMapping("/api/users/{id}/skus")
    fun deleteSkuFromUser(
        @PathVariable id: Int,
        @RequestParam key: UUID,
        @RequestBody sku: SkuRequest
    ) = if (isAuthCorrect(id, key)) WbRepository.removeSkuFromUser(sku.sku, id) else null

    private fun isAuthCorrect(id: Int, key: UUID) = WbRepository.getUser(id)?.key == key
}
