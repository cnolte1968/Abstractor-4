package com.example.domain.engine

interface PromptAssetLoader {
    fun loadAsset(path: String): String
}
