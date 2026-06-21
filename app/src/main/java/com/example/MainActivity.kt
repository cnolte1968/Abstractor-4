package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.DomainSummary
import com.example.domain.model.TakeawayItem
import com.example.ui.MainViewModel
import com.example.ui.UiState
import com.example.ui.LoadingStep
import com.example.ui.AuthStatus
import androidx.compose.ui.draw.alpha
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private val isSharedLaunchState = mutableStateOf(false)

    private fun getSharedTextFromIntent(intent: Intent?): String {
        if (intent == null) return ""
        var sharedText = intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""
        if (sharedText.isBlank()) {
            val clipData = intent.clipData
            if (clipData != null && clipData.itemCount > 0) {
                val item = clipData.getItemAt(0)
                sharedText = item.text?.toString() ?: ""
            }
        }
        return sharedText
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize UseCases and repositories in ViewModel
        viewModel.initIfNeeded(applicationContext)

        // Initialize PromptLoader with application context
        com.example.data.PromptLoader.init(applicationContext)

        // Schedule periodic background sync with WorkManager
        com.example.data.sync.SyncScheduler.schedulePeriodicSync(applicationContext)

        // Build & Model diagnostics logging at startup
        android.util.Log.d("AbstractorDiagnostic", "=== DIAGNOSTIC STARTUP ===")
        android.util.Log.d("AbstractorDiagnostic", "BUILD_DIAGNOSTIC_VER = ${com.example.data.GeminiModelConfig.ABSTRACTOR_BUILD_DIAGNOSTIC}")
        android.util.Log.d("AbstractorDiagnostic", "BuildConfig.VERSION_CODE = ${com.example.BuildConfig.VERSION_CODE}")
        android.util.Log.d("AbstractorDiagnostic", "BuildConfig.VERSION_NAME = ${com.example.BuildConfig.VERSION_NAME}")
        android.util.Log.d("AbstractorDiagnostic", "Primary configured model = ${com.example.data.GeminiModelConfig.TEXT_MODEL}")
        android.util.Log.d("AbstractorDiagnostic", "Fallback configured model = ${com.example.data.GeminiModelConfig.FALLBACK_MODEL}")
        android.util.Log.d("AbstractorDiagnostic", "==========================")

        // Intercept Android System Action Send Intents
        if (intent?.action == Intent.ACTION_SEND && "text/plain" == intent.type) {
            val sharedText = getSharedTextFromIntent(intent)
            if (sharedText.isNotBlank()) {
                isSharedLaunchState.value = true
                viewModel.processSharedText(sharedText, intent)
            }
        }

        setContent {
            MyApplicationTheme {
                val isSharedLaunch by isSharedLaunchState
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = if (isSharedLaunch) Color.Transparent else MaterialTheme.colorScheme.background
                ) {
                    AbstractorAppScreen(
                        viewModel = viewModel,
                        isSharedLaunch = isSharedLaunch,
                        onDismiss = { finish() },
                        onResetSharedLaunchState = { isSharedLaunchState.value = false }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.action == Intent.ACTION_SEND && "text/plain" == intent.type) {
            val sharedText = getSharedTextFromIntent(intent)
            if (sharedText.isNotBlank()) {
                isSharedLaunchState.value = true
                viewModel.processSharedText(sharedText, intent)
            }
        }
    }
}

@Composable
fun AbstractorAppScreen(
    viewModel: MainViewModel,
    isSharedLaunch: Boolean,
    onDismiss: () -> Unit,
    onResetSharedLaunchState: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentUrl by viewModel.currentUrl.collectAsState()
    val currentTitle by viewModel.currentTitle.collectAsState()
    val sharedUrlToFill by viewModel.sharedUrlToFill.collectAsState()
    val selectedAnalysisType by viewModel.currentAnalysisType.collectAsState()

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var manualUrlInput by remember { mutableStateOf("") }
    var showInfoDialog by remember { mutableStateOf(false) }
    var showFileOptionsDialog by remember { mutableStateOf(false) }
    var showHistorySyncDialog by remember { mutableStateOf(false) }

    // Synchronize shared launcher input immediately
    LaunchedEffect(sharedUrlToFill) {
        if (sharedUrlToFill.isNotBlank()) {
            manualUrlInput = sharedUrlToFill
            viewModel.clearSharedUrlToFill()
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.summarizeFileUri(context, it) }
    }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.summarizeFileUri(context, it) }
    }

    // Outer container layout
    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (isSharedLaunch) {
                    Modifier.background(Color.Black.copy(alpha = 0.45f))
                } else {
                    Modifier.background(MaterialTheme.colorScheme.background)
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(if (isSharedLaunch) 0.94f else 1.0f)
                .padding(
                    horizontal = if (isSharedLaunch) 16.dp else 20.dp,
                    vertical = if (isSharedLaunch) 16.dp else 24.dp
                )
                .then(
                    if (isSharedLaunch) {
                        Modifier
                            .fillMaxHeight(0.92f)
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.background)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(24.dp)
                            )
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {}
                    } else {
                        Modifier
                            .fillMaxHeight()
                            .navigationBarsPadding()
                            .statusBarsPadding()
                            .verticalScroll(rememberScrollState())
                    }
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // State-driven UI routing
            when (val state = uiState) {
                is UiState.Idle -> {
                    // 1. STARTSCREEN (Das Funktionscockpit)
                    
                    // Header / Toolbar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { showInfoDialog = true },
                            modifier = Modifier.testTag("menu_info_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menü",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = "✨", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Abstractor",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.5).sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End
                        ) {
                            IconButton(
                                onClick = { showHistorySyncDialog = true },
                                modifier = Modifier.testTag("history_sync_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = if (com.example.data.BackendFeatureConfig.cloudSyncEnabled) "Verlauf & Sync" else "Lokaler Verlauf",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            Surface(
                                color = Color(0xFFFEF3C7),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "👑", fontSize = 12.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Pro",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFD97706)
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Intro Callout Text
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = "Was möchtest du analysieren?",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Füge eine URL ein oder verwende die Zwischenablage.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.secondary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Input Card
                    FloatingUrlInputCard(
                        manualUrlInput = manualUrlInput,
                        onUrlChange = { manualUrlInput = it },
                        onTriggerSummary = {
                            val urlToProcess = manualUrlInput.trim()
                            if (urlToProcess.isNotBlank()) {
                                viewModel.fetchSummary(urlToProcess, directContent = viewModel.cachedDirectContent, analysisType = selectedAnalysisType)
                            } else {
                                Toast.makeText(context, "Bitte gib zuerst eine gültige Webadresse ein.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Cockpit Mode Selection
                    CockpitLayout(
                        selectedType = selectedAnalysisType,
                        onOptionSelected = { type ->
                            viewModel.setAnalysisType(type)
                            val urlToProcess = manualUrlInput.trim()
                            if (urlToProcess.isNotBlank()) {
                                viewModel.fetchSummary(urlToProcess, directContent = viewModel.cachedDirectContent, analysisType = type)
                            } else {
                                Toast.makeText(context, "Bitte gib zuerst eine gültige Webadresse ein.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Mit Dateien & Fotos arbeiten Bar Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { showFileOptionsDialog = true },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FolderOpen,
                                        contentDescription = "Datei Ordner",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Mit Dateien & Fotos arbeiten",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowRight,
                                contentDescription = "Pfeil rechts",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                is UiState.Loading -> {
                    // 2. LOADING- / FORTSCHRITTSSCREEN
                    
                    // Toolbar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.resetToIdle() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Abbrechen",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = getTitleForAnalysisType(selectedAnalysisType),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = getDomainName(currentUrl),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                ),
                                color = MaterialTheme.colorScheme.secondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(onClick = { showInfoDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Details",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Large Circular Progress Ring Illustration
                    Box(
                        modifier = Modifier
                            .size(150.dp)
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.fillMaxSize(),
                            strokeWidth = 4.5.dp,
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        )
                        Box(
                            modifier = Modifier
                                .size(105.dp)
                                .background(Color.White, CircleShape)
                                .border(1.dp, Color(0xFFF1F5F9), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(text = "📄", fontSize = 34.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(text = "🔍", fontSize = 16.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Analysiere Inhalte...",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Gemini KI verarbeitet die Webseite und bereitet die Ergebnisse vor.",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Timeline Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "AKTIVITÄTS-FORTSCHRITT",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.3.sp
                                ),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
                            )

                            // Step 1: Webpage laden
                            val isStep1Active = state.step == LoadingStep.FETCHING_DATA
                            val isStep1Completed = state.step == LoadingStep.ANALYZING_INPUT || state.step == LoadingStep.GENERATING_OUTPUT
                            LoadingStepTimelineItem(
                                stepNumber = "1",
                                title = "Webseite laden",
                                description = "Inhalte & Strukturen abrufen",
                                isActive = isStep1Active,
                                isCompleted = isStep1Completed,
                                isLast = false
                            )

                            // Step 2: Inhalte analysieren
                            val isStep2Active = state.step == LoadingStep.ANALYZING_INPUT
                            val isStep2Completed = state.step == LoadingStep.GENERATING_OUTPUT
                            LoadingStepTimelineItem(
                                stepNumber = "2",
                                title = "Inhalte analysieren",
                                description = "Themen, Kernaussagen & Relevanz erkennen",
                                isActive = isStep2Active,
                                isCompleted = isStep2Completed,
                                isLast = false
                            )

                            // Step 3: Ergebnisse vorbereiten
                            val isStep3Active = state.step == LoadingStep.GENERATING_OUTPUT
                            val isStep3Completed = false
                            LoadingStepTimelineItem(
                                stepNumber = "3",
                                title = "Ergebnisse vorbereiten",
                                description = "Die wichtigsten Erkenntnisse verdichten",
                                isActive = isStep3Active,
                                isCompleted = isStep3Completed,
                                isLast = false
                            )

                            // Step 4: Ausgabe erstellen
                            LoadingStepTimelineItem(
                                stepNumber = "4",
                                title = "Ausgabe erstellen",
                                description = "Fertigstellung in Kürze",
                                isActive = false,
                                isCompleted = false,
                                isLast = true
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Supportive Tip box
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(text = "💡", fontSize = 18.sp, modifier = Modifier.padding(top = 1.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Tipp: Je besser die Quelle, desto präziser die Analyse. Wir extrahieren nur relevante Informationen.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 14.sp),
                                color = Color(0xFF1E40AF)
                            )
                        }
                    }
                }

                is UiState.Success -> {
                    // 3. ERGEBNIS-SCREEN / EXECUTIVE BRIEFING
                    
                    // Toolbar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.resetToIdle() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Zurück",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = getTitleForAnalysisType(state.analysisType),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = getDomainName(state.summary.originalUrl.ifBlank { currentUrl }),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                ),
                                color = MaterialTheme.colorScheme.secondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(onClick = {
                            val plainText = buildPlainTextShareOrCopyText(state.summary, state.analysisType, currentUrl)
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, plainText)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Teilen via"))
                        }) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Teilen",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Card 1: Metadata / Source
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        val cleanDisplayLink = state.summary.originalUrl.ifBlank { currentUrl }
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "🌐", fontSize = 20.sp)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = state.summary.title.ifBlank { currentTitle.ifBlank { "Analysierter Inhalt" } },
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            letterSpacing = (-0.1).sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    val owner = state.summary.owner ?: ""
                                    Text(
                                        text = if (owner.isNotBlank()) "von $owner" else cleanDisplayLink,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 11.sp
                                        ),
                                        color = MaterialTheme.colorScheme.secondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(10.dp))

                            // Source Link
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Language,
                                        contentDescription = "Link",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = cleanDisplayLink.replace("https://", "").replace("http://", ""),
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.primary
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.clickable {
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(cleanDisplayLink))
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Link konnte nicht geöffnet werden.", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    )
                                }

                                Row {
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(cleanDisplayLink))
                                            Toast.makeText(context, "Link in die Zwischenablage kopiert!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Kopieren",
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    IconButton(
                                        onClick = {
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(cleanDisplayLink))
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Link konnte nicht geöffnet werden.", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Launch,
                                            contentDescription = "Öffnen",
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Card 2: "GANZ KURZ" Summary Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "GANZ KURZ",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.2.sp
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(text = "✨", fontSize = 16.sp)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = state.summary.shortDescription,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    lineHeight = 20.sp,
                                    fontSize = 13.sp
                                ),
                                color = Color.Black
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Heading: KERNAUSSAGEN
                    Text(
                        text = "WICHTIGSTE KERNAUSSAGEN",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.3.sp
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(bottom = 10.dp, start = 4.dp)
                    )

                    // Insight Cards List
                    val takeaways = state.summary.keyTakeaways
                    val isNumbered = state.analysisType == com.example.data.AnalysisType.TOP_3_KERNAUSSAGEN ||
                            state.analysisType == com.example.data.AnalysisType.RISIKO_ANALYSE

                    takeaways.forEachIndexed { index, takeaway ->
                        val numBadge = "%02d".format(index + 1)
                        val config = getTakeawayStyleConfig(index)

                        Card(
                            modifier = Modifier
                                  .fillMaxWidth()
                                  .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                // Large purple badge index OR beautiful Icon Bullet
                                if (isNumbered) {
                                    Text(
                                        text = numBadge,
                                        style = TextStyle(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        ),
                                        modifier = Modifier.padding(top = 1.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = getIconForAnalysisType(state.analysisType),
                                        contentDescription = "Inhaltspunkt",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .padding(top = 2.dp)
                                            .size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                // Content text
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = takeaway.title,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = takeaway.details,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = 12.sp,
                                            lineHeight = 16.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                // Beautiful styled right alignment icon
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(config.bgColor, RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = config.icon,
                                        contentDescription = "Teamelement",
                                        tint = config.tintColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Fixed Action Footer Container
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val textToCopy = buildPlainTextShareOrCopyText(state.summary, state.analysisType, currentUrl)
                                clipboardManager.setText(AnnotatedString(textToCopy))
                                Toast.makeText(context, "Zusammenfassung wurde kopiert!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Kopieren", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                val plainText = buildPlainTextShareOrCopyText(state.summary, state.analysisType, currentUrl)
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, plainText)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Teilen via"))
                            },
                            modifier = Modifier
                                .weight(0.6f)
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Teilen",
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Button(
                            onClick = {
                                viewModel.resetToIdle()
                            },
                            modifier = Modifier
                                .weight(1.4f)
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Neustart",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Neue Analyse", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                is UiState.Error -> {
                    // ERROR STATE
                    
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { viewModel.resetToIdle() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Zurück",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "Fehler bei Analyse",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(20.dp)
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Fehler",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(52.dp)
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Center
                                )

                                if (state.detail != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = state.detail,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                                        color = MaterialTheme.colorScheme.secondary,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        if (state.isPaywallOrBlocked) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = "🔓", fontSize = 18.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Lösung für geschützte Seiten / Abos",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Manche geschützten Artikel verlangen einen Login. Du kannst das einfach umgehen:\n\n" +
                                                "1. Markiere den Text direkt in deinem Browser oder App.\n" +
                                                "2. Kopiere den markierten Text.\n" +
                                                "3. Kehre hierher zurück und klicke auf den Button unten, um den kopierten Inhalt sofort per KI zu analysieren.",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                        color = MaterialTheme.colorScheme.secondary,
                                        lineHeight = 16.sp
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))

                                    Button(
                                        onClick = {
                                            try {
                                                val clip = clipboardManager.getText()
                                                if (clip != null && clip.text.isNotBlank()) {
                                                    viewModel.processSharedText(clip.text)
                                                } else {
                                                    Toast.makeText(context, "Deine Zwischenablage ist aktuell leer!", Toast.LENGTH_LONG).show()
                                                }
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Zugriff auf Zwischenablage fehlgeschlagen.", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(44.dp)
                                            .testTag("error_clipboard_fallback_button"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Text("📋 Text aus Zwischenablage analysieren", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (isSharedLaunch) {
                            Button(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                )
                            ) {
                                Text("Schließen", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = {
                                    onResetSharedLaunchState()
                                    viewModel.resetToIdle()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Zurück zur Startseite", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // Info overlay modal dialog
    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = {
                Text(
                    text = "Über Abstractor ✨",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Abstractor ist dein intelligenter Begleiter, um lange Webseiten, Dokumente, Fotos und YouTube-Videos blitzschnell auszuwerten.",
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Features:\n" +
                                "• Hochpräzises Scopen von Inhalten.\n" +
                                "• YouTube Unbannable oEmbed-Fallback.\n" +
                                "• Schnelle Zwischenablage-Analyse.\n" +
                                "• Robustes, intelligentes Parsing.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        lineHeight = 16.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text("Schließen", fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color.White
        )
    }

    // Modal dialog to choose Document or Photo analyze
    if (showFileOptionsDialog) {
        AlertDialog(
            onDismissRequest = { showFileOptionsDialog = false },
            title = {
                Text(
                    "Mit Dateien & Fotos arbeiten",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Wähle eine Option aus, um lokale Dateien oder Bilder hochzuladen und durch Gemini analysieren zu lassen:",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B)
                    )

                    // Option 1: Dokumente
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showFileOptionsDialog = false
                                documentPickerLauncher.launch(
                                    arrayOf(
                                        "application/pdf",
                                        "text/*",
                                        "application/msword",
                                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                                    )
                                )
                            },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "📄", fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Dokument zusammenfassen", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("PDF, Word oder Text-Dateien", fontSize = 11.sp, color = Color(0xFF64748B))
                            }
                        }
                    }

                    // Option 2: Photos
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showFileOptionsDialog = false
                                imagePickerLauncher.launch(arrayOf("image/*"))
                            },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "📸", fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Fotos & Screenshots auswerten", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Texte auf Bildern & Fotos analysieren", fontSize = 11.sp, color = Color(0xFF64748B))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFileOptionsDialog = false }) {
                    Text("Abbrechen", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color.White
        )
    }

    if (showHistorySyncDialog) {
        val context = androidx.compose.ui.platform.LocalContext.current
        val savedHistories by viewModel.savedHistories.collectAsState()
        val syncPendingCount by viewModel.syncPendingCount.collectAsState()
        val syncUiState by viewModel.syncUiState.collectAsState()
        val syncErrorMessage by viewModel.syncErrorMessage.collectAsState()
        val activeUser by viewModel.activeUser.collectAsState()
        val authStatus by viewModel.authStatus.collectAsState()

        // Trigger active user and pending sync count updates when dialog opens
        LaunchedEffect(Unit) {
            viewModel.updateActiveUser()
            viewModel.updatePendingSyncCount()
        }

        AlertDialog(
            onDismissRequest = { showHistorySyncDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (com.example.data.BackendFeatureConfig.cloudSyncEnabled) "Verlauf & Sync 🔄" else "Lokaler Verlauf ⏳",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    IconButton(
                        onClick = { showHistorySyncDialog = false },
                        modifier = Modifier.size(24.dp).testTag("close_history_dialog_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Schließen",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 480.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (com.example.data.BackendFeatureConfig.cloudSyncEnabled) {
                        // Sync Status Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Synchronisationsstatus",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        val accountText = when (val status = authStatus) {
                                            is AuthStatus.Authenticated -> "Konto: ${status.username}"
                                            is AuthStatus.Guest -> "Lokaler Gastmodus"
                                            is AuthStatus.Error -> "Fehler: ${status.message}"
                                        }
                                        Text(
                                            text = accountText,
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (authStatus is AuthStatus.Error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (authStatus is AuthStatus.Authenticated) {
                                            Text(
                                                text = "Ausstehend: $syncPendingCount",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        } else {
                                            Text(
                                                text = "Keine Cloud-Synchronisation",
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                    }
                                    
                                    Button(
                                        onClick = {
                                            viewModel.triggerSync()
                                            com.example.data.sync.SyncScheduler.enqueueOneTimeSync(context.applicationContext)
                                        },
                                        enabled = syncUiState != "SYNCING" && authStatus is AuthStatus.Authenticated,
                                        modifier = Modifier.testTag("trigger_sync_button"),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary
                                        )
                                    ) {
                                        if (syncUiState == "SYNCING") {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Sync,
                                                contentDescription = "Sync",
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(text = "Sync", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                if (authStatus is AuthStatus.Guest) {
                                    Text(
                                        text = "Registrierung oder Login erforderlich – Synchronisation nicht möglich im lokalen Gastmodus.",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Normal),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                when (syncUiState) {
                                    "SYNCING" -> {
                                        Text(
                                            text = "Synchronisiere mit Backend...",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    "SUCCESS" -> {
                                        if (authStatus is AuthStatus.Authenticated) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Erfolg",
                                                    tint = Color(0xFF10B981),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Text(
                                                    text = "Synchronisation erfolgreich abgeschlossen!",
                                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                                    color = Color(0xFF10B981)
                                                )
                                            }
                                        }
                                    }
                                    "ERROR" -> {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Warning,
                                                contentDescription = "Fehler",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = syncErrorMessage ?: "Fehler bei der Synchronisation.",
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Offline Notice Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = "Lokaler Verlauf",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Lokale Offline-Speicherung",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = "Ihre generierten Analysen werden sicher und privat ausschließlich lokal auf Ihrem Gerät gespeichert. Cloud-Synchronisation ist vorbereitet, im aktuellen Release jedoch deaktiviert.",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 15.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // History Section Header
                    Text(
                        text = "Lokaler Verlauf (${savedHistories.size})",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Histories List
                    if (savedHistories.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(text = "📭", fontSize = 28.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Noch keine gespeicherten Analysen vorhanden.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(savedHistories) { summary ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.openSavedAnalysis(summary)
                                            showHistorySyncDialog = false
                                        }
                                        .testTag("history_item_${summary.id}"),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    ),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp)
                                    ) {
                                        Text(
                                            text = summary.title,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = summary.owner ?: summary.originalUrl,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace
                                            ),
                                            color = MaterialTheme.colorScheme.secondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = summary.shortDescription,
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 14.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showHistorySyncDialog = false },
                    modifier = Modifier.testTag("dismiss_history_dialog_button")
                ) {
                    Text("Schließen", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloatingUrlInputCard(
    manualUrlInput: String,
    onUrlChange: (String) -> Unit,
    onTriggerSummary: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    var textFieldValueState by remember {
        mutableStateOf(TextFieldValue(text = manualUrlInput))
    }

    LaunchedEffect(manualUrlInput) {
        if (textFieldValueState.text != manualUrlInput) {
            textFieldValueState = TextFieldValue(
                text = manualUrlInput,
                selection = androidx.compose.ui.text.TextRange(manualUrlInput.length)
            )
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            OutlinedTextField(
                value = textFieldValueState,
                onValueChange = { newValue ->
                    textFieldValueState = newValue
                    if (newValue.text != manualUrlInput) {
                        onUrlChange(newValue.text)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("url_input_field"),
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp
                ),
                placeholder = {
                    Text(
                        "https://example.com/artikel",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = "Link",
                        tint = Color(0xFF94A3B8)
                    )
                },
                trailingIcon = {
                    if (manualUrlInput.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                onUrlChange("")
                                textFieldValueState = TextFieldValue("")
                            },
                            modifier = Modifier.testTag("clear_url_input_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Eingabe löschen",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color(0xFFE2E8F0),
                    focusedContainerColor = Color(0xFFF8FAFC),
                    unfocusedContainerColor = Color(0xFFF8FAFC),
                    cursorColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Search,
                    keyboardType = KeyboardType.Uri
                ),
                keyboardActions = KeyboardActions(
                    onSearch = { onTriggerSummary() }
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = {
                    try {
                        val clip = clipboardManager.getText()
                        if (clip != null && clip.text.isNotBlank()) {
                            onUrlChange(clip.text.trim())
                            textFieldValueState = TextFieldValue(
                                text = clip.text.trim(),
                                selection = androidx.compose.ui.text.TextRange(clip.text.trim().length)
                            )
                            Toast.makeText(context, "URL aus Zwischenablage eingefügt!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Deine Zwischenablage ist leer oder enthält keinen Text.", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Zugriff auf Zwischenablage fehlgeschlagen.", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("analyze_clipboard_button"),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(text = "📋", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Aus Zwischenablage einfügen", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

data class AnalysisOption(
    val type: com.example.data.AnalysisType,
    val title: String,
    val description: String,
    val icon: String,
    val color: Color
)

@Composable
fun CockpitLayout(
    selectedType: com.example.data.AnalysisType,
    onOptionSelected: (com.example.data.AnalysisType) -> Unit
) {
    val options = remember {
        listOf(
            AnalysisOption(
                type = com.example.data.AnalysisType.STANDARD_WEBSEITE,
                title = "Webseite zusammenfassen",
                description = "Präzise Inhalts-Zusammenfassung",
                icon = "📝",
                color = Color(0xFF6366F1)
            ),
            AnalysisOption(
                type = com.example.data.AnalysisType.TOP_3_KERNAUSSAGEN,
                title = "3 Kernpunkte",
                description = "Die 3 wichtigsten Kernaussagen & Themen",
                icon = "📊",
                color = Color(0xFFF59E0B)
            ),
            AnalysisOption(
                type = com.example.data.AnalysisType.FACTS_VS_OPINIONS_ANALYZER,
                title = "Fakten & Meinungen",
                description = "Aussagen klassifizieren und einordnen",
                icon = "⚖️",
                color = Color(0xFF0EA5E9)
            ),
            AnalysisOption(
                type = com.example.data.AnalysisType.AKTUALITAETS_CHECK,
                title = "Aktualität prüfen",
                description = "Zeitliche Relevanz & Gültigkeit checken",
                icon = "📅",
                color = Color(0xFF10B981)
            ),
            AnalysisOption(
                type = com.example.data.AnalysisType.FEHLINFORMATIONS_RADAR,
                title = "Fehlinformationen erkennen",
                description = "Fakes, Übertreibungen & logische Fehler",
                icon = "🔍",
                color = Color(0xFFEC4899)
            ),
            AnalysisOption(
                type = com.example.data.AnalysisType.RISIKO_ANALYSE,
                title = "Risiken identifizieren",
                description = "Systemische Gefahren & Nachteile aufdecken",
                icon = "⚠️",
                color = Color(0xFFEF4444)
            ),
            AnalysisOption(
                type = com.example.data.AnalysisType.BUSINESS_INKUBATOR,
                title = "Geschäftsideen finden",
                description = "Innovative, profitable Nischenkonzepte",
                icon = "💡",
                color = Color(0xFF8B5CF6)
            ),
            AnalysisOption(
                type = com.example.data.AnalysisType.PERSPECTIVES_AND_COUNTERPOSITIONS,
                title = "Gegenpositionen finden",
                description = "Alternative Sichtweisen & Gegenargumente",
                icon = "🔄",
                color = Color(0xFF14B8A6)
            ),
            AnalysisOption(
                type = com.example.data.AnalysisType.MULTIMEDIA,
                title = "Video zusammenfassen",
                description = "Videos und Podcasts analysieren",
                icon = "📹",
                color = Color(0xFFEF4444)
            )
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Analysemodus wählen",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            ),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
        )

        val chunked = options.chunked(2)
        chunked.forEach { rowOptions ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowOptions.forEach { option ->
                    val isSelected = option.type == selectedType
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(84.dp)
                            .testTag("cockpit_button_${option.type.name.lowercase()}")
                            .clickable { onOptionSelected(option.type) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFFEEF2FF) else MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        color = if (isSelected) Color.White else option.color.copy(alpha = 0.08f),
                                        shape = RoundedCornerShape(10.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = option.icon, fontSize = 18.sp)
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = option.title,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        lineHeight = 14.sp
                                    ),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = option.description,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 10.sp,
                                        lineHeight = 11.sp
                                    ),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f) else Color(0xFF94A3B8),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                if (rowOptions.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun LoadingStepTimelineItem(
    stepNumber: String,
    title: String,
    description: String,
    isActive: Boolean,
    isCompleted: Boolean,
    isLast: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(36.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(
                        color = when {
                            isCompleted -> MaterialTheme.colorScheme.primary
                            isActive -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            else -> Color(0xFFF1F5F9)
                        },
                        shape = CircleShape
                    )
                    .then(
                        if (isActive && !isCompleted) {
                            Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isCompleted -> {
                        Text(
                            text = "✔",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    isActive -> {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                        )
                    }
                    else -> {
                        Text(
                            text = stepNumber,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }

            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(32.dp)
                        .background(
                            color = if (isCompleted) MaterialTheme.colorScheme.primary else Color(0xFFE2E8F0)
                        )
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.padding(top = 2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                ),
                color = if (isActive || isCompleted) MaterialTheme.colorScheme.onSurface else Color(0xFF94A3B8)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp
                ),
                color = if (isActive || isCompleted) Color(0xFF64748B) else Color(0xFFCBD5E1)
            )
        }
    }
}

data class TakeawayStyleConfig(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val bgColor: Color,
    val tintColor: Color
)

fun getTakeawayStyleConfig(index: Int): TakeawayStyleConfig {
    return when (index) {
        0 -> TakeawayStyleConfig(Icons.Default.TrackChanges, Color(0xFFEFF2FF), Color(0xFF536EF1))
        1 -> TakeawayStyleConfig(Icons.Default.BusinessCenter, Color(0xFFFEF3C7), Color(0xFFD97706))
        2 -> TakeawayStyleConfig(Icons.Default.Person, Color(0xFFCCFBF1), Color(0xFF0D9488))
        3 -> TakeawayStyleConfig(Icons.Default.Business, Color(0xFFFCE7F3), Color(0xFFDB2777))
        4 -> TakeawayStyleConfig(Icons.Default.Verified, Color(0xFFD1FAE5), Color(0xFF059669))
        else -> TakeawayStyleConfig(Icons.Default.Lightbulb, Color(0xFFF3E8FF), Color(0xFF7C3AED))
    }
}

fun getIconForAnalysisType(analysisType: com.example.data.AnalysisType): androidx.compose.ui.graphics.vector.ImageVector {
    return when (analysisType) {
        com.example.data.AnalysisType.RISIKO_ANALYSE -> Icons.Default.Warning
        com.example.data.AnalysisType.BUSINESS_INKUBATOR -> Icons.Default.Lightbulb
        com.example.data.AnalysisType.FACTS_VS_OPINIONS_ANALYZER -> Icons.Default.Verified
        com.example.data.AnalysisType.FEHLINFORMATIONS_RADAR -> Icons.Default.Warning
        com.example.data.AnalysisType.AKTUALITAETS_CHECK -> Icons.Default.Search
        else -> Icons.Default.CheckCircle
    }
}



fun getPlatformEmoji(url: String): String {
    val lower = url.lowercase()
    return when {
        lower.contains("youtube.com") || lower.contains("youtu.be") -> "📺"
        lower.contains("facebook.com") || lower.contains("fb.") -> "👥"
        lower.contains("instagram.com") -> "📸"
        lower.contains("linkedin.com") -> "💼"
        lower.contains("twitter.com") || lower.contains("x.com") -> "🐦"
        lower.contains("tiktok.com") -> "🎵"
        lower.contains("content://") || lower.endsWith(".docx") || lower.endsWith(".pdf") || lower.endsWith(".xlsx") || lower.endsWith(".pptx") || lower.endsWith(".txt") -> "📄"
        else -> "🔗"
    }
}

fun parseMarkdownToAnnotatedString(text: String): AnnotatedString {
    val builder = AnnotatedString.Builder()
    val parts = text.split("**")
    var isBold = false
    for (part in parts) {
        if (isBold) {
            builder.pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold))
            builder.append(part)
            builder.pop()
        } else {
            builder.append(part)
        }
        isBold = !isBold
    }
    return builder.toAnnotatedString()
}

fun getTitleForAnalysisType(type: com.example.data.AnalysisType): String {
    return when (type) {
        com.example.data.AnalysisType.STANDARD_WEBSEITE -> "Standard-Webseite zusammenfassen"
        com.example.data.AnalysisType.MULTIMEDIA -> "Multimedia-Inhalt zusammenfassen"
        com.example.data.AnalysisType.TOP_3_KERNAUSSAGEN -> "Die Top 3 Kernaussagen ermitteln"
        com.example.data.AnalysisType.AKTUALITAETS_CHECK -> "Aktualität prüfen"
        com.example.data.AnalysisType.FEHLINFORMATIONS_RADAR -> "Fehlinformations-Radar aktivieren"
        com.example.data.AnalysisType.RISIKO_ANALYSE -> "Risiko-Analyse durchführen"
        com.example.data.AnalysisType.BUSINESS_INKUBATOR -> "Geschäftsideen-Inkubator starten"
        com.example.data.AnalysisType.DOKUMENTE -> "Dokumente zusammenfassen"
        com.example.data.AnalysisType.FACTS_VS_OPINIONS_ANALYZER -> "Fakt oder Meinung!?"
        com.example.data.AnalysisType.PERSPECTIVES_AND_COUNTERPOSITIONS -> "Perspektiven & Gegenpositionen finden"
    }
}

fun getDomainName(url: String): String {
    if (url.isBlank()) return "Webadresse"
    return try {
        val uri = Uri.parse(url)
        val host = uri.host ?: url
        host.replace("www.", "")
    } catch (e: Exception) {
        url.replace("https://", "").replace("http://", "").split("/").firstOrNull() ?: url
    }
}

fun buildPlainTextShareOrCopyText(
    summary: DomainSummary,
    analysisType: com.example.data.AnalysisType,
    fallbackUrl: String
): String {
    fun cleanMarkdown(text: String): String {
        var temp = text
            .replace(Regex("\\*\\*(.*?)\\*\\*"), "$1")
            .replace(Regex("\\*(.*?)\\*"), "$1")
            .replace(Regex("`([^`]+)`"), "$1")

        temp = temp.replace(Regex("^\\s*[-*+•]\\s+"), "")
        return temp.trim()
    }

    val url = summary.originalUrl.ifBlank { fallbackUrl }
    val title = summary.title.ifBlank { "Analysierter Inhalt" }
    val ownerValue = summary.owner ?: ""
    val ownerDisplay = if (ownerValue.isNotBlank()) ownerValue else getDomainName(url)

    return buildString {
        appendLine("Titel der Quelle: $title")
        appendLine("Owner: $ownerDisplay")
        appendLine()

        appendLine("URL:")
        appendLine(url)
        appendLine()

        if (summary.shortDescription.isNotBlank()) {
            appendLine("Ganz kurz:")
            appendLine(cleanMarkdown(summary.shortDescription))
            appendLine()
        }

        if (summary.keyTakeaways.isNotEmpty()) {
            appendLine("Inhalte:")
            val isNumbered = analysisType == com.example.data.AnalysisType.TOP_3_KERNAUSSAGEN ||
                    analysisType == com.example.data.AnalysisType.RISIKO_ANALYSE

            summary.keyTakeaways.forEachIndexed { index, takeaway ->
                val cleanTitle = cleanMarkdown(takeaway.title)
                val cleanDetails = cleanMarkdown(takeaway.details)
                val formatText = if (cleanTitle.isNotBlank()) "$cleanTitle: $cleanDetails" else cleanDetails
                if (isNumbered) {
                    appendLine("${index + 1}. $formatText")
                } else {
                    appendLine("- $formatText")
                }
            }
        }
    }.trim()
}
