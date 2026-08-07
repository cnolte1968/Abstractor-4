package com.example.share

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.BuildConfig
import com.example.MainActivity
import com.example.data.AnalysisType

data class DirectSharePublishResult(
    val gateEnabled: Boolean,
    val action: String,
    val requestedIds: List<String> = emptyList(),
    val publishedCount: Int = 0,
    val dynamicShortcutCountAfter: Int = 0,
    val errorMessage: String? = null
)

object DirectShareManager {

    private const val SHORTCUT_MAPS_ANALYZER = "shortcut_maps_analyzer"
    private const val SHORTCUT_WEB_SUMMARY = "shortcut_web_summary"
    private const val SHORTCUT_MAPS_CONTEXT = "shortcut_maps_context"

    private const val CATEGORY_SHARE_TARGET = "com.example.share.category.TEXT_SHARE_TARGET"

    val isPersonalEnabled: Boolean
        get() = try {
            "true".equals(BuildConfig.DIRECT_SHARE_PERSONAL_ENABLED, ignoreCase = true)
        } catch (e: Exception) {
            false
        }

    fun getAnalysisTypeForShortcutId(shortcutId: String?): AnalysisType? {
        return when (shortcutId) {
            SHORTCUT_MAPS_ANALYZER -> AnalysisType.GOOGLE_MAPS_ANALYZER
            SHORTCUT_WEB_SUMMARY -> AnalysisType.WEB_SUMMARY
            SHORTCUT_MAPS_CONTEXT -> AnalysisType.GOOGLE_MAPS_LOCATION_CONTEXT
            else -> null
        }
    }

    fun updateShortcuts(context: Context): DirectSharePublishResult {
        val myShortcutIds = setOf(SHORTCUT_MAPS_ANALYZER, SHORTCUT_WEB_SUMMARY, SHORTCUT_MAPS_CONTEXT)
        val enabled = isPersonalEnabled

        if (!enabled) {
            return try {
                val existing = ShortcutManagerCompat.getDynamicShortcuts(context)
                val shortcutsToRemove = existing.map { it.id }.filter { it in myShortcutIds }
                if (shortcutsToRemove.isNotEmpty()) {
                    ShortcutManagerCompat.removeDynamicShortcuts(context, shortcutsToRemove)
                }
                val countAfter = ShortcutManagerCompat.getDynamicShortcuts(context).count { it.id in myShortcutIds }
                DirectSharePublishResult(
                    gateEnabled = false,
                    action = "removed",
                    requestedIds = myShortcutIds.toList(),
                    publishedCount = 0,
                    dynamicShortcutCountAfter = countAfter
                )
            } catch (e: Exception) {
                Log.e("DirectShareManager", "Error removing shortcuts", e)
                DirectSharePublishResult(
                    gateEnabled = false,
                    action = "failed",
                    requestedIds = myShortcutIds.toList(),
                    errorMessage = e.message
                )
            }
        }

        return try {
            val createIntent = { id: String ->
                Intent(context, MainActivity::class.java).apply {
                    action = Intent.ACTION_SEND
                    type = "text/plain"
                    putExtra(ShortcutManagerCompat.EXTRA_SHORTCUT_ID, id)
                    putExtra("android.intent.extra.shortcut.ID", id)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
            }

            val mapsAnalyzerShortcut = ShortcutInfoCompat.Builder(context, SHORTCUT_MAPS_ANALYZER)
                .setShortLabel("Maps Analyzer")
                .setLongLabel("Relevantor: GoogleMaps-Analyzer")
                .setIcon(IconCompat.createWithResource(context, com.example.R.mipmap.ic_launcher))
                .setCategories(setOf(CATEGORY_SHARE_TARGET))
                .setIntent(createIntent(SHORTCUT_MAPS_ANALYZER))
                .setRank(0)
                .build()

            val webSummaryShortcut = ShortcutInfoCompat.Builder(context, SHORTCUT_WEB_SUMMARY)
                .setShortLabel("Zusammenfassung")
                .setLongLabel("Relevantor: Zusammenfassung")
                .setIcon(IconCompat.createWithResource(context, com.example.R.mipmap.ic_launcher))
                .setCategories(setOf(CATEGORY_SHARE_TARGET))
                .setIntent(createIntent(SHORTCUT_WEB_SUMMARY))
                .setRank(1)
                .build()

            val mapsContextShortcut = ShortcutInfoCompat.Builder(context, SHORTCUT_MAPS_CONTEXT)
                .setShortLabel("Kontext zum Ort")
                .setLongLabel("Relevantor: Kontext zum Ort")
                .setIcon(IconCompat.createWithResource(context, com.example.R.mipmap.ic_launcher))
                .setCategories(setOf(CATEGORY_SHARE_TARGET))
                .setIntent(createIntent(SHORTCUT_MAPS_CONTEXT))
                .setRank(2)
                .build()

            val shortcutsToAdd = listOf(mapsAnalyzerShortcut, webSummaryShortcut, mapsContextShortcut)

            val existingShortcuts = ShortcutManagerCompat.getDynamicShortcuts(context)
            if (existingShortcuts.isEmpty() || existingShortcuts.all { it.id in myShortcutIds }) {
                ShortcutManagerCompat.setDynamicShortcuts(context, shortcutsToAdd)
            } else {
                ShortcutManagerCompat.addDynamicShortcuts(context, shortcutsToAdd)
            }

            val afterList = ShortcutManagerCompat.getDynamicShortcuts(context)
            val published = afterList.filter { it.id in myShortcutIds }

            Log.i("DirectShareManager", "Published ${published.size} shortcuts: ${published.map { it.id }}")

            DirectSharePublishResult(
                gateEnabled = true,
                action = "published",
                requestedIds = myShortcutIds.toList(),
                publishedCount = published.size,
                dynamicShortcutCountAfter = published.size
            )
        } catch (e: Exception) {
            Log.e("DirectShareManager", "Error publishing shortcuts", e)
            DirectSharePublishResult(
                gateEnabled = true,
                action = "failed",
                requestedIds = myShortcutIds.toList(),
                errorMessage = e.message
            )
        }
    }
}
