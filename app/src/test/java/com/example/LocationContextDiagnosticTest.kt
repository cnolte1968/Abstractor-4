package com.example

import androidx.test.core.app.ApplicationProvider
import com.example.data.contextengine.LocationContextDiagnosticRunner
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LocationContextDiagnosticTest {

    @Test
    fun testLocationContextDiagnosticRunnerExecution() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        val reportChiangMai = LocationContextDiagnosticRunner.runDiagnosis(
            context = context,
            inputUrl = "https://maps.google.com/?q=Chiang+Mai",
            customPlaceName = "Chiang Mai"
        )
        System.err.println("--- CHIANG MAI REPORT ---")
        System.err.println(reportChiangMai.toFormattedString())

        val reportDoiSuthep = LocationContextDiagnosticRunner.runDiagnosis(
            context = context,
            inputUrl = "https://maps.google.com/?q=Wat+Phra+That+Doi+Suthep",
            customPlaceName = "Wat Phra That Doi Suthep"
        )
        System.err.println("--- DOI SUTHEP REPORT ---")
        System.err.println(reportDoiSuthep.toFormattedString())

        val reportGreenhouse = LocationContextDiagnosticRunner.runDiagnosis(
            context = context,
            inputUrl = "https://maps.google.com/?q=Greenhouse+Community+Space",
            customPlaceName = "Greenhouse Community Space"
        )
        System.err.println("--- GREENHOUSE REPORT ---")
        System.err.println(reportGreenhouse.toFormattedString())

        // Verify Runner executes and produces a valid report structure
        check(reportChiangMai.placeName == "Chiang Mai")
        check(reportChiangMai.googleMapsResultStatus.startsWith("SUCCESS"))

        check(reportDoiSuthep.placeName == "Wat Phra That Doi Suthep")
        check(reportDoiSuthep.googleMapsResultStatus.startsWith("SUCCESS"))

        check(reportGreenhouse.placeName == "Greenhouse Community Space")
        check(reportGreenhouse.googleMapsResultStatus.startsWith("SUCCESS"))
        check(reportGreenhouse.geminiContextInjectionHasGoogleMapsBase)
    }
}
