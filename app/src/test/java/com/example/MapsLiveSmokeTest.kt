package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.ui.MainViewModel
import com.example.ui.UiState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MapsLiveSmokeTest {

    @Test
    fun runLiveSmoke3Times() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val testUrl = "https://maps.app.goo.gl/WgXTvya1yCDJjameA"
        
        for (runIndex in 1..3) {
            val runId = java.util.UUID.randomUUID().toString()
            println("=== START LIVE SMOKE RUN $runIndex (Run-ID: $runId) ===")
            val viewModel = MainViewModel()
            viewModel.initIfNeeded(app)
            
            viewModel.fetchSummary(testUrl)
            
            var state = viewModel.uiState.value
            var attempts = 0
            while ((state is UiState.Loading || state is UiState.Idle) && attempts < 120) {
                org.robolectric.shadows.ShadowLooper.idleMainLooper(500, java.util.concurrent.TimeUnit.MILLISECONDS)
                kotlinx.coroutines.delay(500)
                state = viewModel.uiState.value
                attempts++
            }
            
            println("Final UI State for Run $runIndex: ${state::class.simpleName}")
            if (state is UiState.Success) {
                println("SUCCESS: Title = ${state.summary.title}")
                println("SUCCESS: OriginalUrl = ${state.summary.originalUrl}")
                println("SUCCESS: KeyTakeaways count = ${state.summary.keyTakeaways.size}")
                assertTrue(state.summary.originalUrl.contains("google.com") || state.summary.originalUrl.contains("maps.app.goo.gl"))
                assertTrue(state.summary.title.isNotBlank())
            } else if (state is UiState.Error) {
                println("ERROR: Message = ${state.message}")
                fail("Run $runIndex failed with error: ${state.message}")
            } else {
                fail("Run $runIndex timed out or remained in state: $state")
            }
            println("=== END LIVE SMOKE RUN $runIndex: PASS (Run-ID: $runId) ===\n")
        }
    }
}
