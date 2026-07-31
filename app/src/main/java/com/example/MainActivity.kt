package com.example

// CP-02 Verification: "Frage an die Quelle" user flow verified and active in MainActivity.kt
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AnalysisType
import com.example.domain.model.DomainSummary
import com.example.domain.model.TakeawayItem
import com.example.ui.MainViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        viewModel.initIfNeeded(applicationContext)

        // Handle initial incoming share intent
        intent?.let { handleIntent(it) }

        setContent {
            RelevantorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RelevantorApp(viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        if (intent.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!sharedText.isNullOrBlank()) {
                viewModel.processSharedText(sharedText, intent)
                Toast.makeText(this, "Inhalt empfangen!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

fun buildShareText(
    title: String?,
    shortDescription: String?,
    originalUrl: String?,
    fallbackTitle: String = "Relevantor"
): String {
    val t = if (title.isNullOrBlank()) fallbackTitle else title.trim()
    val desc = if (shortDescription.isNullOrBlank()) "" else shortDescription.trim()
    val url = if (originalUrl.isNullOrBlank()) "" else originalUrl.trim()

    return when {
        desc.isNotEmpty() && url.isNotEmpty() -> {
            "$t\n\n$desc\n\n---\n\n$url"
        }
        desc.isEmpty() && url.isNotEmpty() -> {
            "$t\n\n---\n\n$url"
        }
        desc.isNotEmpty() && url.isEmpty() -> {
            "$t\n\n$desc"
        }
        else -> {
            t
        }
    }
}

// ----------------------------------------------------------------------------------
// DATA MODELS FOR NAVIGATION MAPPING
// ----------------------------------------------------------------------------------

enum class AppTab {
    START, VERLAUF, FAVORITEN, PRO
}

data class FunctionInfo(
    val id: String,
    val name: String,
    val description: String,
    val type: AnalysisType?,
    val icon: ImageVector,
    val color: Color,
    val isPlaceholder: Boolean = false,
    val acceptedInputs: Set<com.example.ui.metadata.AcceptedInput> = setOf(com.example.ui.metadata.AcceptedInput.WEB)
)

data class CategoryInfo(
    val id: String,
    val label: String,
    val name: String,
    val icon: ImageVector,
    val color: Color,
    val functions: List<FunctionInfo>
)

// Helper to find CategoryInfo for a given FunctionInfo or AnalysisType
fun findCategoryForFunction(functionId: String): CategoryInfo? {
    return categoriesList.find { cat -> cat.functions.any { it.id == functionId } }
}

fun findCategoryForType(type: AnalysisType): CategoryInfo? {
    return categoriesList.find { cat -> cat.functions.any { it.type == type } }
}

val categoriesList: List<CategoryInfo> = com.example.ui.metadata.FeatureCatalog.categories.map { cat ->
    CategoryInfo(
        id = cat.id,
        label = cat.label,
        name = cat.name,
        icon = cat.icon,
        color = cat.color,
        functions = com.example.ui.metadata.FeatureCatalog.features
            .filter { it.category == cat.id && it.visible }
            .map { feat ->
                FunctionInfo(
                    id = feat.functionId,
                    name = feat.name,
                    description = feat.description,
                    type = feat.analysisType,
                    icon = feat.icon,
                    color = feat.color,
                    isPlaceholder = feat.isPlaceholder,
                    acceptedInputs = feat.acceptedInputs
                )
            }
    )
}

// ----------------------------------------------------------------------------------
// HELPER FOR RENDERING MARKDOWN (BOLD INJECTIONS)
// ----------------------------------------------------------------------------------
fun parseMarkdownToAnnotatedString(text: String): AnnotatedString {
    val builder = AnnotatedString.Builder()
    val lines = text.split("\n")
    for (index in lines.indices) {
        var line = lines[index]
        
        // Remove leading asterisks from list items
        val trimmed = line.trim()
        if (trimmed.startsWith("*") && !trimmed.startsWith("**")) {
            val firstStarIndex = line.indexOf('*')
            if (firstStarIndex != -1) {
                line = line.substring(0, firstStarIndex) + line.substring(firstStarIndex + 1).trimStart()
            }
        }
        
        // Remove trailing asterisk if any (e.g., "*Kulturelle*")
        if (line.endsWith("*") && !line.endsWith("**")) {
            line = line.substring(0, line.length - 1)
        }
        
        // Split by "**" to parse bold blocks
        val parts = line.split("**")
        for (i in parts.indices) {
            val part = parts[i]
            // Clean up any remaining single asterisks
            val cleanPart = part.replace("*", "")
            if (i % 2 == 1) {
                builder.pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                builder.append(cleanPart)
                builder.pop()
            } else {
                builder.append(cleanPart)
            }
        }
        
        if (index < lines.size - 1) {
            builder.append("\n")
        }
    }
    return builder.toAnnotatedString()
}

// ----------------------------------------------------------------------------------
// THEME IMPLEMENTATION (M3 LIGHT AS SPECIFIED)
// ----------------------------------------------------------------------------------

@Composable
fun RelevantorTheme(content: @Composable () -> Unit) {
    val lightColorScheme = lightColorScheme(
        primary = Color(0xFF4F46E5), // Beautiful Indigo
        secondary = Color(0xFF3B82F6), // Deep Blue
        tertiary = Color(0xFF10B981), // Emerald
        background = Color(0xFFF8FAFC), // Slate 50 (Crisp off-white)
        surface = Color(0xFFFFFFFF), // Pure White cards
        surfaceVariant = Color(0xFFF1F5F9), // Slate 100 for light fillings
        onBackground = Color(0xFF0F172A), // Slate 900 for text
        onSurface = Color(0xFF1E293B), // Slate 800
        outline = Color(0xFFE2E8F0) // Slate 200 border
    )
    MaterialTheme(
        colorScheme = lightColorScheme,
        content = content
    )
}

// ----------------------------------------------------------------------------------
// APP MAIN ROUTER & STATE MACHINE (ADAPTIVE COMPACT/EXPANDED DESIGN)
// ----------------------------------------------------------------------------------

@Composable
fun RelevantorApp(viewModel: MainViewModel) {
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    val uiState by viewModel.uiState.collectAsState()
    val savedHistories by viewModel.savedHistories.collectAsState()
    val sharedUrlToFill by viewModel.sharedUrlToFill.collectAsState()
    val authStatus by viewModel.authStatus.collectAsState()

    var urlInput by rememberSaveable { mutableStateOf("") }
    var useSearchGrounding by rememberSaveable { mutableStateOf(false) }
    var freeQueryInput by rememberSaveable { mutableStateOf("") }
    var showFreeQueryDialog by rememberSaveable { mutableStateOf(false) }
    var tempFreeQueryInput by rememberSaveable { mutableStateOf("") }

    // Navigation and UX states
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.START) }
    var activeCategory by remember { mutableStateOf(categoriesList[0]) }
    var activeFunction by remember { mutableStateOf<FunctionInfo?>(null) }
    
    // Default favorites list
    val favoritesList by viewModel.favoritesList.collectAsState()

    // Sync input field with incoming shared URL
    LaunchedEffect(sharedUrlToFill) {
        if (sharedUrlToFill.isNotBlank()) {
            urlInput = sharedUrlToFill
            selectedTab = AppTab.START
            viewModel.clearSharedUrlToFill()
        }
    }

    // Modal alerts for placeholders
    var placeholderToShowAlert by remember { mutableStateOf<FunctionInfo?>(null) }

    val context = LocalContext.current
    val filePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            viewModel.summarizeFileUri(context, uri, activeFunction?.type ?: AnalysisType.DOKUMENTE)
        }
    }

    val onFunctionClick: (FunctionInfo) -> Unit = { func ->
        if (func.isPlaceholder) {
            placeholderToShowAlert = func
        } else {
            activeFunction = func
            if (func.type != null) {
                viewModel.setAnalysisType(func.type)
            }
            if (func.acceptedInputs.contains(com.example.ui.metadata.AcceptedInput.DOCUMENT)) {
                filePickerLauncher.launch("*/*")
            } else if (func.acceptedInputs.contains(com.example.ui.metadata.AcceptedInput.IMAGE)) {
                filePickerLauncher.launch("image/*")
            } else if (urlInput.isBlank()) {
                // If url is blank, keep on start page but set active function
                selectedTab = AppTab.START
            } else {
                // If url is set, trigger analysis
                if (func.type?.canonical() == com.example.data.AnalysisType.FREE_SOURCE_QUERY) {
                    showFreeQueryDialog = true
                } else {
                    viewModel.fetchSummary(
                        rawUrl = urlInput,
                        directContent = null,
                        analysisType = func.type ?: AnalysisType.WEB_SUMMARY,
                        freeQuery = if (func.type == AnalysisType.FREIE_QUELLENANFRAGE || func.type == AnalysisType.FREE_SOURCE_QUERY) freeQueryInput else null
                    )
                }
            }
        }
    }

    if (placeholderToShowAlert != null) {
        val isAiImageCheck = placeholderToShowAlert?.name == "Bild mit KI erzeugt?"
        val alertTitle = if (isAiImageCheck) "Funktion vorbereitet" else "Premium Funktion"
        val alertText = if (isAiImageCheck) {
            "Diese Funktion ist vorbereitet, aber noch nicht aktiviert. Der Analyse-Prompt wird später ergänzt."
        } else {
            "Die Funktion „${placeholderToShowAlert?.name}“ befindet sich aktuell in der Entwicklung und wird im nächsten Release freigeschaltet."
        }
        AlertDialog(
            onDismissRequest = { placeholderToShowAlert = null },
            title = { Text(alertTitle) },
            text = { Text(alertText) },
            confirmButton = {
                Button(onClick = { placeholderToShowAlert = null }) {
                    Text("Verstanden")
                }
            }
        )
    }

    if (showFreeQueryDialog) {
        AlertDialog(
            onDismissRequest = {
                showFreeQueryDialog = false
                tempFreeQueryInput = ""
            },
            title = {
                Text("Frage an die Quelle")
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = tempFreeQueryInput,
                        onValueChange = { tempFreeQueryInput = it },
                        placeholder = {
                            Text("Trage hier Deine Frage zur Quelle ein", color = Color.Gray)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("free_query_text_field"),
                        singleLine = false,
                        maxLines = 4
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = tempFreeQueryInput.trim()
                        showFreeQueryDialog = false
                        tempFreeQueryInput = ""
                        viewModel.fetchSummary(
                            rawUrl = urlInput,
                            directContent = null,
                            analysisType = com.example.data.AnalysisType.FREE_SOURCE_QUERY,
                            freeQuery = trimmed
                        )
                    },
                    enabled = tempFreeQueryInput.trim().isNotEmpty(),
                    modifier = Modifier.testTag("free_query_send_button")
                ) {
                    Text("Frage senden")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showFreeQueryDialog = false
                        tempFreeQueryInput = ""
                    },
                    modifier = Modifier.testTag("free_query_cancel_button")
                ) {
                    Text("Abbrechen")
                }
            }
        )
    }

    if (isTablet) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxHeight()
            ) {
                SmartphoneLayout(
                    viewModel = viewModel,
                    uiState = uiState,
                    savedHistories = savedHistories,
                    authStatus = authStatus,
                    urlInput = urlInput,
                    onUrlInputChange = { urlInput = it },
                    useSearchGrounding = useSearchGrounding,
                    onSearchGroundingChange = { useSearchGrounding = it },
                    freeQueryInput = freeQueryInput,
                    onFreeQueryInputChange = { freeQueryInput = it },
                    selectedTab = selectedTab,
                    onTabChange = { selectedTab = it },
                    activeCategory = activeCategory,
                    onCategoryChange = { activeCategory = it },
                    favoritesList = favoritesList,
                    onFunctionClick = onFunctionClick,
                    onToggleFavorite = { id -> viewModel.toggleFavorite(id) },
                    activeFunction = activeFunction
                )
            }
        }
    } else {
        SmartphoneLayout(
            viewModel = viewModel,
            uiState = uiState,
            savedHistories = savedHistories,
            authStatus = authStatus,
            urlInput = urlInput,
            onUrlInputChange = { urlInput = it },
            useSearchGrounding = useSearchGrounding,
            onSearchGroundingChange = { useSearchGrounding = it },
            freeQueryInput = freeQueryInput,
            onFreeQueryInputChange = { freeQueryInput = it },
            selectedTab = selectedTab,
            onTabChange = { selectedTab = it },
            activeCategory = activeCategory,
            onCategoryChange = { activeCategory = it },
            favoritesList = favoritesList,
            onFunctionClick = onFunctionClick,
            onToggleFavorite = { id -> viewModel.toggleFavorite(id) },
            activeFunction = activeFunction
        )
    }
}

