package com.jayanth.hearit

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.activity.ComponentActivity

/**
 * Cold-start launcher activity: renders the branded splash screen with modern audio-lines
 * identity before handing control to [MainActivity].
 */
@SuppressLint("CustomSplashScreen")
class SplashActivity : ComponentActivity() {

    private var launched = false
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.splash_screen)

        playEntranceAnimation()
        scheduleNavigation()
    }

    private fun playEntranceAnimation() {
        val iconContainer = findViewById<View>(R.id.splash_icon_container)
        val title = findViewById<View>(R.id.splash_title)
        val subtitle = findViewById<View>(R.id.splash_subtitle)
        val badge = findViewById<View>(R.id.splash_badge)

        val interpolator = DecelerateInterpolator()

        iconContainer?.apply {
            alpha = 0f
            scaleX = 0.85f
            scaleY = 0.85f
            animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(450)
                .setInterpolator(interpolator)
                .start()
        }

        title?.apply {
            alpha = 0f
            translationY = 24f
            animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(450)
                .setStartDelay(100)
                .setInterpolator(interpolator)
                .start()
        }

        subtitle?.apply {
            alpha = 0f
            translationY = 20f
            animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(450)
                .setStartDelay(180)
                .setInterpolator(interpolator)
                .start()
        }

        badge?.apply {
            alpha = 0f
            animate()
                .alpha(1f)
                .setDuration(400)
                .setStartDelay(260)
                .setInterpolator(interpolator)
                .start()
        }
    }

    private fun scheduleNavigation() {
        handler.postDelayed({
            if (isFinishing || isDestroyed || launched) return@postDelayed
            launched = true
            val intent = Intent(this@SplashActivity, MainActivity::class.java)
            startActivity(intent)
            @Suppress("DEPRECATION")
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }, 850)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
