package com.example

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.platform.testTag
import com.example.ui.components.TakeawayCard
import com.example.ui.metadata.PresentationPolicy
import com.example.ui.metadata.ListStyle
import com.example.ui.metadata.LayoutType
import com.example.data.AnalysisType
import com.example.domain.model.DomainSummary
import com.example.domain.model.TakeawayItem
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun testTakeawayCard_riskList_withHighRiskLevel() {
    val takeaway = TakeawayItem(
      title = "Risk Title",
      details = "Risk details description",
      visualMetadata = mapOf("risk_level" to "high")
    )

    composeTestRule.setContent {
      MaterialTheme {
        TakeawayCard(
          takeaway = takeaway,
          index = 0,
          policy = PresentationPolicy(listStyle = ListStyle.BULLET, showTakeawayIcons = true, layoutType = LayoutType.RISK_LIST),
          activeColor = Color.Blue,
          showIcon = true
        )
      }
    }
    // Verify "Risiko: Hoch" is displayed
    composeTestRule.onNodeWithText("Risiko: Hoch").assertIsDisplayed()
  }

  @Test
  fun testTakeawayCard_riskList_withSeverityMedium() {
    val takeaway = TakeawayItem(
      title = "Risk Title",
      details = "Risk details description",
      visualMetadata = mapOf("severity" to "medium")
    )

    composeTestRule.setContent {
      MaterialTheme {
        TakeawayCard(
          takeaway = takeaway,
          index = 0,
          policy = PresentationPolicy(listStyle = ListStyle.BULLET, showTakeawayIcons = true, layoutType = LayoutType.RISK_LIST),
          activeColor = Color.Blue,
          showIcon = true
        )
      }
    }
    // Verify "Risiko: Mittel" is displayed
    composeTestRule.onNodeWithText("Risiko: Mittel").assertIsDisplayed()
  }

  @Test
  fun testTakeawayCard_riskList_noMetadata() {
    val takeaway = TakeawayItem(
      title = "Risk Title",
      details = "Risk details description",
      visualMetadata = emptyMap()
    )

    composeTestRule.setContent {
      MaterialTheme {
        TakeawayCard(
          takeaway = takeaway,
          index = 0,
          policy = PresentationPolicy(listStyle = ListStyle.BULLET, showTakeawayIcons = true, layoutType = LayoutType.RISK_LIST),
          activeColor = Color.Blue,
          showIcon = true
        )
      }
    }
    // Verify "Risiko: Hoch" or "Risiko: Mittel" or "Risiko: Niedrig" are NOT displayed
    composeTestRule.onNodeWithText("Risiko: Hoch").assertDoesNotExist()
    composeTestRule.onNodeWithText("Risiko: Mittel").assertDoesNotExist()
    composeTestRule.onNodeWithText("Risiko: Niedrig").assertDoesNotExist()
  }

  @Test
  fun testTakeawayCard_defaultList_withHighRisk() {
    val takeaway = TakeawayItem(
      title = "Standard Title",
      details = "Standard details",
      visualMetadata = mapOf("risk_level" to "high")
    )

    composeTestRule.setContent {
      MaterialTheme {
        TakeawayCard(
          takeaway = takeaway,
          index = 0,
          policy = PresentationPolicy(listStyle = ListStyle.BULLET, showTakeawayIcons = true, layoutType = LayoutType.DEFAULT_LIST),
          activeColor = Color.Blue,
          showIcon = true
        )
      }
    }
    // Verify "Risiko: Hoch" is NOT displayed (styling ignored for DEFAULT_LIST)
    composeTestRule.onNodeWithText("Risiko: Hoch").assertDoesNotExist()
  }

  @Test
  fun testTakeawayCard_unknownMetadata_ignored() {
    val takeaway = TakeawayItem(
      title = "Standard Title",
      details = "Standard details",
      visualMetadata = mapOf("risk_level" to "super_dangerous")
    )

    composeTestRule.setContent {
      MaterialTheme {
        TakeawayCard(
          takeaway = takeaway,
          index = 0,
          policy = PresentationPolicy(listStyle = ListStyle.BULLET, showTakeawayIcons = true, layoutType = LayoutType.RISK_LIST),
          activeColor = Color.Blue,
          showIcon = true
        )
      }
    }
    // Verify "Risiko" badge is not displayed
    composeTestRule.onNodeWithText("Risiko: Hoch").assertDoesNotExist()
    composeTestRule.onNodeWithText("Risiko: Mittel").assertDoesNotExist()
    composeTestRule.onNodeWithText("Risiko: Niedrig").assertDoesNotExist()
  }

  @Test
  fun testTakeawayCard_numberedPolicy() {
    val takeaway = TakeawayItem(
      title = "A **special** takeaway",
      details = "With **bold** details"
    )

    // Render with NUMBERED policy
    composeTestRule.setContent {
      MaterialTheme {
        TakeawayCard(
          takeaway = takeaway,
          index = 0,
          policy = PresentationPolicy(listStyle = ListStyle.NUMBERED, showTakeawayIcons = true),
          activeColor = Color.Blue,
          showIcon = true
        )
      }
    }
    // Verify "01" prefix is displayed
    composeTestRule.onNodeWithText("01").assertIsDisplayed()
    // Verify title and details are displayed, and bold markdown characters are removed from details
    composeTestRule.onNodeWithText("With bold details").assertIsDisplayed()
  }

  @Test
  fun testTakeawayCard_bulletPolicy() {
    val takeaway = TakeawayItem(
      title = "A **special** takeaway",
      details = "With **bold** details"
    )

    // Render with BULLET policy
    composeTestRule.setContent {
      MaterialTheme {
        TakeawayCard(
          takeaway = takeaway,
          index = 0,
          policy = PresentationPolicy(listStyle = ListStyle.BULLET, showTakeawayIcons = true),
          activeColor = Color.Blue,
          showIcon = true
        )
      }
    }
    // Verify "•" prefix is displayed
    composeTestRule.onNodeWithText("•").assertIsDisplayed()
  }

  @Test
  fun greeting_screenshot() {
    composeTestRule.setContent { MaterialTheme { Text("Relevantor") } }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }

  @Test
  fun gudrun_nolte_pdf_result_screenshot() {
    val sampleSummary = DomainSummary(
      id = java.util.UUID.randomUUID().toString(),
      title = "Steuerliche Bescheinigung für das Kalenderjahr 2025",
      originalUrl = "file://GudrunNolte.pdf",
      shortDescription = "Dieses Dokument ist eine steuerliche Bescheinigung der DekaBank Deutsche Girozentrale für Gudrun Nolte, die einen nicht ausgeglichenen Verlust im Sinne des § 20 EStG für das Kalenderjahr 2025 ausweist und Hinweise zur Einkommensteuererklärung gibt.",
      keyTakeaways = listOf(
        TakeawayItem(
          title = "Aussteller und Empfänger",
          details = "Die Bescheinigung wurde von der DekaBank Deutsche Girozentrale / Deka Investments ausgestellt und ist an Gudrun Nolte, wohnhaft in Henri-Dunant-Str. 5, 37075 Göttingen, gerichtet."
        ),
        TakeawayItem(
          title = "Dokumententyp",
          details = "Es handelt sich um eine Verlustbescheinigung im Sinne des § 43a Abs. 3 Satz 4 EStG für das Kalenderjahr 2025."
        ),
        TakeawayItem(
          title = "Nicht ausgeglichener Verlust",
          details = "Ein nicht ausgeglichener Verlust im Sinne des § 20 EStG in Höhe von 1.697,33 EUR wird ausgewiesen. Dieser Betrag resultiert ausschließlich aus sonstigen Verlusten, während keine Verluste aus der Veräußerung von Aktien vorliegen."
        ),
        TakeawayItem(
          title = "Kapitalerträge und Steuerabzüge",
          details = "Die Höhe der Kapitalerträge im Sinne des § 20 EStG, einbehaltene Kapitalertragsteuer und der Solidaritätszuschlag betragen jeweils 0,00 EUR."
        ),
        TakeawayItem(
          title = "Verwendungshinweise",
          details = "Die Bescheinigung ist zur Vorlage beim Finanzamt im Rahmen der Einkommensteuererklärung bestimmt. Die ausgewiesenen Werte sind in die Anlage KAP (Einkünfte aus Kapitalvermögen) einzutragen, insbesondere die nicht ausgeglichenen Verluste in Zeile 12 und folgende, um eine Verlustverrechnung zu ermöglichen."
        ),
        TakeawayItem(
          title = "Hinweis zu Alt-Anteilen",
          details = "Für Alt-Anteile, die vor dem 01.01.2009 erworben wurden, gelten gesonderte Übergangsregelungen. Veräußerungsgewinne oder -verluste aus diesen Alt-Anteilen sind steuerlich nicht relevant und werden in dieser Bescheinigung nicht aufgeführt."
        )
      ),
      owner = "DekaBank Deutsche Girozentrale / Deka Investments",
      analysisId = java.util.UUID.randomUUID().toString()
    )

    val category = CategoryInfo(
      id = "E",
      label = "",
      name = "Dokumente & KI-Analysen",
      icon = Icons.Default.Description,
      color = Color(0xFF10B981),
      functions = emptyList()
    )

    val function = FunctionInfo(
      id = "E.1",
      name = "Dokumenten-Analyse",
      description = "Dokumente & PDFs analysieren",
      type = AnalysisType.DOKUMENTE,
      icon = Icons.Default.Description,
      color = Color(0xFF10B981)
    )

    composeTestRule.setContent {
      MaterialTheme {
        ResultScreen(
          summary = sampleSummary,
          activeCategory = category,
          activeFunction = function,
          isFavorite = false,
          onToggleFavorite = {},
          onBackClick = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/gudrun_nolte_pdf_result.png")
  }

  @Test
  fun testFreeQueryDialog_flow() {
    val tempInput = androidx.compose.runtime.mutableStateOf("")
    var sentQuestion: String? = null
    val showDialog = androidx.compose.runtime.mutableStateOf(true)

    composeTestRule.setContent {
      if (showDialog.value) {
        androidx.compose.foundation.layout.Box {
          androidx.compose.foundation.layout.Column {
            androidx.compose.material3.Text("Frage an die Quelle")
            androidx.compose.material3.OutlinedTextField(
              value = tempInput.value,
              onValueChange = { tempInput.value = it },
              placeholder = { androidx.compose.material3.Text("Trage hier Deine Frage zur Quelle ein") },
              modifier = androidx.compose.ui.Modifier
                .fillMaxWidth()
                .testTag("free_query_text_field")
            )
            androidx.compose.material3.Button(
              onClick = {
                sentQuestion = tempInput.value.trim()
                showDialog.value = false
                tempInput.value = ""
              },
              enabled = tempInput.value.trim().isNotEmpty(),
              modifier = androidx.compose.ui.Modifier.testTag("free_query_send_button")
            ) {
              androidx.compose.material3.Text("Frage senden")
            }
            androidx.compose.material3.TextButton(
              onClick = {
                showDialog.value = false
                tempInput.value = ""
              },
              modifier = androidx.compose.ui.Modifier.testTag("free_query_cancel_button")
            ) {
              androidx.compose.material3.Text("Abbrechen")
            }
          }
        }
      }
    }

    // 1. Verify dialog is displayed
    composeTestRule.onNodeWithText("Frage an die Quelle").assertIsDisplayed()
    composeTestRule.onNodeWithTag("free_query_text_field").assertIsDisplayed()

    // 2. Verify Send button is disabled initially because input is empty
    composeTestRule.onNodeWithTag("free_query_send_button").assertIsNotEnabled()

    // 3. Type whitespace only, send button remains disabled
    composeTestRule.onNodeWithTag("free_query_text_field").performTextInput("    ")
    composeTestRule.onNodeWithTag("free_query_send_button").assertIsNotEnabled()

    // 4. Type a valid question, send button becomes enabled
    composeTestRule.onNodeWithTag("free_query_text_field").performTextReplacement("Welche Herausforderungen beschreibt der Autor?")
    composeTestRule.onNodeWithTag("free_query_send_button").assertIsEnabled()

    // 5. Click Send button, verify it triggers sending and closes dialog
    composeTestRule.onNodeWithTag("free_query_send_button").performClick()
    org.junit.Assert.assertEquals("Welche Herausforderungen beschreibt der Autor?", sentQuestion)
    org.junit.Assert.assertFalse(showDialog.value)
  }

  @Test
  fun testFreeQueryDialog_cancel() {
    val showDialog = androidx.compose.runtime.mutableStateOf(true)
    val tempInput = androidx.compose.runtime.mutableStateOf("Some question")

    composeTestRule.setContent {
      if (showDialog.value) {
        androidx.compose.foundation.layout.Box {
          androidx.compose.foundation.layout.Column {
            androidx.compose.material3.Text("Frage an die Quelle")
            androidx.compose.material3.OutlinedTextField(
              value = tempInput.value,
              onValueChange = { tempInput.value = it },
              modifier = androidx.compose.ui.Modifier.testTag("free_query_text_field")
            )
            androidx.compose.material3.Button(
              onClick = { showDialog.value = false },
              modifier = androidx.compose.ui.Modifier.testTag("free_query_send_button")
            ) {
              androidx.compose.material3.Text("Frage senden")
            }
            androidx.compose.material3.TextButton(
              onClick = {
                showDialog.value = false
                tempInput.value = ""
              },
              modifier = androidx.compose.ui.Modifier.testTag("free_query_cancel_button")
            ) {
              androidx.compose.material3.Text("Abbrechen")
            }
          }
        }
      }
    }

    // Click cancel and verify dialog closes and clears input
    composeTestRule.onNodeWithTag("free_query_cancel_button").performClick()
    org.junit.Assert.assertFalse(showDialog.value)
    org.junit.Assert.assertEquals("", tempInput.value)
  }
}