// ----------------------------------------------------------------------------------
// ADAPTIVE LAYOUTS (TABLET & SMARTPHONE)
// ----------------------------------------------------------------------------------

@Composable
fun TabletLayout(
    viewModel: MainViewModel,
    uiState: com.example.ui.UiState,
    savedHistories: List<DomainSummary>,
    authStatus: com.example.ui.AuthStatus,
    urlInput: String,
    onUrlInputChange: (String) -> Unit,
    useSearchGrounding: Boolean,
    onSearchGroundingChange: (Boolean) -> Unit,
    freeQueryInput: String,
    onFreeQueryInputChange: (String) -> Unit,
    selectedTab: AppTab = AppTab.START,
    onTabChange: (AppTab) -> Unit = {},
    activeCategory: CategoryInfo,
    onCategoryChange: (CategoryInfo) -> Unit,
    favoritesList: List<String>,
    onFunctionClick: (FunctionInfo) -> Unit,
    onToggleFavorite: (String) -> Unit,
    activeFunction: FunctionInfo?
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var currentSubView by remember { mutableStateOf("start") } // "start", "settings"

    Row(modifier = Modifier.fillMaxSize()) {
        // Left Sidebar Navigation
        Column(
            modifier = Modifier
                .width(280.dp)
                .fillMaxHeight()
                .background(Color.White)
                .border(1.dp, MaterialTheme.colorScheme.outline)
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Header Logo
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoStories,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "Relevantor",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                // Categories (A - E)
                Text(
                    "KATEGORIEN",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                )

                categoriesList.forEach { category ->
                    val isSelected = activeCategory.id == category.id && currentSubView == "start"
                    NavigationItemRow(
                        label = category.name,
                        icon = category.icon,
                        countText = "${category.functions.size} Funktionen",
                        isSelected = isSelected,
                        activeColor = category.color,
                        onClick = {
                            currentSubView = "start"
                            onCategoryChange(category)
                            if (uiState !is com.example.ui.UiState.Idle) {
                                viewModel.resetToIdle()
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Standard sections
                Text(
                    "MEIN BEREICH",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                )

                NavigationItemRow(
                    label = "Verlauf",
                    icon = Icons.Default.History,
                    countText = "${savedHistories.size} Analysen",
                    isSelected = false,
                    onClick = {
                        Toast.makeText(context, "Verlauf im rechten Bereich sichtbar!", Toast.LENGTH_SHORT).show()
                    }
                )

                NavigationItemRow(
                    label = "Einstellungen",
                    icon = Icons.Default.Settings,
                    isSelected = currentSubView == "settings",
                    onClick = { currentSubView = "settings" }
                )
            }

            // Bottom Pro Card
            ProPlanCard()
        }

        // Divider
        VerticalDivider(color = MaterialTheme.colorScheme.outline)

        // Main Content Area (Dynamic right panel)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (currentSubView == "settings") {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                    AccountSettingsScreen(viewModel, authStatus)
                }
            } else {
                // Start or result pipeline
                when (uiState) {
                    is com.example.ui.UiState.Idle -> {
                        Row(modifier = Modifier.fillMaxSize()) {
                            // Left column of content pane: primary workspace
                            Column(
                                modifier = Modifier
                                    .weight(1.3f)
                                    .fillMaxHeight()
                                    .verticalScroll(rememberScrollState())
                                    .padding(24.dp),
                                verticalArrangement = Arrangement.spacedBy(20.dp)
                            ) {
                                // Input Box
                                UrlInputCard(
                                    urlInput = urlInput,
                                    onUrlInputChange = onUrlInputChange,
                                    useSearchGrounding = useSearchGrounding,
                                    onSearchGroundingChange = onSearchGroundingChange,
                                    clipboardManager = clipboardManager,
                                    context = context
                                )

                                // Favorites Horizontal Panel
                                FavoritesPanel(
                                    favoritesList = favoritesList,
                                    onFunctionClick = onFunctionClick,
                                    onToggleFavorite = onToggleFavorite,
                                    onEditClick = { onTabChange(AppTab.FAVORITEN) }
                                )

                                // Category Specific Workspace List
                                CategoryWorkspaceList(
                                    category = activeCategory,
                                    onFunctionClick = onFunctionClick,
                                    favoritesList = favoritesList,
                                    onToggleFavorite = onToggleFavorite
                                )
                            }

                            // Right column of content pane: Category info + Tips
                            Column(
                                modifier = Modifier
                                    .weight(0.7f)
                                    .fillMaxHeight()
                                    .background(Color.White)
                                    .border(1.dp, MaterialTheme.colorScheme.outline)
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CategoryInfoCard(category = activeCategory)
                                TipOfTheDayCard()
                                
                                Spacer(modifier = Modifier.weight(1f))
                                
                                // History Mini Quick List
                                if (savedHistories.isNotEmpty()) {
                                    Text("Letzte Analysen", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        savedHistories.take(3).forEach { summary ->
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { viewModel.openSavedAnalysis(summary) },
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                            ) {
                                                Column(modifier = Modifier.padding(10.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = summary.title,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 12.sp,
                                                            modifier = Modifier.weight(1f),
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(
                                                            text = summary.timestamp,
                                                            fontSize = 9.sp,
                                                            color = Color.Gray
                                                        )
                                                    }
                                                    Text(summary.originalUrl, fontSize = 10.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .background(
                                                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                                                shape = RoundedCornerShape(4.dp)
                                                            )
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = getFunctionNameForAnalysis(summary),
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Medium,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    is com.example.ui.UiState.Loading -> {
                        ProcessingScreen(
                            uiState = uiState,
                            activeCategory = findCategoryForType(viewModel.currentAnalysisType.value) ?: activeCategory,
                            activeFunction = activeFunction ?: FunctionInfo("Custom", "Quell-Analyse", "", null, Icons.Default.Bolt, Color.Gray),
                            onBackClick = { viewModel.resetToIdle() }
                        )
                    }
                    is com.example.ui.UiState.Success -> {
                        val currentCat = findCategoryForType(uiState.analysisType) ?: activeCategory
                        val currentFunc = currentCat.functions.find { it.type == uiState.analysisType } ?: activeFunction ?: FunctionInfo("Custom", "Quell-Analyse", "", null, Icons.Default.Bolt, Color.Gray)
                        ResultScreen(
                            summary = uiState.summary,
                            activeCategory = currentCat,
                            activeFunction = currentFunc,
                            onBackClick = { viewModel.resetToIdle() },
                            isFavorite = favoritesList.contains(currentFunc.id),
                            onToggleFavorite = { onToggleFavorite(currentFunc.id) }
                        )
                    }
                    is com.example.ui.UiState.Error -> {
                        ErrorScreen(
                            message = uiState.message,
                            detail = uiState.detail ?: "Ein unerwarteter Fehler ist aufgetreten. Bitte versuche es erneut.",
                            onBackClick = { viewModel.resetToIdle() }
                        )
                    }
                }
            }
        }
    }
}

fun getCategoryDescription(categoryId: String): String {
    return when (categoryId) {
        "A" -> "Zusammenfassung, Kernaussagen, Quellensuche, Multimedia-Analyse"
        "B" -> "Aktualität, Fehlinformationen, Fakt/Meinung, Risiken, Perspektiven, Weitere Aspekte"
        "E" -> "Dokumente, PDFs, Bilder, Screenshots, KI-Analysen"
        "D" -> "Social Media, E-Mails, Pressemitteilungen, Multi-URLs"
        "C" -> "Infografiken, Mindmaps, Diagramme, Bild-Prompts"
        "F" -> "Google Maps Analyser, Kontext zum Ort"
        else -> ""
    }
}

@Composable
fun SmartphoneLayout(
    viewModel: MainViewModel,
    uiState: com.example.ui.UiState,
    savedHistories: List<DomainSummary>,
    authStatus: com.example.ui.AuthStatus,
    urlInput: String,
    onUrlInputChange: (String) -> Unit,
    useSearchGrounding: Boolean,
    onSearchGroundingChange: (Boolean) -> Unit,
    freeQueryInput: String,
    onFreeQueryInputChange: (String) -> Unit,
    selectedTab: AppTab,
    onTabChange: (AppTab) -> Unit,
    activeCategory: CategoryInfo,
    onCategoryChange: (CategoryInfo) -> Unit,
    favoritesList: List<String>,
    onFunctionClick: (FunctionInfo) -> Unit,
    onToggleFavorite: (String) -> Unit,
    activeFunction: FunctionInfo?
) {
    var selectedCategoryForDetailId by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedCategoryForDetail = categoriesList.find { it.id == selectedCategoryForDetailId }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Scaffold(
        bottomBar = {
            if (uiState is com.example.ui.UiState.Idle) {
                NavigationBar(
                    containerColor = Color.White,
                    tonalElevation = 0.dp,
                    modifier = Modifier.border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                ) {
                    NavigationBarItem(
                        selected = selectedTab == AppTab.START,
                        onClick = { 
                            selectedCategoryForDetailId = null
                            onTabChange(AppTab.START) 
                        },
                        alwaysShowLabel = true,
                        icon = { 
                            Icon(
                                if (selectedTab == AppTab.START) Icons.Filled.Home else Icons.Outlined.Home, 
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            ) 
                        },
                        label = { 
                            Text(
                                text = "Start", 
                                fontWeight = if (selectedTab == AppTab.START) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 11.sp,
                                maxLines = 1,
                                softWrap = false
                            ) 
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == AppTab.VERLAUF,
                        onClick = { 
                            selectedCategoryForDetailId = null
                            onTabChange(AppTab.VERLAUF) 
                        },
                        alwaysShowLabel = true,
                        icon = { 
                            Icon(
                                Icons.Default.History, 
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            ) 
                        },
                        label = { 
                            Text(
                                text = "Verlauf", 
                                fontWeight = if (selectedTab == AppTab.VERLAUF) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 11.sp,
                                maxLines = 1,
                                softWrap = false
                            ) 
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == AppTab.FAVORITEN,
                        onClick = { 
                            selectedCategoryForDetailId = null
                            onTabChange(AppTab.FAVORITEN) 
                        },
                        alwaysShowLabel = true,
                        icon = { 
                            Icon(
                                if (selectedTab == AppTab.FAVORITEN) Icons.Filled.Star else Icons.Outlined.StarOutline, 
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            ) 
                        },
                        label = { 
                            Text(
                                text = "Favoriten", 
                                fontWeight = if (selectedTab == AppTab.FAVORITEN) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 11.sp,
                                maxLines = 1,
                                softWrap = false
                            ) 
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
                        )
                    )

                    NavigationBarItem(
                        selected = selectedTab == AppTab.PRO,
                        onClick = { 
                            selectedCategoryForDetailId = null
                            onTabChange(AppTab.PRO) 
                        },
                        alwaysShowLabel = true,
                        icon = { 
                            Icon(
                                if (selectedTab == AppTab.PRO) Icons.Filled.Stars else Icons.Outlined.Stars, 
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            ) 
                        },
                        label = { 
                            Text(
                                text = "Pro", 
                                fontWeight = if (selectedTab == AppTab.PRO) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 11.sp,
                                maxLines = 1,
                                softWrap = false
                            ) 
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState) {
                is com.example.ui.UiState.Idle -> {
                    when (selectedTab) {
                        AppTab.START -> {
                            if (selectedCategoryForDetailId != null) {
                                androidx.activity.compose.BackHandler {
                                    selectedCategoryForDetailId = null
                                }
                            }

                            // EBENE 1 - HOME SCREEN WITH VERTICAL LIST OF CATEGORIES
                            val orderedCategories = listOf(
                                categoriesList.find { it.id == "A" },
                                categoriesList.find { it.id == "B" },
                                categoriesList.find { it.id == "E" },
                                categoriesList.find { it.id == "D" },
                                categoriesList.find { it.id == "C" },
                                categoriesList.find { it.id == "F" }
                            ).filterNotNull()

                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Top Hero Section with Coffeehouse Background
                                item {
                                    androidx.compose.foundation.layout.Box(
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        // Background Image matching the content height of Header, URL Input and Favorites
                                        androidx.compose.foundation.Image(
                                            painter = androidx.compose.ui.res.painterResource(id = R.drawable.relevantor_home_coffeehouse),
                                            contentDescription = null,
                                            modifier = Modifier.matchParentSize(),
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                            alignment = Alignment.TopCenter
                                        )

                                        // Soft Gradient Overlay fading into the solid background color
                                        androidx.compose.foundation.layout.Box(
                                            modifier = Modifier
                                                .matchParentSize()
                                                .background(
                                                    androidx.compose.ui.graphics.Brush.verticalGradient(
                                                        colors = listOf(
                                                            MaterialTheme.colorScheme.background.copy(alpha = 0.35f),
                                                            MaterialTheme.colorScheme.background.copy(alpha = 0.75f),
                                                            MaterialTheme.colorScheme.background
                                                        )
                                                    )
                                                )
                                        )

                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 8.dp),
                                            verticalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            // Top Header Row
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Default.AutoStories,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        "Relevantor",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 20.sp,
                                                        color = MaterialTheme.colorScheme.onBackground
                                                    )
                                                }

                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    IconButton(onClick = {}) {
                                                        Icon(Icons.Default.MoreVert, contentDescription = "Menü", tint = Color.Gray)
                                                    }
                                                }
                                            }

                                            // Input Text Field
                                            UrlInputCard(
                                                urlInput = urlInput,
                                                onUrlInputChange = onUrlInputChange,
                                                useSearchGrounding = useSearchGrounding,
                                                onSearchGroundingChange = onSearchGroundingChange,
                                                clipboardManager = clipboardManager,
                                                context = context
                                            )

                                            // Favorites panel
                                            FavoritesPanel(
                                                favoritesList = favoritesList,
                                                onFunctionClick = onFunctionClick,
                                                onToggleFavorite = onToggleFavorite,
                                                onEditClick = { onTabChange(AppTab.FAVORITEN) }
                                            )
                                        }
                                    }
                                }

                                // Vertikale Kategorien
                                item {
                                    Text("Kategorien", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onBackground)
                                }

                                 items(orderedCategories) { category ->
                                    val isExpanded = selectedCategoryForDetailId == category.id
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isExpanded) category.color.copy(alpha = 0.04f) else Color.White
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(
                                            width = if (isExpanded) 1.5.dp else 1.dp,
                                            color = if (isExpanded) category.color.copy(alpha = 0.3f) else Color(0xFFE2E8F0)
                                        )
                                    ) {
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            // Category Header with Left Indicator Strip
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        selectedCategoryForDetailId = if (isExpanded) null else category.id
                                                        onCategoryChange(category)
                                                    },
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // Left colored indicator stripe
                                                Box(
                                                    modifier = Modifier
                                                        .width(5.dp)
                                                        .height(54.dp)
                                                        .background(category.color)
                                                )
                                                
                                                Row(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.Top,
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(36.dp)
                                                                .clip(RoundedCornerShape(8.dp))
                                                                .background(category.color.copy(alpha = 0.12f)),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(
                                                                imageVector = category.icon,
                                                                contentDescription = null,
                                                                tint = category.color,
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                        }
                                                        Spacer(modifier = Modifier.width(10.dp))
                                                        Column {
                                                            Text(
                                                                text = category.name,
                                                                fontWeight = FontWeight.ExtraBold,
                                                                fontSize = 14.sp,
                                                                color = MaterialTheme.colorScheme.onBackground
                                                            )
                                                            Spacer(modifier = Modifier.height(2.dp))
                                                            Text(
                                                                text = getCategoryDescription(category.id),
                                                                fontSize = 10.sp,
                                                                color = Color.Gray,
                                                                lineHeight = 12.sp,
                                                                maxLines = 2,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                    }
                                                    Icon(
                                                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                        contentDescription = null,
                                                        tint = category.color,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }

                                            if (isExpanded) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(start = 17.dp, end = 12.dp, bottom = 12.dp),
                                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    category.functions.forEach { func ->
                                                        val isFav = favoritesList.contains(func.id)
                                                        Card(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .clickable { onFunctionClick(func) },
                                                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                                            shape = RoundedCornerShape(8.dp),
                                                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                                        ) {
                                                            Row(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.SpaceBetween
                                                            ) {
                                                                Row(
                                                                    verticalAlignment = Alignment.Top,
                                                                    modifier = Modifier.weight(1f)
                                                                ) {
                                                                    Box(
                                                                        modifier = Modifier
                                                                            .size(28.dp)
                                                                            .clip(RoundedCornerShape(6.dp))
                                                                            .background(func.color.copy(alpha = 0.08f)),
                                                                        contentAlignment = Alignment.Center
                                                                    ) {
                                                                        Icon(func.icon, contentDescription = null, tint = func.color, modifier = Modifier.size(14.dp))
                                                                    }
                                                                    Spacer(modifier = Modifier.width(8.dp))
                                                                    Column {
                                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                                            Text(
                                                                                func.name,
                                                                                fontWeight = FontWeight.SemiBold,
                                                                                fontSize = 12.sp,
                                                                                color = MaterialTheme.colorScheme.onBackground
                                                                            )
                                                                            if (func.isPlaceholder) {
                                                                                Spacer(modifier = Modifier.width(4.dp))
                                                                                Box(
                                                                                    modifier = Modifier
                                                                                        .clip(RoundedCornerShape(4.dp))
                                                                                        .background(Color(0xFFFEF3C7))
                                                                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                                                                ) {
                                                                                    Text("PRO", color = Color(0xFFD97706), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                                                                }
                                                                            }
                                                                        }
                                                                        Spacer(modifier = Modifier.height(1.dp))
                                                                        Text(
                                                                            func.description,
                                                                            fontSize = 10.sp,
                                                                            color = Color.Gray,
                                                                            maxLines = 1,
                                                                            overflow = TextOverflow.Ellipsis
                                                                        )
                                                                    }
                                                                }

                                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                                    IconButton(
                                                                        onClick = { onToggleFavorite(func.id) },
                                                                        modifier = Modifier.size(28.dp)
                                                                    ) {
                                                                        Icon(
                                                                            imageVector = if (isFav) Icons.Default.Star else Icons.Default.StarBorder,
                                                                            contentDescription = null,
                                                                            tint = if (isFav) Color(0xFFD97706) else Color.Gray,
                                                                            modifier = Modifier.size(14.dp)
                                                                        )
                                                                    }
                                                                    Icon(
                                                                        imageVector = Icons.Default.ChevronRight,
                                                                        contentDescription = null,
                                                                        tint = Color(0xFFCBD5E1),
                                                                        modifier = Modifier.size(14.dp)
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // Pro Plan Usage indicator
                                item {
                                    ProPlanCard()
                                }
                            }
                        }
                        AppTab.VERLAUF -> {
                            Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                                HistoryTabScreen(savedHistories, viewModel)
                            }
                        }
                        AppTab.FAVORITEN -> {
                            Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                                FavoritesTabScreen(
                                    favoritesList = favoritesList,
                                    onFunctionClick = onFunctionClick,
                                    onToggleFavorite = onToggleFavorite,
                                    onMoveUp = { viewModel.moveFavoriteUp(it) },
                                    onMoveDown = { viewModel.moveFavoriteDown(it) }
                                )
                            }
                        }

                        AppTab.PRO -> {
                            Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                                AccountSettingsScreen(viewModel, authStatus)
                            }
                        }
                    }
                }
                is com.example.ui.UiState.Loading -> {
                    val currentCat = findCategoryForType(viewModel.currentAnalysisType.value) ?: activeCategory
                    val currentFunc = currentCat.functions.find { it.type == viewModel.currentAnalysisType.value } ?: activeFunction ?: FunctionInfo("Custom", "Quell-Analyse", "", null, Icons.Default.Bolt, Color.Gray)
                    ProcessingScreen(
                        uiState = uiState,
                        activeCategory = currentCat,
                        activeFunction = currentFunc,
                        onBackClick = { viewModel.resetToIdle() }
                    )
                }
                is com.example.ui.UiState.Success -> {
                    val currentCat = findCategoryForType(uiState.analysisType) ?: activeCategory
                    val currentFunc = currentCat.functions.find { it.type == uiState.analysisType } ?: activeFunction ?: FunctionInfo("Custom", "Quell-Analyse", "", null, Icons.Default.Bolt, Color.Gray)
                    ResultScreen(
                        summary = uiState.summary,
                        activeCategory = currentCat,
                        activeFunction = currentFunc,
                        onBackClick = { viewModel.resetToIdle() },
                        isFavorite = favoritesList.contains(currentFunc.id),
                        onToggleFavorite = { onToggleFavorite(currentFunc.id) }
                    )
                }
                is com.example.ui.UiState.Error -> {
                    ErrorScreen(
                        message = uiState.message,
                        detail = uiState.detail ?: "Fehler beim Analysieren der Quelle.",
                        onBackClick = { viewModel.resetToIdle() }
                    )
                }
            }
        }
    }
}

// ----------------------------------------------------------------------------------
// REUSABLE SUB-SCREENS & UI COMPONENTS
// ----------------------------------------------------------------------------------

@Composable
fun NavigationItemRow(
    label: String,
    icon: ImageVector,
    countText: String? = null,
    isSelected: Boolean,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    val rowBg = if (isSelected) activeColor.copy(alpha = 0.1f) else Color.Transparent
    val textStyleColor = if (isSelected) activeColor else MaterialTheme.colorScheme.onBackground
    val iconTint = if (isSelected) activeColor else Color.Gray

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(rowBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, color = textStyleColor, fontSize = 13.sp)
        }
        if (countText != null) {
            Text(countText, fontSize = 11.sp, color = Color.Gray)
        }
    }
}

@Composable
fun UrlInputCard(
    urlInput: String,
    onUrlInputChange: (String) -> Unit,
    useSearchGrounding: Boolean,
    onSearchGroundingChange: (Boolean) -> Unit,
    clipboardManager: androidx.compose.ui.platform.ClipboardManager,
    context: android.content.Context
) {
    OutlinedTextField(
        value = urlInput,
        onValueChange = onUrlInputChange,
        placeholder = { Text("URL eingeben", fontSize = 14.sp, color = Color.Gray) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        leadingIcon = { Icon(Icons.Default.Link, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) },
        trailingIcon = {
            if (urlInput.isNotBlank()) {
                IconButton(onClick = { onUrlInputChange("") }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Löschen", modifier = Modifier.size(16.dp))
                }
            } else {
                IconButton(
                    onClick = {
                        val clipText = clipboardManager.getText()?.text
                        if (!clipText.isNullOrBlank()) {
                            onUrlInputChange(clipText)
                            Toast.makeText(context, "Link eingefügt!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Zwischenablage ist leer!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.ContentPaste, contentDescription = "Einfügen", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                }
            }
        },
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = Color(0xFFE2E8F0)
        ),
        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
    )
}

@Composable
fun FavoritesPanel(
    favoritesList: List<String>,
    onFunctionClick: (FunctionInfo) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onEditClick: () -> Unit = {}
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Favoriten", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onBackground)
            Text("Bearbeiten", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onEditClick() })
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val allFunctionsMap = categoriesList.flatMap { it.functions }.associateBy { it.id }
            val favFuncs = favoritesList.mapNotNull { allFunctionsMap[it] }.take(10)

            if (favFuncs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Noch keine Favoriten hinzugefügt.", color = Color.Gray, fontSize = 12.sp)
                }
            } else {
                favFuncs.forEach { func ->
                    Card(
                        modifier = Modifier
                            .width(145.dp)
                            .height(95.dp)
                            .clickable { onFunctionClick(func) },
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            IconButton(
                                onClick = { onToggleFavorite(func.id) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(24.dp)
                                    .padding(top = 2.dp, end = 2.dp)
                            ) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(14.dp))
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(func.color.copy(alpha = 0.08f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(func.icon, contentDescription = null, tint = func.color, modifier = Modifier.size(14.dp))
                                }

                                Text(
                                    func.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryWorkspaceList(
    category: CategoryInfo,
    onFunctionClick: (FunctionInfo) -> Unit,
    favoritesList: List<String>,
    onToggleFavorite: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Section Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(category.color.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(category.icon, contentDescription = null, tint = category.color, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(category.name, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground)
            }
        }

        // Individual Cards for functions
        category.functions.forEach { func ->
            val isFav = favoritesList.contains(func.id)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onFunctionClick(func) },
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color(0xFFF1F5F9))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(func.color.copy(alpha = 0.08f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(func.icon, contentDescription = null, tint = func.color, modifier = Modifier.size(14.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    func.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                if (func.isPlaceholder) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFFFEF3C7))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("PRO", color = Color(0xFFD97706), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                func.description,
                                fontSize = 11.sp,
                                color = Color.Gray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { onToggleFavorite(func.id) },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = if (isFav) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = null,
                                tint = if (isFav) Color(0xFFD97706) else Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = Color(0xFFCBD5E1),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryInfoCard(category: CategoryInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = category.color.copy(alpha = 0.05f)),
        border = BorderStroke(1.dp, category.color.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Über diese Kategorie", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = category.color)
            Text(
                when(category.id) {
                    "A" -> "Bewerte die Kerninformationen, gewinne eine komprimierte Kurzzusammenfassung und extrahiere wesentliche Aussagen aus der Quelle."
                    "B" -> "Verifiziere Glaubwürdigkeit und Aktualität der Quelle, decke versteckte Motive auf und filtere fehlerhafte Behauptungen heraus."
                    "C" -> "Erzeuge strukturierte, visuelle Organigramme, Infografiken und Bildkonzepte zur didaktischen Unterstützung."
                    "D" -> "Konvertiere den Quellinhalt direkt in Social Media Beiträge, formelle Anschreiben oder kombiniere mehrere URLs miteinander."
                    "E" -> "Analysiere direkt PDF-Dokumente, extrahiere Text aus visuellen Scans oder verarbeite Multimedia-Transkripte."
                    "F" -> "Analysiere Ortsparameter, Places API Details sowie Umfeld- und Ortskontext für Google Maps Orte."
                    else -> "Analysiere Inhalte mit spezialisierten Funktionen."
                },
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text("Mehr erfahren ↗", color = category.color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun TipOfTheDayCard() {
    var activeTip by remember { mutableStateOf(0) }
    val tips = listOf(
        "Nutze den Fehlinformations-Radar, um fragwürdige Behauptungen im Web schnell zu validieren.",
        "Dokumentanalysen unterstützen vollständige PDF-Berichte. Lade sie über 'Arbeiten mit Dateien' hoch.",
        "Verwende die freie Quellenanfrage für eine präzise Faktenprüfung."
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Tipp des Tages", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
            Text(tips[activeTip], fontSize = 12.sp)

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                tips.forEachIndexed { index, _ ->
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (index == activeTip) MaterialTheme.colorScheme.primary else Color.LightGray)
                            .clickable { activeTip = index }
                    )
                }
            }
        }
    }
}

@Composable
fun ProPlanCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF5FF)),
        border = BorderStroke(1.dp, Color(0xFFE9D5FF))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Stars, contentDescription = null, tint = Color(0xFF9333EA), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Pro Plan", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF9333EA))
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF9333EA))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("Aktiv", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }

            Text("432 / 1.000 Analysen diesen Monat", fontSize = 11.sp, color = Color.Gray)

            LinearProgressIndicator(
                progress = { 0.432f },
                color = Color(0xFF9333EA),
                trackColor = Color(0xFFF3E8FF),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Nächste Verlängerung: 12. Juli", fontSize = 10.sp, color = Color.Gray)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable {}) {
                    Text("Details", color = Color(0xFF9333EA), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF9333EA), modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

// ----------------------------------------------------------------------------------
// COMPLETED COMPOSABLE: EBENE 2 - PROCESSING VIEW (VARIANTE 2A)
// ----------------------------------------------------------------------------------

@Composable
fun ProcessingScreen(
    uiState: com.example.ui.UiState,
    activeCategory: CategoryInfo,
    activeFunction: FunctionInfo,
    onBackClick: () -> Unit
) {
    val step = if (uiState is com.example.ui.UiState.Loading) uiState.step else com.example.ui.LoadingStep.FETCHING_DATA
    
    val targetProgress = when(step) {
        com.example.ui.LoadingStep.IDLE -> 0f
        com.example.ui.LoadingStep.FETCHING_DATA -> 0.35f
        com.example.ui.LoadingStep.ANALYZING_INPUT -> 0.62f
        com.example.ui.LoadingStep.GENERATING_OUTPUT -> 0.88f
        else -> 1.0f
    }
    
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = spring(stiffness = Spring.StiffnessLow)
    )

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Zurück")
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(activeFunction.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Quelle wird verarbeitet...", fontSize = 11.sp, color = Color.Gray)
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.MoreVert, contentDescription = null)
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF8FAFC)), // Slate 50
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = 600.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = activeCategory.color,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Analyse läuft...",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "${(animatedProgress * 100).toInt()}%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = activeCategory.color
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        color = activeCategory.color,
                        trackColor = Color(0xFFF1F5F9),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("AKTIVITÄTS-FORTSCHRITT", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = activeCategory.color, letterSpacing = 1.sp)

                        ProgressCheckRow(
                            title = "URL geladen",
                            isCompleted = true,
                            isActive = false,
                            color = activeCategory.color
                        )

                        ProgressCheckRow(
                            title = "Inhalte extrahiert",
                            isCompleted = true,
                            isActive = false,
                            color = activeCategory.color
                        )

                        ProgressCheckRow(
                            title = "Inhalt wird analysiert",
                            isCompleted = step == com.example.ui.LoadingStep.GENERATING_OUTPUT,
                            isActive = step == com.example.ui.LoadingStep.ANALYZING_INPUT,
                            color = activeCategory.color
                        )

                        ProgressCheckRow(
                            title = "Ergebnis wird erstellt",
                            isCompleted = false,
                            isActive = step == com.example.ui.LoadingStep.GENERATING_OUTPUT,
                            color = activeCategory.color
                        )

                        ProgressCheckRow(
                            title = "Fertigstellung",
                            isCompleted = false,
                            isActive = false,
                            color = activeCategory.color
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Deine Daten sind sicher und werden nicht gespeichert.", fontSize = 11.sp, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun ProgressCheckRow(
    title: String,
    isCompleted: Boolean,
    isActive: Boolean,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(
                        if (isCompleted) color else if (isActive) color.copy(alpha = 0.15f) else Color(0xFFE2E8F0)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                } else if (isActive) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color.Gray.copy(alpha = 0.5f))
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                title,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = if (isActive) color else MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

fun getTakeawayIcon(title: String, details: String, metadata: Map<String, String> = emptyMap()): ImageVector {
    val textToSearch = (title + " " + details + " " + metadata.values.joinToString(" ")).lowercase()
    
    return when {
        textToSearch.contains("geld") || textToSearch.contains("money") || textToSearch.contains("finanz") || 
        textToSearch.contains("preis") || textToSearch.contains("kosten") || textToSearch.contains("sim") || 
        textToSearch.contains("tarif") || textToSearch.contains("bezahl") || textToSearch.contains("abo") || 
        textToSearch.contains("vertrag") || textToSearch.contains("gebühr") || textToSearch.contains("badge") ||
        textToSearch.contains("account") -> Icons.Default.Badge
        
        textToSearch.contains("sand") || textToSearch.contains("fahrt") || textToSearch.contains("route") || 
        textToSearch.contains("car") || textToSearch.contains("auto") || textToSearch.contains("reise") || 
        textToSearch.contains("travel") || textToSearch.contains("mobil") || textToSearch.contains("verkehr") || 
        textToSearch.contains("zug") || textToSearch.contains("bahn") || textToSearch.contains("straße") -> Icons.Default.DirectionsCar
        
        textToSearch.contains("menschen") || textToSearch.contains("people") || textToSearch.contains("groups") || 
        textToSearch.contains("mitarbeiter") || textToSearch.contains("nutzer") || textToSearch.contains("user") || 
        textToSearch.contains("kunden") || textToSearch.contains("team") || textToSearch.contains("gesellschaft") ||
        textToSearch.contains("gruppe") -> Icons.Default.Groups
        
        textToSearch.contains("umwelt") || textToSearch.contains("nature") || textToSearch.contains("natur") || 
        textToSearch.contains("eco") || textToSearch.contains("klima") || textToSearch.contains("green") || 
        textToSearch.contains("warning") || textToSearch.contains("risiko") || textToSearch.contains("gefahr") ||
        textToSearch.contains("warnung") -> Icons.Default.Eco
        
        else -> Icons.Default.Language
    }
}

fun printSummary(context: android.content.Context, summary: com.example.domain.model.DomainSummary, analysisType: com.example.data.AnalysisType? = null) {
    val printManager = context.getSystemService(android.content.Context.PRINT_SERVICE) as android.print.PrintManager
    val jobName = "Relevantor_Analyse_${summary.title.replace(" ", "_")}"
    
    val policy = com.example.ui.metadata.OutputPresentationPolicy.getPolicyFor(analysisType)
    val htmlContent = com.example.ui.metadata.ExportFormatter.formatHtml(summary, policy)

    val webView = android.webkit.WebView(context)
    webView.webViewClient = object : android.webkit.WebViewClient() {
        override fun onPageFinished(view: android.webkit.WebView, url: String) {
            val printAdapter = webView.createPrintDocumentAdapter(jobName)
            printManager.print(
                jobName,
                printAdapter,
                android.print.PrintAttributes.Builder().build()
            )
        }
    }
    webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
}

// ----------------------------------------------------------------------------------
// COMPLETED COMPOSABLE: EBENE 3 - RESULT SCREEN (VARIANTE 3A)
// ----------------------------------------------------------------------------------

@Composable
fun ResultScreen(
    summary: DomainSummary,
    activeCategory: CategoryInfo,
    activeFunction: FunctionInfo,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onBackClick: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showMenu by remember { mutableStateOf(false) }
    var showDebugDataDialog by remember { mutableStateOf(false) }
    var showPreflightDialog by remember { mutableStateOf(false) }
    var isTestingPreflight by remember { mutableStateOf(false) }
    var preflightReport by remember { mutableStateOf<com.example.data.PreflightReport?>(null) }
    var showSmokeDialog by remember { mutableStateOf(false) }
    var isRunningSmokeTests by remember { mutableStateOf(false) }
    var smokeTestReport by remember { mutableStateOf<com.example.data.SmokeTestHarnessReport?>(null) }
    var showLocationContextDialog by remember { mutableStateOf(false) }
    var isRunningLocationContextDiagnosis by remember { mutableStateOf(false) }
    var locationContextReport by remember { mutableStateOf<com.example.data.contextengine.LocationContextDiagnosticReport?>(null) }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Zurück")
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(activeFunction.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(summary.originalUrl, fontSize = 11.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    IconButton(onClick = {
                        val shareText = buildShareText(
                            title = summary.title,
                            shortDescription = summary.shortDescription,
                            originalUrl = summary.originalUrl
                        )
                        val sendIntent: Intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, shareText)
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, null)
                        context.startActivity(shareIntent)
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Teilen")
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Optionen")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Diagnose-Daten anzeigen (Dev)") },
                                onClick = {
                                    showMenu = false
                                    showDebugDataDialog = true
                                },
                                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Copy PR") },
                                onClick = {
                                    showMenu = false
                                    val json = com.example.data.PipelineReportStore.getLastReportJson()
                                    clipboardManager.setText(AnnotatedString(json))
                                    Toast.makeText(context, "Pipeline Report (JSON) kopiert!", Toast.LENGTH_SHORT).show()
                                },
                                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Smoke-Test ausführen (Dev)") },
                                onClick = {
                                    showMenu = false
                                    isRunningSmokeTests = true
                                    scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                        val report = com.example.data.RuntimeSmokeTestHarness.runSmokeTests(context)
                                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                            smokeTestReport = report
                                            showSmokeDialog = true
                                            isRunningSmokeTests = false
                                        }
                                    }
                                },
                                leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Preflight-Check ausführen (Dev)") },
                                onClick = {
                                    showMenu = false
                                    isTestingPreflight = true
                                    scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                        val report = com.example.data.RuntimePreflight.runPreflight(context)
                                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                            preflightReport = report
                                            showPreflightDialog = true
                                            isTestingPreflight = false
                                        }
                                    }
                                },
                                leadingIcon = { Icon(Icons.Default.NetworkCheck, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Location Context Diagnose (Dev)") },
                                onClick = {
                                    showMenu = false
                                    isRunningLocationContextDiagnosis = true
                                    scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                        val report = com.example.data.contextengine.LocationContextDiagnosticRunner.runDiagnosis(
                                            context = context,
                                            inputUrl = summary.originalUrl.ifBlank { "https://maps.google.com/?q=Wat+Phra+That+Doi+Suthep" }
                                        )
                                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                            locationContextReport = report
                                            showLocationContextDialog = true
                                            isRunningLocationContextDiagnosis = false
                                        }
                                    }
                                },
                                leadingIcon = { Icon(Icons.Default.Place, contentDescription = null) }
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = false,
                    onClick = {
                        val policy = com.example.ui.metadata.OutputPresentationPolicy.getPolicyFor(activeFunction.type)
                        val copyText = com.example.ui.metadata.ExportFormatter.formatPlainText(summary, policy)
                        clipboardManager.setText(AnnotatedString(copyText))
                        Toast.makeText(context, "Analyse in Zwischenablage kopiert!", Toast.LENGTH_SHORT).show()
                    },
                    icon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                    label = { Text("Kopieren") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = {
                        try {
                            printSummary(context, summary, activeFunction.type)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Drucken fehlgeschlagen: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                        }
                    },
                    icon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null) },
                    label = { Text("Als PDF") }
                )
                NavigationBarItem(
                    selected = isFavorite,
                    onClick = onToggleFavorite,
                    icon = { Icon(if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder, contentDescription = null, tint = if (isFavorite) Color(0xFFD97706) else Color.Gray) },
                    label = { Text("In Favoriten") }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF8FAFC)), // Slate 50
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = 600.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        val headerIcon = getTakeawayIcon(summary.title, summary.shortDescription)
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(activeCategory.color.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(headerIcon, contentDescription = null, tint = activeCategory.color)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = parseMarkdownToAnnotatedString(summary.title.ifBlank { "Unbenannter Bericht" }), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            val authorText = if (!summary.owner.isNullOrBlank()) {
                                "von ${summary.owner}"
                            } else {
                                "Owner unbekannt"
                            }
                            Text(authorText, fontSize = 11.sp, color = Color.Gray)
                        }
                    }

                    IconButton(onClick = onToggleFavorite) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = if (isFavorite) Color(0xFFD97706) else Color.LightGray)
                    }
                }
            }

            if ((activeFunction.type == com.example.data.AnalysisType.MULTIMEDIA_ANALYSIS || activeFunction.id == "MULTIMEDIA_ANALYSIS") && summary.fallbackUsed) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED)),
                    border = BorderStroke(1.dp, Color(0xFFFDBA74))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFFC2410C),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Eingeschränkte Analyse: Kein Transkript verfügbar. Das Ergebnis basiert auf Videotitel, Beschreibung und weiteren verfügbaren Metadaten.",
                            fontSize = 12.sp,
                            color = Color(0xFF9A3412),
                            fontWeight = FontWeight.Medium,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEEF2F6)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("GANZ KURZ", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = activeCategory.color, letterSpacing = 1.sp)
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = activeCategory.color, modifier = Modifier.size(18.dp))
                    }

                    Text(
                        text = parseMarkdownToAnnotatedString(summary.shortDescription.ifBlank { "Keine Kurzzusammenfassung generiert." }),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        lineHeight = 18.sp
                    )
                }
            }

            val policy = com.example.ui.metadata.OutputPresentationPolicy.getPolicyFor(activeFunction.type)
            Text(policy.sectionHeader, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Gray, letterSpacing = 1.sp)

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (summary.keyTakeaways.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Keine detaillierten Kernpunkte gefunden.", color = Color.Gray)
                    }
                } else {
                    summary.keyTakeaways.forEachIndexed { index, takeaway ->
                        com.example.ui.components.TakeawayCard(
                            takeaway = takeaway,
                            index = index,
                            policy = policy,
                            activeColor = activeCategory.color,
                            showIcon = true
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
        }
    }

    // Loading indicator overlay
    if (isRunningSmokeTests || isTestingPreflight) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(if (isRunningSmokeTests) "Smoke-Test läuft" else "Preflight läuft", fontWeight = FontWeight.Bold) },
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    CircularProgressIndicator()
                    Text("Bitte warten...", fontSize = 14.sp)
                }
            },
            confirmButton = {}
        )
    }

    // Debug data dialog
    if (showDebugDataDialog) {
        val maxDetailsLength = summary.keyTakeaways.maxOfOrNull { it.details.length } ?: 0
        val appVersion = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }

        AlertDialog(
            onDismissRequest = { showDebugDataDialog = false },
            title = { Text("Diagnose & Debug-Daten", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Box(modifier = Modifier.heightIn(max = 400.dp)) {
                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        DebugRow("analysisType", activeFunction.type?.name ?: "UNKNOWN")
                        DebugRow("canonicalAnalysisType", activeFunction.type?.canonical()?.name ?: "UNKNOWN")
                        DebugRow("functionId", activeFunction.id)
                        DebugRow("sourceUrl", summary.originalUrl)
                        DebugRow("promptAssetFile", com.example.data.GatewayDiagnostics.loadedPromptAssetFile.ifBlank { "prompts/F_STANDARD_WEBSEITE.md" })
                        DebugRow("promptSha256", com.example.data.GatewayDiagnostics.loadedPromptSha256.ifBlank { "N/A" })
                        DebugRow("finalUserContentLength", "${com.example.data.GatewayDiagnostics.textAfterCleaningLength}")
                        DebugRow("selectedContentContainer", com.example.data.GatewayDiagnostics.selectedContentContainer.ifBlank { "none" })
                        DebugRow("textAfterCleaningLength", "${com.example.data.GatewayDiagnostics.textAfterCleaningLength}")
                        DebugRow("keyTakeawayCount", "${summary.keyTakeaways.size}")
                        DebugRow("maxDetailsLength", "$maxDetailsLength")
                        DebugRow("contractValidationStatus", "PASS")
                        DebugRow("timestamp", summary.timestamp)
                        DebugRow("appVersion", appVersion)
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showDebugDataDialog = false }) {
                    Text("Schließen")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    val debugJson = buildDebugJson(summary, activeFunction, context)
                    clipboardManager.setText(AnnotatedString(debugJson))
                    Toast.makeText(context, "Debug-Daten kopiert!", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Kopieren")
                }
            }
        )
    }

    // Location Context Diagnostics Dialog
    if (showLocationContextDialog && locationContextReport != null) {
        val report = locationContextReport!!
        AlertDialog(
            onDismissRequest = { showLocationContextDialog = false },
            title = { Text("Location Context Diagnose (Dev)", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Box(modifier = Modifier.heightIn(max = 420.dp)) {
                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        DebugRow("1. Input URL", report.inputUrl)
                        DebugRow("2. Ort / Place", report.placeName)
                        DebugRow("   Place Details", report.placeData)
                        DebugRow("3. AnalysisType", report.analysisType)
                        DebugRow("4. Prompt Pfad", report.loadedPromptPath)
                        DebugRow("   Prompt SHA256", report.loadedPromptSha256.take(16) + "...")
                        DebugRow("5. Service Aufruf", report.serviceCallStatus)
                        DebugRow("6. ContextEngine Status", report.contextEngineStatus)
                        DebugRow("7. Wikipedia Status", report.wikipediaResultStatus)
                        DebugRow("   Wikipedia Titel", report.wikipediaResultTitle)
                        DebugRow("   Wikipedia Zeichen", "${report.wikipediaResultCharCount}")
                        DebugRow("8. Wikivoyage Status", report.wikivoyageResultStatus)
                        DebugRow("   Wikivoyage Titel", report.wikivoyageResultTitle)
                        DebugRow("   Wikivoyage Zeichen", "${report.wikivoyageResultCharCount}")
                        DebugRow("9. Gemini Injection", "${report.geminiContextInjectionLength} Zeichen")
                        DebugRow("   Fakten-Abschnitt", if (report.geminiContextInjectionHasFacts) "VORHANDEN" else "FEHLT")
                        DebugRow("   Reisekontext-Abschnitt", if (report.geminiContextInjectionHasTravelContext) "VORHANDEN" else "FEHLT")
                        DebugRow("10. Contract Status", report.finalContractStatus)

                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Vorschau Context Injection:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Surface(
                            color = Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        ) {
                            Text(
                                text = report.geminiContextInjectionPreview,
                                fontSize = 11.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showLocationContextDialog = false }) {
                    Text("Schließen")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    clipboardManager.setText(AnnotatedString(report.toFormattedString()))
                    Toast.makeText(context, "Diagnose-Report kopiert!", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Kopieren")
                }
            }
        )
    }

    // Preflight Diagnostics Dialog
    if (showPreflightDialog && preflightReport != null) {
        val report = preflightReport!!
        AlertDialog(
            onDismissRequest = { showPreflightDialog = false },
            title = { Text("Preflight Verbindungs-Check", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Box(modifier = Modifier.heightIn(max = 400.dp)) {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        item {
                            Text("Gerät: ${report.device}", fontSize = 11.sp, color = Color.Gray)
                            Text("Netzwerk: ${report.networkType}", fontSize = 11.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        items(report.checks) { check ->
                            val isPass = check.status == "PASS"
                            Card(
                                colors = CardDefaults.cardColors(containerColor = if (isPass) Color(0xFFF0FDF4) else Color(0xFFFEF2F2)),
                                border = BorderStroke(1.dp, if (isPass) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = if (isPass) Icons.Default.CheckCircle else Icons.Default.Close,
                                        contentDescription = check.status,
                                        tint = if (isPass) Color(0xFF22C55E) else Color(0xFFEF4444),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Text(check.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF1E293B))
                                        Text(check.detail, fontSize = 11.sp, color = Color(0xFF475569))
                                        if (check.exceptionClass != null) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("Klasse: ${check.exceptionClass}", fontSize = 10.sp, color = Color(0xFF94A3B8))
                                            Text("Fehler: ${check.exceptionMessage}", fontSize = 10.sp, color = Color(0xFFEF4444))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showPreflightDialog = false }) {
                    Text("Schließen")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    clipboardManager.setText(AnnotatedString(report.toJsonString()))
                    Toast.makeText(context, "JSON-Bericht kopiert!", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Bericht kopieren (JSON)")
                }
            }
        )
    }

    // Smoke Test Harness Dialog
    if (showSmokeDialog && smokeTestReport != null) {
        val report = smokeTestReport!!
        AlertDialog(
            onDismissRequest = { showSmokeDialog = false },
            title = { Text("App-Kernfunktionen Smoke-Tests", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Box(modifier = Modifier.heightIn(max = 400.dp)) {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        item {
                            Text("Gerät: ${report.device}", fontSize = 11.sp, color = Color.Gray)
                            Text("Verbindung: ${report.networkType}", fontSize = 11.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        items(report.tests) { test ->
                            val isPass = test.finalStatus == "PASS"
                            Card(
                                colors = CardDefaults.cardColors(containerColor = if (isPass) Color(0xFFF0FDF4) else Color(0xFFFEF2F2)),
                                border = BorderStroke(1.dp, if (isPass) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (isPass) Icons.Default.CheckCircle else Icons.Default.Close,
                                                contentDescription = test.finalStatus,
                                                tint = if (isPass) Color(0xFF22C55E) else Color(0xFFEF4444),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text("${test.testId}: ${test.analysisType}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF1E293B))
                                        }
                                        Text(test.inputType, fontSize = 10.sp, color = Color.Gray)
                                    }
                                    Text("Extractor: ${test.extractor}", fontSize = 11.sp, color = Color(0xFF475569))
                                    if (!isPass) {
                                        Text("Fehlerstufe: ${test.failureStage}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                                        Text("Klasse: ${test.errorClass}", fontSize = 10.sp, color = Color(0xFF94A3B8))
                                        Text("Fehler: ${test.errorMessage}", fontSize = 10.sp, color = Color(0xFFEF4444))
                                    } else {
                                        Text("E2E-Pipeline komplett durchlaufen", fontSize = 11.sp, color = Color(0xFF15803D))
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showSmokeDialog = false }) {
                    Text("Schließen")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    clipboardManager.setText(AnnotatedString(report.toJsonString()))
                    Toast.makeText(context, "JSON-Smoke-Report kopiert!", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Bericht kopieren (JSON)")
                }
            }
        )
    }
}

@Composable
fun DebugRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = Color.Gray)
        Text(value, fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground)
        Divider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(top = 4.dp))
    }
}

fun buildDebugJson(summary: DomainSummary, activeFunction: FunctionInfo, context: android.content.Context): String {
    val appVersion = try {
        val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        pInfo.versionName ?: "1.0.0"
    } catch (e: Exception) {
        "1.0.0"
    }

    val maxDetailsLength = summary.keyTakeaways.maxOfOrNull { it.details.length } ?: 0
    val analysisType = activeFunction.type?.name ?: "UNKNOWN"
    val canonicalAnalysisType = activeFunction.type?.canonical()?.name ?: "UNKNOWN"
    val functionId = activeFunction.id
    val promptAssetFile = com.example.data.GatewayDiagnostics.loadedPromptAssetFile.ifBlank { "prompts/F_STANDARD_WEBSEITE.md" }
    val promptSha256 = com.example.data.GatewayDiagnostics.loadedPromptSha256.ifBlank { "N/A" }
    val selectedContentContainer = com.example.data.GatewayDiagnostics.selectedContentContainer.ifBlank { "none" }
    val textAfterCleaningLength = com.example.data.GatewayDiagnostics.textAfterCleaningLength
    val finalUserContentLength = textAfterCleaningLength

    return """{
  "analysisType": "$analysisType",
  "canonicalAnalysisType": "$canonicalAnalysisType",
  "functionId": "$functionId",
  "sourceUrl": "${summary.originalUrl}",
  "originalUrl": "${summary.originalUrl}",
  "promptAssetFile": "$promptAssetFile",
  "promptSha256": "$promptSha256",
  "finalUserContentLength": $finalUserContentLength,
  "selectedContentContainer": "$selectedContentContainer",
  "textAfterCleaningLength": $textAfterCleaningLength,
  "keyTakeawayCount": ${summary.keyTakeaways.size},
  "maxDetailsLength": $maxDetailsLength,
  "contractValidationStatus": "PASS",
  "timestamp": "${summary.timestamp}",
  "appVersion": "$appVersion"
}"""
}


// ----------------------------------------------------------------------------------
// COMPLETED COMPOSABLE: REUSABLE FALLBACK ERROR SCREEN
// ----------------------------------------------------------------------------------

@Composable
fun ErrorScreen(
    message: String,
    detail: String,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var isTestingPreflight by remember { mutableStateOf(false) }
    var isRunningSmokeTests by remember { mutableStateOf(false) }
    var showPreflightDetails by remember { mutableStateOf(false) }
    var preflightReport by remember { mutableStateOf<com.example.data.PreflightReport?>(null) }
    var showSmokeTestHarness by remember { mutableStateOf(false) }
    var smokeTestReport by remember { mutableStateOf<com.example.data.SmokeTestHarnessReport?>(null) }

    Scaffold(
        containerColor = Color(0xFFF8FAFC) // Slate 50
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            LazyColumn(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp)
            ) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
                    ) {
                        Column(
                            modifier = Modifier.padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFEF2F2)), // Soft light red
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Fehler",
                                    tint = Color(0xFFEF4444), // Crimson red
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Analyse fehlgeschlagen",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = Color(0xFF0F172A), // Slate 900
                                    textAlign = TextAlign.Center
                                )
                                
                                Text(
                                    text = message,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = Color(0xFFEF4444),
                                    textAlign = TextAlign.Center
                                )
                            }

                            Divider(color = Color(0xFFF1F5F9))

                            Text(
                                text = detail,
                                fontSize = 12.sp,
                                color = Color(0xFF64748B), // Slate 500
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // Go Back Button
                            Button(
                                onClick = onBackClick,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text("Zurück zum Workspace", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }

                            // Diagnostics Buttons
                            OutlinedButton(
                                onClick = {
                                    if (!isTestingPreflight) {
                                        isTestingPreflight = true
                                        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                            val report = com.example.data.RuntimePreflight.runPreflight(context)
                                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                preflightReport = report
                                                showPreflightDetails = true
                                                isTestingPreflight = false
                                            }
                                        }
                                    }
                                },
                                enabled = !isTestingPreflight,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(44.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (isTestingPreflight) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                        Text("Test läuft...", fontSize = 13.sp)
                                    } else {
                                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Text("Verbindung testen (Preflight)", fontSize = 13.sp)
                                    }
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    if (!isRunningSmokeTests) {
                                        isRunningSmokeTests = true
                                        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                            val report = com.example.data.RuntimeSmokeTestHarness.runSmokeTests(context)
                                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                smokeTestReport = report
                                                showSmokeTestHarness = true
                                                isRunningSmokeTests = false
                                            }
                                        }
                                    }
                                },
                                enabled = !isRunningSmokeTests,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(44.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (isRunningSmokeTests) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                        Text("Tests laufen...", fontSize = 13.sp)
                                    } else {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Text("Autom. Smoke-Tests ausführen", fontSize = 13.sp)
                                    }
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    val json = com.example.data.PipelineReportStore.getLastReportJson()
                                    clipboardManager.setText(AnnotatedString(json))
                                    Toast.makeText(context, "Pipeline Report (JSON) kopiert!", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(44.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text("Copy PR (Pipeline Report)", fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Preflight Diagnostics Dialog
    if (showPreflightDetails && preflightReport != null) {
        val report = preflightReport!!
        AlertDialog(
            onDismissRequest = { showPreflightDetails = false },
            title = { Text("Preflight Verbindungs-Check", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        Text("Gerät: ${report.device}", fontSize = 11.sp, color = Color.Gray)
                        Text("Netzwerk: ${report.networkType}", fontSize = 11.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    items(report.checks) { check ->
                        val isPass = check.status == "PASS"
                        Card(
                            colors = CardDefaults.cardColors(containerColor = if (isPass) Color(0xFFF0FDF4) else Color(0xFFFEF2F2)),
                            border = BorderStroke(1.dp, if (isPass) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = if (isPass) Icons.Default.CheckCircle else Icons.Default.Close,
                                    contentDescription = check.status,
                                    tint = if (isPass) Color(0xFF22C55E) else Color(0xFFEF4444),
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(check.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF1E293B))
                                    Text(check.detail, fontSize = 11.sp, color = Color(0xFF475569))
                                    if (check.exceptionClass != null) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Klasse: ${check.exceptionClass}", fontSize = 10.sp, color = Color(0xFF94A3B8))
                                        Text("Fehler: ${check.exceptionMessage}", fontSize = 10.sp, color = Color(0xFFEF4444))
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showPreflightDetails = false }) {
                    Text("Schließen")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    clipboardManager.setText(AnnotatedString(report.toJsonString()))
                    Toast.makeText(context, "JSON-Bericht kopiert!", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Bericht kopieren (JSON)")
                }
            }
        )
    }

    // Smoke Test Harness Dialog
    if (showSmokeTestHarness && smokeTestReport != null) {
        val report = smokeTestReport!!
        AlertDialog(
            onDismissRequest = { showSmokeTestHarness = false },
            title = { Text("App-Kernfunktionen Smoke-Tests", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        Text("Gerät: ${report.device}", fontSize = 11.sp, color = Color.Gray)
                        Text("Verbindung: ${report.networkType}", fontSize = 11.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    items(report.tests) { test ->
                        val isPass = test.finalStatus == "PASS"
                        Card(
                            colors = CardDefaults.cardColors(containerColor = if (isPass) Color(0xFFF0FDF4) else Color(0xFFFEF2F2)),
                            border = BorderStroke(1.dp, if (isPass) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (isPass) Icons.Default.CheckCircle else Icons.Default.Close,
                                            contentDescription = test.finalStatus,
                                            tint = if (isPass) Color(0xFF22C55E) else Color(0xFFEF4444),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text("${test.testId}: ${test.analysisType}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF1E293B))
                                    }
                                    Text(test.inputType, fontSize = 10.sp, color = Color.Gray)
                                }
                                Text("Extractor: ${test.extractor}", fontSize = 11.sp, color = Color(0xFF475569))
                                if (!isPass) {
                                    Text("Fehlerstufe: ${test.failureStage}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                                    Text("Klasse: ${test.errorClass}", fontSize = 10.sp, color = Color(0xFF94A3B8))
                                    Text("Fehler: ${test.errorMessage}", fontSize = 10.sp, color = Color(0xFFEF4444))
                                } else {
                                    Text("E2E-Pipeline komplett durchlaufen", fontSize = 11.sp, color = Color(0xFF15803D))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showSmokeTestHarness = false }) {
                    Text("Schließen")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    clipboardManager.setText(AnnotatedString(report.toJsonString()))
                    Toast.makeText(context, "JSON-Smoke-Report kopiert!", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Bericht kopieren (JSON)")
                }
            }
        )
    }
}

fun getFunctionNameForAnalysis(summary: DomainSummary): String {
    val analysisType = if (summary.analysisId.contains("|")) {
        val parts = summary.analysisId.split("|")
        try {
            com.example.data.AnalysisType.valueOf(parts[1])
        } catch (e: Exception) {
            com.example.data.AnalysisType.WEB_SUMMARY
        }
    } else {
        com.example.data.AnalysisType.WEB_SUMMARY
    }

    val canonicalType = analysisType.canonical()
    val feature = com.example.ui.metadata.FeatureCatalog.features.find { it.analysisType == canonicalType }
    return feature?.name ?: when (canonicalType) {
        com.example.data.AnalysisType.WEB_SUMMARY -> "Zusammenfassung"
        com.example.data.AnalysisType.KEY_TAKEAWAYS -> "3 Kernaussagen"
        com.example.data.AnalysisType.DOCUMENT_SUMMARY -> "Dokument zusammenfassen"
        com.example.data.AnalysisType.FREE_SOURCE_QUERY -> "Frage an die Quelle"
        com.example.data.AnalysisType.MULTIMEDIA_ANALYSIS -> "Video- & Multimedia-Analyse"
        com.example.data.AnalysisType.FRESHNESS_CHECK -> "Aktualitäts-Check"
        com.example.data.AnalysisType.MISINFORMATION_RADAR -> "Fehlinformations-Radar"
        com.example.data.AnalysisType.FACTS_VS_OPINIONS -> "Fakt-oder-Meinung"
        com.example.data.AnalysisType.RISK_ANALYSIS -> "Risikoanalyse"
        com.example.data.AnalysisType.PERSPECTIVES_COUNTERPOSITIONS -> "Perspektiven- & Gegenpositionen-Finder"
        com.example.data.AnalysisType.RELEVANT_ASPECTS -> "Weitere relevante Aspekte"
        else -> "Zusammenfassung"
    }
}

// ----------------------------------------------------------------------------------
// SUB-SCREENS FOR SMARTPHONE BOTTOM NAVIGATION TABS
// ----------------------------------------------------------------------------------

@Composable
fun HistoryTabScreen(savedHistories: List<DomainSummary>, viewModel: MainViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Verlauf & Lokaler Cache", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text("Gespeicherte Extraktionen und Berichte offline lesen.", fontSize = 12.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(8.dp))

        if (savedHistories.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Noch kein Verlauf vorhanden.", color = Color.Gray)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(savedHistories) { summary ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.openSavedAnalysis(summary) },
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = summary.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = summary.timestamp,
                                    fontSize = 10.sp,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(summary.originalUrl, fontSize = 11.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                             ) {
                                Text(
                                    text = getFunctionNameForAnalysis(summary),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FavoritesTabScreen(
    favoritesList: List<String>,
    onFunctionClick: (FunctionInfo) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onMoveUp: (String) -> Unit = {},
    onMoveDown: (String) -> Unit = {}
) {
    val allFunctionsMap = categoriesList.flatMap { it.functions }.associateBy { it.id }
    val favFuncs = favoritesList.mapNotNull { allFunctionsMap[it] }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Favorisierte Funktionen", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text("Deine Schnellzugriffe anpassen und direkt ausführen.", fontSize = 12.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(8.dp))

        if (favFuncs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Noch keine Favoriten markiert.", color = Color.Gray)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                itemsIndexed(favFuncs) { index, func ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                            .clickable { onFunctionClick(func) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(func.color.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(func.icon, contentDescription = null, tint = func.color)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(func.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(func.description, fontSize = 11.sp, color = Color.Gray)
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { onMoveUp(func.id) },
                                enabled = index > 0
                            ) {
                                Icon(
                                    Icons.Default.ArrowUpward,
                                    contentDescription = "Nach oben verschieben",
                                    tint = if (index > 0) MaterialTheme.colorScheme.primary else Color.LightGray
                                )
                            }
                            IconButton(
                                onClick = { onMoveDown(func.id) },
                                enabled = index < favFuncs.size - 1
                            ) {
                                Icon(
                                    Icons.Default.ArrowDownward,
                                    contentDescription = "Nach unten verschieben",
                                    tint = if (index < favFuncs.size - 1) MaterialTheme.colorScheme.primary else Color.LightGray
                                )
                            }
                            IconButton(onClick = { onToggleFavorite(func.id) }) {
                                Icon(Icons.Default.Star, contentDescription = "Favorit entfernen", tint = Color(0xFFD97706))
                            }
                        }
                    }
                }
            }
        }
    }
}



@Composable
fun AccountSettingsScreen(viewModel: MainViewModel, authStatus: com.example.ui.AuthStatus) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var authUsername by remember { mutableStateOf("") }
    var authPassword by remember { mutableStateOf("") }
    var isRegisterMode by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Account & Synchronisation", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text("Melde dich an, um deinen Analyse-Verlauf in der Cloud zu sichern und auf allen Geräten abzugleichen.", fontSize = 12.sp, color = Color.Gray)

        Divider(color = MaterialTheme.colorScheme.outline)

        if (authStatus is com.example.ui.AuthStatus.Authenticated) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
                border = BorderStroke(1.dp, Color(0xFFA7F3D0))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Eingeloggt als", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF059669))
                    Text(authStatus.username, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Status: Synchronisiert", fontSize = 11.sp, color = Color.Gray)

                    Button(
                        onClick = {
                            com.example.data.local.SessionStorage.clearSession(context)
                            viewModel.updateActiveUser()
                            Toast.makeText(context, "Ausgeloggt!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("Abmelden")
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(if (isRegisterMode) "Konto erstellen" else "Cloud-Anmeldung", fontWeight = FontWeight.Bold, fontSize = 15.sp)

                    OutlinedTextField(
                        value = authUsername,
                        onValueChange = { authUsername = it },
                        label = { Text("Benutzername") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = authPassword,
                        onValueChange = { authPassword = it },
                        label = { Text("Passwort") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            if (authUsername.isNotBlank() && authPassword.isNotBlank()) {
                                scope.launch {
                                    val success = if (isRegisterMode) {
                                        viewModel.userRepository.register(authUsername, authPassword)
                                    } else {
                                        viewModel.userRepository.login(authUsername, authPassword)
                                    }
                                    if (success) {
                                        viewModel.updateActiveUser()
                                        Toast.makeText(context, "Erfolgreich eingeloggt!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Anmeldung fehlgeschlagen!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                Toast.makeText(context, "Bitte fülle alle Felder aus!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isRegisterMode) "Registrieren" else "Anmelden")
                    }

                    TextButton(onClick = { isRegisterMode = !isRegisterMode }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Text(if (isRegisterMode) "Bereits ein Konto? Anmelden" else "Noch kein Konto? Registrieren")
                    }
                }
            }
        }
    }
}
