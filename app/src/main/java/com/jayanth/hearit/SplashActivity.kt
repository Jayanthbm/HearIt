package com.jayanth.hearit

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity

/**
 * Cold-start surface: shows the branded splash drawable while the app boots, then hands
 * control to [MainActivity], which renders the real Compose UI.
 *
 * It has no Compose content, no service start and no navigation logic — it only delays the
 * transition so the adaptive icon and the splash drawable are painted on a real window
 * instead of a black frame. The theme transition in [MainActivity] is instant.
 */
class SplashActivity : ComponentActivity() {

    private var launched = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.splash_screen)
        // Defer the real launch until the next frame so the splash drawable is painted
        // first and then replaced by the Compose UI. This is a visual delay, not a
        // business delay: nothing here starts the announcement service.
        onNextFrame()
    }

    private fun onNextFrame() {
        // Use the activity's Looper to schedule the launch on the UI thread.
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        handler.post {
            if (launched) return@post
            launched = true
            finish()
            startActivity(Intent(this, MainActivity::class.java))
        }
    }
}
