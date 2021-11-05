package alexeyzhizhensky.watchberries

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging

object WbFirebaseMessaging {

    private lateinit var app: FirebaseMessaging

    fun init() {
        val options = FirebaseOptions.builder()
            .setCredentials(GoogleCredentials.getApplicationDefault())
            .build()

        app = FirebaseMessaging.getInstance(FirebaseApp.initializeApp(options))
    }
}
