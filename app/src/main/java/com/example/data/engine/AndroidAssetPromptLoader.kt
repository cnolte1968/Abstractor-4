package com.example.data.engine

import android.content.Context
import com.example.domain.engine.PromptAssetLoader

class AndroidAssetPromptLoader(private val context: Context) : PromptAssetLoader {
    override fun loadAsset(path: String): String {
        return context.assets.open(path).use { input ->
            input.bufferedReader().use { it.readText() }
        }
    }
}
