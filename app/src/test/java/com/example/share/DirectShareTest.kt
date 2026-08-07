package com.example.share

import android.content.Context
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.test.core.app.ApplicationProvider
import com.example.data.AnalysisType
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class DirectShareTest {

    @Test
    fun testShortcutIdMapping() {
        assertEquals(
            AnalysisType.GOOGLE_MAPS_ANALYZER,
            DirectShareManager.getAnalysisTypeForShortcutId("shortcut_maps_analyzer")
        )
        assertEquals(
            AnalysisType.WEB_SUMMARY,
            DirectShareManager.getAnalysisTypeForShortcutId("shortcut_web_summary")
        )
        assertEquals(
            AnalysisType.GOOGLE_MAPS_LOCATION_CONTEXT,
            DirectShareManager.getAnalysisTypeForShortcutId("shortcut_maps_context")
        )
        assertNull(DirectShareManager.getAnalysisTypeForShortcutId("unknown_id"))
    }

    @Test
    fun testShortcutsUpdate() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val result = DirectShareManager.updateShortcuts(context)
        val shortcuts = ShortcutManagerCompat.getDynamicShortcuts(context)

        if (DirectShareManager.isPersonalEnabled) {
            assertEquals("published", result.action)
            assertEquals(3, result.publishedCount)
            assertEquals(3, result.dynamicShortcutCountAfter)
            assertEquals(3, shortcuts.size)

            val ids = shortcuts.map { it.id }.toSet()
            assertTrue(ids.contains("shortcut_maps_analyzer"))
            assertTrue(ids.contains("shortcut_web_summary"))
            assertTrue(ids.contains("shortcut_maps_context"))

            shortcuts.forEach { shortcut ->
                assertTrue(shortcut.categories?.contains("com.example.share.category.TEXT_SHARE_TARGET") == true)
                assertNotNull(shortcut.intent)
                assertEquals(android.content.Intent.ACTION_SEND, shortcut.intent?.action)
                assertEquals("text/plain", shortcut.intent?.type)
                val extraId = shortcut.intent?.getStringExtra(ShortcutManagerCompat.EXTRA_SHORTCUT_ID)
                assertEquals(shortcut.id, extraId)
            }
        } else {
            assertEquals("removed", result.action)
            assertEquals(0, result.publishedCount)
            assertEquals(0, shortcuts.size)
        }
    }
}
