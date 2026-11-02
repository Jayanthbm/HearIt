package com.jayanth.hearit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.jayanth.hearit.setup.SetupRoute
import com.jayanth.hearit.setup.SetupViewModel
import com.jayanth.hearit.ui.theme.HearItTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * The single activity (master prompt: single activity, MVVM, Hilt DI).
 *
 * Until P12 adds the navigation shell, the activity hosts one destination: the setup flow
 * while `setup.completed` is false, and a minimal completion screen afterwards. The
 * ViewModel is Hilt-injected and survives configuration changes; all flow state is also
 * persisted in DataStore, so process death resumes on the same step.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val setupViewModel: SetupViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HearItTheme {
                SetupRoute(viewModel = setupViewModel)
            }
        }
    }
}
