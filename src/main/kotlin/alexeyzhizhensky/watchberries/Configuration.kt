package alexeyzhizhensky.watchberries

import alexeyzhizhensky.watchberries.data.WbDatabase
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class Configuration {

    @Bean
    fun init() = CommandLineRunner {
        WbDatabase.getInstance().connect()

        WbFirebaseMessaging.init()
    }
}
