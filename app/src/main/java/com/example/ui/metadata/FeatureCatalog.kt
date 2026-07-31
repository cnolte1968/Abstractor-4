package com.example.ui.metadata

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.data.AnalysisType

enum class AcceptedInput {
    WEB,
    DOCUMENT,
    MULTIMEDIA,
    IMAGE,
    MULTI_URL
}

data class FeatureMetadata(
    val functionId: String,
    val analysisType: AnalysisType?,
    val name: String,
    val description: String,
    val category: String, // Category ID like "A", "B" etc.
    val sortOrder: Int,
    val icon: ImageVector,
    val color: Color,
    val enabled: Boolean,
    val visible: Boolean = true,
    val isPlaceholder: Boolean = false,
    val acceptedInputs: Set<AcceptedInput> = setOf(AcceptedInput.WEB)
)

data class CategoryMetadata(
    val id: String,
    val label: String,
    val name: String,
    val icon: ImageVector,
    val color: Color,
    val sortOrder: Int
)

object FeatureCatalog {
    val categories: List<CategoryMetadata> = listOf(
        CategoryMetadata("A", "", "Verstehen & Verdichten", Icons.Default.AutoStories, Color(0xFF2563EB), 1),
        CategoryMetadata("B", "", "Qualität, Kritik & Einordnung", Icons.Default.Shield, Color(0xFF4F46E5), 2),
        CategoryMetadata("E", "", "Arbeiten mit Dateien", Icons.Default.Folder, Color(0xFF0891B2), 3),
        CategoryMetadata("D", "", "Inhalte verarbeiten", Icons.Default.Share, Color(0xFFD97706), 4),
        CategoryMetadata("C", "", "Visualisierung", Icons.Default.Palette, Color(0xFF059669), 5),
        CategoryMetadata("F", "", "Google Maps", Icons.Default.Place, Color(0xFFEA4335), 6)
    ).sortedBy { it.sortOrder }

