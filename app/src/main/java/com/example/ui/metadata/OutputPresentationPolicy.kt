package com.example.ui.metadata

import com.example.data.AnalysisType

enum class ListStyle {
    BULLET,
    NUMBERED
}

enum class LayoutType {
    DEFAULT_LIST,
    TOP3_LIST,
    RISK_LIST,
    SCORE_LIST,
    PRO_CONTRA_LIST
}

data class PresentationPolicy(
    val listStyle: ListStyle,
    val showTakeawayIcons: Boolean = true,
    val maxTakeaways: Int? = null,
    val layoutType: LayoutType = LayoutType.DEFAULT_LIST,
    val sectionHeader: String = "WICHTIGSTE KERNAUSSAGEN"
)

object OutputPresentationPolicy {
    fun getPolicyFor(analysisType: AnalysisType?): PresentationPolicy {
        return when (analysisType) {
            AnalysisType.TOP_3_KERNAUSSAGEN, AnalysisType.KEY_TAKEAWAYS -> PresentationPolicy(
                listStyle = ListStyle.NUMBERED,
                showTakeawayIcons = true,
                layoutType = LayoutType.TOP3_LIST
            )
            AnalysisType.RISIKO_ANALYSE, AnalysisType.RISK_ANALYSIS -> PresentationPolicy(
                listStyle = ListStyle.BULLET,
                showTakeawayIcons = true,
                layoutType = LayoutType.RISK_LIST
            )
            AnalysisType.PERSPECTIVES_AND_COUNTERPOSITIONS, AnalysisType.PERSPECTIVES_COUNTERPOSITIONS -> PresentationPolicy(
                listStyle = ListStyle.BULLET,
                showTakeawayIcons = true,
                layoutType = LayoutType.PRO_CONTRA_LIST
            )
            AnalysisType.WEITERE_RELEVANTE_ASPEKTE, AnalysisType.RELEVANT_ASPECTS -> PresentationPolicy(
                listStyle = ListStyle.BULLET,
                showTakeawayIcons = true,
                layoutType = LayoutType.DEFAULT_LIST,
                sectionHeader = "WEITERE RELEVANTE ASPEKTE ZUR QUELLE"
            )
            else -> PresentationPolicy(
                listStyle = ListStyle.BULLET,
                showTakeawayIcons = true,
                layoutType = LayoutType.DEFAULT_LIST
            )
        }
    }
}
