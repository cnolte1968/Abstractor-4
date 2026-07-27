package com.example.data.extraction

import android.content.Context
import com.example.data.AnalysisType

class InputExtractorRegistry(private val context: Context) {

    private val extractors: List<InputExtractor> = listOf(
        YoutubeInputExtractor(),
        DocumentInputExtractor(context),
        WebInputExtractor()
    )

    fun getExtractor(
        rawUrl: String,
        normalizedUrl: String,
        directContent: String?,
        analysisType: AnalysisType
    ): InputExtractor? {
        return extractors.find { it.supports(rawUrl, normalizedUrl, directContent, analysisType) }
    }
}