    val features: List<FeatureMetadata> = listOf(
        // Category A
        FeatureMetadata("WEB_SUMMARY", AnalysisType.WEB_SUMMARY, "Zusammenfassung", "Quelle kompakt zusammenfassen", "A", 1, Icons.Default.Description, Color(0xFF2563EB), enabled = true, acceptedInputs = setOf(AcceptedInput.WEB)),
        FeatureMetadata("KEY_TAKEAWAYS", AnalysisType.KEY_TAKEAWAYS, "3 Kernaussagen", "Wichtigste Erkenntnisse extrahieren", "A", 2, Icons.Default.FormatListNumbered, Color(0xFF2563EB), enabled = true, acceptedInputs = setOf(AcceptedInput.WEB)),
        FeatureMetadata("FREE_SOURCE_QUERY", AnalysisType.FREE_SOURCE_QUERY, "Frage an die Quelle", "Spezifische Fragen stellen", "A", 3, Icons.Default.QuestionAnswer, Color(0xFF2563EB), enabled = true, acceptedInputs = setOf(AcceptedInput.WEB)),
        FeatureMetadata("MULTIMEDIA_ANALYSIS", AnalysisType.MULTIMEDIA_ANALYSIS, "Video- & Multimedia-Analyse", "Video- & Podcast-Inhalte zusammenfassen", "A", 4, Icons.Default.PlayArrow, Color(0xFF2563EB), enabled = true, visible = false, acceptedInputs = setOf(AcceptedInput.WEB, AcceptedInput.MULTIMEDIA)),

        // Category B
        FeatureMetadata("FRESHNESS_CHECK", AnalysisType.FRESHNESS_CHECK, "Aktualitäts-Check", "Aktualität der Inhalte bewerten", "B", 1, Icons.Default.Event, Color(0xFF4F46E5), enabled = true, acceptedInputs = setOf(AcceptedInput.WEB)),
        FeatureMetadata("MISINFORMATION_RADAR", AnalysisType.MISINFORMATION_RADAR, "Fehlinformations-Radar", "Fragwürdige Aussagen erkennen", "B", 2, Icons.Default.Radar, Color(0xFF4F46E5), enabled = true, acceptedInputs = setOf(AcceptedInput.WEB)),
        FeatureMetadata("FACTS_VS_OPINIONS", AnalysisType.FACTS_VS_OPINIONS, "Fakt-oder-Meinung", "Fakten, Meinungen und Spekulationen trennen", "B", 3, Icons.Default.Balance, Color(0xFF4F46E5), enabled = true, acceptedInputs = setOf(AcceptedInput.WEB)),
        FeatureMetadata("RISK_ANALYSIS", AnalysisType.RISK_ANALYSIS, "Risikoanalyse", "Risiken identifizieren", "B", 4, Icons.Default.Warning, Color(0xFF4F46E5), enabled = true, acceptedInputs = setOf(AcceptedInput.WEB)),
        FeatureMetadata("PERSPECTIVES_COUNTERPOSITIONS", AnalysisType.PERSPECTIVES_COUNTERPOSITIONS, "Perspektiven- & Gegenpositionen-Finder", "Fehlende Sichtweisen und Gegenargumente aufdecken", "B", 5, Icons.Default.People, Color(0xFF4F46E5), enabled = true, acceptedInputs = setOf(AcceptedInput.WEB)),
        FeatureMetadata("RELEVANT_ASPECTS", AnalysisType.RELEVANT_ASPECTS, "Weitere relevante Aspekte", "Zusätzliche für das Thema relevante Gesichtspunkte identifizieren", "B", 6, Icons.Default.Add, Color(0xFF4F46E5), enabled = true, acceptedInputs = setOf(AcceptedInput.WEB)),

        // Category C
        FeatureMetadata("INFOGRAPHIC_GENERATOR", null, "Infografik-Generator", "Infografiken erzeugen", "C", 1, Icons.Default.Image, Color(0xFF059669), enabled = false, isPlaceholder = true, acceptedInputs = setOf(AcceptedInput.WEB)),
        FeatureMetadata("STRUCTURE_VISUALIZER", null, "Struktur-Visualisierer", "Mindmaps, Wissenslandkarten und Prozessdiagramme erzeugen", "C", 2, Icons.Default.AccountTree, Color(0xFF059669), enabled = false, isPlaceholder = true, acceptedInputs = setOf(AcceptedInput.WEB)),
        FeatureMetadata("IMAGE_IDEA_GENERATOR", null, "Bildideen-Generator", "Bild-, Illustrations-, Comic- und Teaserbild-Prompts erzeugen", "C", 3, Icons.Default.Lightbulb, Color(0xFF059669), enabled = false, isPlaceholder = true, acceptedInputs = setOf(AcceptedInput.WEB)),

        // Category D
        FeatureMetadata("SOCIAL_MEDIA_GENERATOR", null, "Social-Media-Generator", "LinkedIn-, X-, Facebook- und Instagram-Posts erzeugen", "D", 1, Icons.Default.Campaign, Color(0xFFD97706), enabled = false, isPlaceholder = true, acceptedInputs = setOf(AcceptedInput.WEB)),
        FeatureMetadata("COMMUNICATION_GENERATOR", null, "Kommunikations-Generator", "Empfehlungsmails, Pressemitteilungen und Kommentare erzeugen", "D", 2, Icons.Default.Mail, Color(0xFFD97706), enabled = false, isPlaceholder = true, acceptedInputs = setOf(AcceptedInput.WEB)),
        FeatureMetadata("MULTI_URL_SUMMARY", null, "Zusammenfassung aus mehreren URL", "Mehrere Quellen zusammenfassen", "D", 3, Icons.Default.Layers, Color(0xFFD97706), enabled = false, isPlaceholder = true, acceptedInputs = setOf(AcceptedInput.MULTI_URL)),

        // Category F (Google Maps)
        FeatureMetadata("GOOGLE_MAPS_ANALYZER", AnalysisType.GOOGLE_MAPS_ANALYZER, "Google Maps Analyser", "Ortsparameter und Places API Details analysieren", "F", 1, Icons.Default.Place, Color(0xFFEA4335), enabled = true, acceptedInputs = setOf(AcceptedInput.WEB)),
        FeatureMetadata("GOOGLE_MAPS_LOCATION_CONTEXT", AnalysisType.GOOGLE_MAPS_LOCATION_CONTEXT, "Kontext zum Ort", "Umfeld-, Orts- und Hintergrundkontext analysieren", "F", 2, Icons.Default.Map, Color(0xFFEA4335), enabled = true, acceptedInputs = setOf(AcceptedInput.WEB)),

        // Category E
        FeatureMetadata("DOCUMENT_SUMMARY", AnalysisType.DOCUMENT_SUMMARY, "Dokument zusammenfassen", "Dateiinhalt kompakt analysieren", "E", 1, Icons.Default.Article, Color(0xFF0891B2), enabled = true, acceptedInputs = setOf(AcceptedInput.DOCUMENT)),
        FeatureMetadata("PHOTO_SCREENSHOT_ANALYSIS", AnalysisType.PHOTO_SCREENSHOT_ANALYSIS, "Foto & Screenshots auswerten", "Bildinhalte beschreiben und einordnen", "E", 2, Icons.Default.AddAPhoto, Color(0xFF0891B2), enabled = true, isPlaceholder = false, acceptedInputs = setOf(AcceptedInput.IMAGE)),
        FeatureMetadata("AI_IMAGE_DETECTOR", null, "Bild mit KI erzeugt?", "Hinweise auf KI-generierte oder manipulierte Bilder prüfen", "E", 3, Icons.Default.Image, Color(0xFF0891B2), enabled = false, isPlaceholder = true, acceptedInputs = setOf(AcceptedInput.IMAGE))
    ).sortedWith(compareBy({ it.category }, { it.sortOrder }))
}
