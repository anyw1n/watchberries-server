package alexeyzhizhensky.watchberries

import alexeyzhizhensky.watchberries.data.WatchberriesDatabase
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class DatabaseConfiguration {

    @Bean
    fun initDatabase() = CommandLineRunner {
        WatchberriesDatabase.connect()
    }
}
