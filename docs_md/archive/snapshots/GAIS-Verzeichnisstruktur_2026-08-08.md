# GAIS Verzeichnisstruktur & Datei-Inventar Relevantor

**Stand / Zeitstempel:** 2026-08-09 18:01:43 ICT  
**Projekt:** Relevantor (Android Kotlin / Jetpack Compose + Supabase Backend)  
**Erfasstes Hauptverzeichnis:** `/` (Kanonischer Workspace Root)  
**Gesamtzahl erfasster Dateien:** 379 Dateien  
**Status:** Topaktuell, Vollständig & Verifiziert  

---

## 1. Übersichts-Baumstruktur (ASCII Tree)

```text
/
├── .env.example
├── .github
│   └── workflows
│       └── build-apk.yml
├── .gitignore
├── AGENTS.md
├── ARCHITECTURE_FREEZE.md
├── GEMINI_429_TRUE_CAUSE_REPORT.md
├── app
│   ├── .gitignore
│   ├── GEMINI_429_TRUE_CAUSE_REPORT.md
│   ├── build.gradle.kts
│   ├── finalUserContent.txt
│   ├── proguard-rules.pro
│   ├── raw_b3_live_response.json
│   ├── raw_b3_live_response_v13.json
│   ├── raw_b3_live_response_v14.json
│   ├── raw_gemini_response.json
│   └── src
│       ├── androidTest
│       │   └── java
│       │       └── com
│       │           └── example
│       │               └── ExampleInstrumentedTest.kt
│       ├── main
│       │   ├── AndroidManifest.xml
│       │   ├── assets
│       │   │   ├── ARCHITECTURE_FREEZE.md
│       │   │   ├── GAIS-Arbeitsstandards-Prompt.md
│       │   │   ├── change-prompts
│       │   │   │   ├── CP-01_OPTIMIERUNG_FUNKTIONS_PROMPT.md
│       │   │   │   ├── CP-02_AENDERUNG_ARBEITSWEISE.md
│       │   │   │   ├── CP-03_NEUANLAGE_FUNKTION.md
│       │   │   │   ├── CP-07_UI_UX_AENDERUNG.md
│       │   │   │   ├── CP-08_ARCHITEKTURAENDERUNG_TEMPLATE.md
│       │   │   │   ├── CP_GUIDELINE.md
│       │   │   │   └── README_CHANGE_PROMPTS.md
│       │   │   └── prompts
│       │   │       ├── FUNCTION_SPEC_TEMPLATE.md
│       │   │       ├── F_AKTUALITAETS_CHECK.md
│       │   │       ├── F_BUSINESS_INKUBATOR.md
│       │   │       ├── F_DOKUMENTE.md
│       │   │       ├── F_FACTS_VS_OPINIONS_ANALYZER.md
│       │   │       ├── F_FEHLINFORMATIONS_RADAR.md
│       │   │       ├── F_FREIE_QUELLENANFRAGE.md
│       │   │       ├── F_GOOGLE_MAPS_ANALYZER.md
│       │   │       ├── F_GOOGLE_MAPS_LOCATION_CONTEXT.md
│       │   │       ├── F_GOOGLE_MAPS_LOCATION_QA.md
│       │   │       ├── F_MULTIMEDIA.md
│       │   │       ├── F_PERSPECTIVES_AND_COUNTERPOSITIONS.md
│       │   │       ├── F_PHOTO_SCREENSHOT_ANALYSIS.md
│       │   │       ├── F_RISIKO_ANALYSE.md
│       │   │       ├── F_STANDARD_WEBSEITE.md
│       │   │       ├── F_TOP_3_KERNAUSSAGEN.md
│       │   │       ├── F_WEITERE_RELEVANTE_ASPEKTE.md
│       │   │       ├── _global_quality_rules.md
│       │   │       ├── function_registry.json
│       │   │       └── prompt_manifest.json
│       │   ├── java
│       │   │   └── com
│       │   │       └── example
│       │   │           ├── LocalContentExtractionEngine.kt
│       │   │           ├── MainActivity.kt
│       │   │           ├── RelevantorAccessibilityService.kt
│       │   │           ├── data
│       │   │           │   ├── AnalysisRuntimeConfigs.kt
│       │   │           │   ├── AnalysisType.kt
│       │   │           │   ├── BackendFeatureConfig.kt
│       │   │           │   ├── FileProcessingHelper.kt
│       │   │           │   ├── FunctionRegistry.kt
│       │   │           │   ├── GatewayDiagnostics.kt
│       │   │           │   ├── GeminiModels.kt
│       │   │           │   ├── GeminiRepository.kt
│       │   │           │   ├── GoogleMapsDisambiguator.kt
│       │   │           │   ├── GoogleMapsUrlParser.kt
│       │   │           │   ├── PipelineReport.kt
│       │   │           │   ├── PipelineReportStore.kt
│       │   │           │   ├── PlacesApiService.kt
│       │   │           │   ├── PlacesDataMapper.kt
│       │   │           │   ├── PromptEngine.kt
│       │   │           │   ├── PromptFallbackProvider.kt
│       │   │           │   ├── PromptLoader.kt
│       │   │           │   ├── PublicVideoSourceResolver.kt
│       │   │           │   ├── ResponseNormalizer.kt
│       │   │           │   ├── RetrofitClient.kt
│       │   │           │   ├── RuntimePreflight.kt
│       │   │           │   ├── RuntimeSmokeTestHarness.kt
│       │   │           │   ├── RuntimeVerificationLayer.kt
│       │   │           │   ├── SummaryResponseParser.kt
│       │   │           │   ├── WebpageExtractor.kt
│       │   │           │   ├── YoutubeTranscriptHelper.kt
│       │   │           │   ├── YoutubeUrlDecoder.kt
│       │   │           │   ├── contextengine
│       │   │           │   │   ├── ContextEngine.kt
│       │   │           │   │   ├── ContextResult.kt
│       │   │           │   │   ├── ContextSource.kt
│       │   │           │   │   ├── ContextSourceType.kt
│       │   │           │   │   ├── GoogleMapsBaseContextSource.kt
│       │   │           │   │   ├── GoogleMapsLocationContextService.kt
│       │   │           │   │   ├── LocationContextDiagnosticRunner.kt
│       │   │           │   │   ├── LocationContextInput.kt
│       │   │           │   │   ├── WikipediaContextSource.kt
│       │   │           │   │   └── WikivoyageContextSource.kt
│       │   │           │   ├── diagnostics
│       │   │           │   │   ├── DiagnosticContributor.kt
│       │   │           │   │   ├── LocationContextDiagnosticContributor.kt
│       │   │           │   │   └── ReportSanitizer.kt
│       │   │           │   ├── engine
│       │   │           │   │   ├── AnalysisRegistryImpl.kt
│       │   │           │   │   ├── AndroidAssetPromptLoader.kt
│       │   │           │   │   ├── BaseGeminiEngine.kt
│       │   │           │   │   ├── EngineRunnerImpl.kt
│       │   │           │   │   ├── document
│       │   │           │   │   │   └── DocumentAnalysisEngine.kt
│       │   │           │   │   ├── location
│       │   │           │   │   │   └── LocationQuestionEngine.kt
│       │   │           │   │   ├── top3
│       │   │           │   │   │   └── Top3KeyPointsEngine.kt
│       │   │           │   │   └── web
│       │   │           │   │       └── WebpageAnalysisEngine.kt
│       │   │           │   ├── extraction
│       │   │           │   │   ├── DocumentInputExtractor.kt
│       │   │           │   │   ├── InputExtractor.kt
│       │   │           │   │   ├── InputExtractorRegistry.kt
│       │   │           │   │   ├── WebInputExtractor.kt
│       │   │           │   │   └── YoutubeInputExtractor.kt
│       │   │           │   ├── local
│       │   │           │   │   ├── RelevantorDatabase.kt
│       │   │           │   │   └── SessionStorage.kt
│       │   │           │   ├── remote
│       │   │           │   │   ├── BackendApiService.kt
│       │   │           │   │   ├── SupabaseApiService.kt
│       │   │           │   │   └── SupabaseSystemStatusChecker.kt
│       │   │           │   ├── repository
│       │   │           │   │   ├── AnalysisRepositoryImpl.kt
│       │   │           │   │   ├── ContentExtractionRepositoryImpl.kt
│       │   │           │   │   ├── SyncRepositoryImpl.kt
│       │   │           │   │   ├── UserRepositoryImpl.kt
│       │   │           │   │   └── YoutubeTranscriptProviderAdapter.kt
│       │   │           │   └── sync
│       │   │           │       ├── SyncScheduler.kt
│       │   │           │       └── SyncWorker.kt
│       │   │           ├── domain
│       │   │           │   ├── engine
│       │   │           │   │   ├── AnalysisEngine.kt
│       │   │           │   │   ├── AnalysisRegistry.kt
│       │   │           │   │   ├── ContractValidator.kt
│       │   │           │   │   ├── EngineRunner.kt
│       │   │           │   │   ├── PromptAssetLoader.kt
│       │   │           │   │   ├── location
│       │   │           │   │   │   ├── LocationQuestionCoordinator.kt
│       │   │           │   │   │   └── LocationQuestionPlanner.kt
│       │   │           │   │   └── validators
│       │   │           │   │       ├── A1ContractValidator.kt
│       │   │           │   │       └── A2ContractValidator.kt
│       │   │           │   ├── model
│       │   │           │   │   ├── AnalysisTrace.kt
│       │   │           │   │   ├── CanonicalAnalysisInput.kt
│       │   │           │   │   ├── ContentExtractionResult.kt
│       │   │           │   │   ├── DomainSummary.kt
│       │   │           │   │   ├── ExtractedContent.kt
│       │   │           │   │   ├── PublicVideoSource.kt
│       │   │           │   │   └── TranscriptProvider.kt
│       │   │           │   ├── repository
│       │   │           │   │   ├── AnalysisRepository.kt
│       │   │           │   │   ├── ContentExtractionRepository.kt
│       │   │           │   │   ├── GeminiGateway.kt
│       │   │           │   │   ├── SyncRepository.kt
│       │   │           │   │   └── UserRepository.kt
│       │   │           │   └── usecase
│       │   │           │       ├── AnalyzeContentUseCase.kt
│       │   │           │       ├── ExtractContentUseCase.kt
│       │   │           │       ├── LoadHistoryUseCase.kt
│       │   │           │       ├── SaveAnalysisUseCase.kt
│       │   │           │       └── SyncUserDataUseCase.kt
│       │   │           ├── share
│       │   │           │   └── DirectShareManager.kt
│       │   │           └── ui
│       │   │               ├── MainViewModel.kt
│       │   │               ├── components
│       │   │               │   └── TakeawayCard.kt
│       │   │               └── metadata
│       │   │                   ├── ExportFormatter.kt
│       │   │                   ├── FeatureCatalog.kt
│       │   │                   └── OutputPresentationPolicy.kt
│       │   └── res
│       │       ├── drawable
│       │       │   ├── ic_launcher_background.xml
│       │       │   └── ic_launcher_foreground.xml
│       │       ├── drawable-nodpi
│       │       │   ├── a_smartphone_app_ui_screenshot_portrait_with_a_w_1.png
│       │       │   └── relevantor_home_coffeehouse_background_v2.webp
│       │       ├── mipmap-anydpi
│       │       │   ├── ic_launcher.xml
│       │       │   └── ic_launcher_round.xml
│       │       ├── mipmap-anydpi-v26
│       │       │   ├── ic_launcher.xml
│       │       │   └── ic_launcher_round.xml
│       │       ├── values
│       │       │   ├── strings.xml
│       │       │   └── themes.xml
│       │       └── xml
│       │           ├── accessibility_service_config.xml
│       │           ├── backup_rules.xml
│       │           ├── data_extraction_rules.xml
│       │           └── shortcuts.xml
│       └── test
│           ├── Transkript_Youtube.txt
│           ├── assets
│           │   └── golden
│           │       ├── DOCUMENT_SUMMARY
│           │       │   ├── expected_domain_summary.json
│           │       │   ├── file_name.txt
│           │       │   ├── gemini_response.json
│           │       │   └── input_document.txt
│           │       ├── FACTS_VS_OPINIONS
│           │       │   ├── expected_domain_summary.json
│           │       │   ├── gemini_response.json
│           │       │   ├── input.html
│           │       │   └── input_url.txt
│           │       ├── FREE_SOURCE_QUERY
│           │       │   ├── expected_domain_summary.json
│           │       │   ├── gemini_response.json
│           │       │   ├── input_text.txt
│           │       │   └── input_url.txt
│           │       ├── FRESHNESS_CHECK
│           │       │   ├── expected_domain_summary.json
│           │       │   ├── gemini_response.json
│           │       │   ├── input.html
│           │       │   └── input_url.txt
│           │       ├── KEY_TAKEAWAYS
│           │       │   ├── expected_domain_summary.json
│           │       │   ├── gemini_response.json
│           │       │   ├── input.html
│           │       │   └── input_url.txt
│           │       ├── MISINFORMATION_RADAR
│           │       │   ├── expected_domain_summary.json
│           │       │   ├── gemini_response.json
│           │       │   ├── input.html
│           │       │   └── input_url.txt
│           │       ├── MULTIMEDIA_ANALYSIS
│           │       │   ├── expected_domain_summary.json
│           │       │   ├── gemini_response.json
│           │       │   ├── input_url.txt
│           │       │   └── transcript.txt
│           │       ├── PERSPECTIVES_COUNTERPOSITIONS
│           │       │   ├── expected_domain_summary.json
│           │       │   ├── gemini_response.json
│           │       │   ├── input.html
│           │       │   └── input_url.txt
│           │       ├── PHOTO_SCREENSHOT_ANALYSIS
│           │       │   ├── expected_domain_summary.json
│           │       │   ├── gemini_response.json
│           │       │   └── input_image_meta.txt
│           │       ├── RELEVANT_ASPECTS
│           │       │   ├── expected_domain_summary.json
│           │       │   ├── gemini_response.json
│           │       │   ├── input.html
│           │       │   └── input_url.txt
│           │       ├── RISK_ANALYSIS
│           │       │   ├── expected_domain_summary.json
│           │       │   ├── gemini_response.json
│           │       │   ├── input.html
│           │       │   └── input_url.txt
│           │       └── WEB_SUMMARY
│           │           ├── expected_domain_summary.json
│           │           ├── gemini_response.json
│           │           ├── input.html
│           │           └── input_url.txt
│           ├── java
│           │   └── com
│           │       └── example
│           │           ├── A1ContractValidatorTest.kt
│           │           ├── A2ContractValidatorTest.kt
│           │           ├── BaseArchitectureRegressionTest.kt
│           │           ├── BaseDiagnosticReportTest.kt
│           │           ├── BulkMigrationVerificationTest.kt
│           │           ├── ContentExtractionRegressionTest.kt
│           │           ├── ExampleRobolectricTest.kt
│           │           ├── ExampleUnitTest.kt
│           │           ├── FavoritesReorderingTest.kt
│           │           ├── GeminiMissingPartsTest.kt
│           │           ├── GoogleMapsBaseContextSourceTest.kt
│           │           ├── GoogleMapsDisambiguatorTest.kt
│           │           ├── GoogleMapsLocationContextServiceTest.kt
│           │           ├── GoogleMapsUrlParserTest.kt
│           │           ├── GreetingScreenshotTest.kt
│           │           ├── LiveTest.kt
│           │           ├── LocationContextDiagnosticTest.kt
│           │           ├── LocationQuestionCoordinatorTest.kt
│           │           ├── LocationQuestionEngineTest.kt
│           │           ├── LocationQuestionPlannerTest.kt
│           │           ├── MultimediaMetadataFallbackTest.kt
│           │           ├── MultimediaTranscriptReferenceTest.kt
│           │           ├── PipelineReportTest.kt
│           │           ├── PlacesApiServiceTest.kt
│           │           ├── RelevantorSelfTestHarnessTest.kt
│           │           ├── ReviewTest.kt
│           │           ├── TranscriptProviderTest.kt
│           │           ├── WikipediaContextSourceTest.kt
│           │           ├── WikivoyageContextSourceTest.kt
│           │           ├── contextpoc
│           │           │   ├── ContextCandidate.kt
│           │           │   ├── ContextCandidateMatcher.kt
│           │           │   ├── ContextMatchResult.kt
│           │           │   ├── ContextMatchingTest.kt
│           │           │   ├── ContextPlaceInput.kt
│           │           │   ├── ContextResolutionStatus.kt
│           │           │   ├── ContextResolver.kt
│           │           │   ├── ContextResolverIntegrationTest.kt
│           │           │   ├── ContextResolverResult.kt
│           │           │   ├── ContextSourceMetadata.kt
│           │           │   ├── ContextSourceType.kt
│           │           │   ├── ContextSuitabilityEvaluator.kt
│           │           │   ├── SimpleTest.kt
│           │           │   ├── WikipediaApiClient.kt
│           │           │   ├── WikipediaApiClientTest.kt
│           │           │   └── WikipediaMockFixtures.kt
│           │           ├── data
│           │           │   ├── PublicVideoSourceResolverTest.kt
│           │           │   └── SupabaseSystemStatusTest.kt
│           │           └── share
│           │               ├── DirectShareTest.kt
│           │               └── DirectShareViewModelTest.kt
│           └── screenshots
│               ├── greeting.png
│               └── gudrun_nolte_pdf_result.png
├── app-debug.apk
├── assets
│   ├── .aistudio
│   │   └── .gitignore
│   ├── Relevantor-App-Icon.png
│   ├── Relevantor-App-Icon_V2.png
│   └── Relevantor-App-Icon_V3.png
├── build.gradle.kts
├── build_metadata
│   ├── EXPORT_VERIFICATION.txt
│   └── metadata.json
├── debug.keystore
├── debug.keystore.base64
├── docs_md
│   ├── ABSTRACTOR_SYSTEM_STATE.md
│   ├── CP00_DOCS_MD_CLEANUP_DRY_RUN_2026-08-09.md
│   ├── CP00_DOCS_MD_CLEANUP_EXECUTION_2026-08-09.md
│   ├── CP08_MVP1B_SUPABASE_SCHEMA_FOUNDATION_2026-08-07_14-25-00_ICT.md
│   ├── CP08_MVP1C_FINAL_ACCEPTANCE_V2_2026-08-07_08-59-45_ICT.md
│   ├── GAIS-Architektur_2026-08-09.md
│   ├── GAIS-Verzeichnisstruktur_2026-08-09.md
│   ├── GAIS_GOVERNANCE_SMOKE_TEST_2026-08-09_17-20-40_ICT.md
│   ├── PROJECT_CONTEXT_RELEVANTOR.md
│   ├── README_INSTALL.txt
│   ├── RELEVANTOR_ARCHITECTURE.md
│   ├── RELEVANTOR_BASELINE_LOCAL_FIRST.md
│   ├── RELEVANTOR_DEVELOPMENT_STATUS.md
│   ├── RELEVANTOR_FUNCTION_EXECUTION_MODEL.md
│   ├── RELEVANTOR_GAIS_WORKSPACE_RULES.md
│   ├── RELEVANTOR_OUTPUT_SPEC.md
│   ├── RELEVANTOR_SELF_TEST_MATRIX.md
│   ├── TEST_COVERAGE_MATRIX.md
│   └── archive
│       ├── CP00_GAIS_ERROR_FORENSIC_AUDIT_2026-08-03_04-00-00_UTC.md
│       ├── CP00_GAIS_PATH_GOVERNANCE_ALIGNMENT_2026-08-08_18-53-33_ICT.md
│       ├── CP00_GHOST_DIRECTORY_AUDIT_2026-08-03_03-48-00_UTC.md
│       ├── CP00_GHOST_DIRECTORY_CLEANUP_2026-08-03_03-52-00_UTC.md
│       ├── CP00_GHOST_DOCUMENTATION_CLEANUP_2026-08-08_18-38-12_ICT.md
│       ├── CP00_GHOST_PATH_RECOVERY_MVP1C_2026-08-07_08-49-42_ICT.md
│       ├── CP00_GITHUB_CHECKPOINT_PREPARATION_2026-08-08_16-21-31_ICT.md
│       ├── CP00_GITHUB_CHECKPOINT_READY_2026-08-08_18-56-14_ICT.md
│       ├── CP00_GIT_RECOVERY_STRATEGY_2026-08-08_16-03-39_ICT.md
│       ├── CP00_GIT_RECOVERY_STRATEGY_2026-08-08_16-17-13_ICT.md
│       ├── CP00_ICON_FORENSIC_ANALYSIS_2026-08-03_04-30-00_UTC.md
│       ├── CP00_ICON_RESTORE_2026-08-03_04-35-00_UTC.md
│       ├── CP00_MVP1C_STABLE_CHECKPOINT_2026-08-08_15-53-54_ICT.md
│       ├── CP00_POST_ICON_RESTORE_VALIDATION_2026-08-03_04-39-16_UTC.md
│       ├── CP00_POST_PHASE6_INTEGRITY_AUDIT_2026-08-03_03-30-00_UTC.md
│       ├── CP00_REPOSITORY_INTEGRITY_AUDIT_2026-08-02_14-55.md
│       ├── CP00_RUNTIME_REGRESSION_AUDIT_2026-08-03_04-25-00_UTC.md
│       ├── CP00_WORKSPACE_DUPLICATE_ROOT_CAUSE_ANALYSIS_2026-08-08_18-31-53_ICT.md
│       ├── CP00_WORKSPACE_PATH_ANALYSIS_2026-08-08_16-11-41_ICT.md
│       ├── CP00_WORKSPACE_PATH_VERIFICATION_2026-08-08_16-34-20_ICT.md
│       ├── CP03_GOOGLE_MAPS_LOCATION_QUERY_DRY_RUN_FINAL_2026-08-02_15-16-24_UTC.md
│       ├── CP03_GOOGLE_MAPS_LOCATION_QUERY_DRY_RUN_FINAL_2026-08-02_15-26-03_UTC.md
│       ├── CP03_LOCATION_QUESTION_PHASE1_PLANNER_2026-08-02_15-32-14_UTC.md
│       ├── CP03_PHASE2_REGISTRATION_2026-08-03_02-28-20_UTC.md
│       ├── CP03_PHASE3_PRECHECK_2026-08-03_02-33-15_UTC.md
│       ├── CP03_PHASE4_COORDINATOR_2026-08-03_02-48-00_UTC.md
│       ├── CP03_PHASE5_CHANGE_AUDIT_2026-08-03_03-15-00_UTC.md
│       ├── CP03_PHASE5_ENGINE_2026-08-03_03-05-00_UTC.md
│       ├── CP03_PHASE6_E2E_TEST_2026-08-03_03-25-00_UTC.md
│       ├── CP08_BACKEND_ARCHITEKTUR_REVIEW_2026-08-06_05-35-00_UTC.md
│       ├── CP08_MVP1C_APP_SUPABASE_DB_PROOF_DRY_RUN_2026-08-07_14-42-00_ICT.md
│       ├── CP08_MVP1C_APP_SUPABASE_DB_PROOF_DRY_RUN_V2_2026-08-07_14-50-00_ICT.md
│       ├── CP08_MVP1C_APP_SUPABASE_DB_PROOF_IMPLEMENTATION_2026-08-07_14-58-00_ICT.md
│       ├── CP08_MVP1C_BACKEND_VERSION_MAPPING_FIX_2026-08-07_15-41-00_ICT.md
│       ├── CP08_MVP1_BACKEND_FOUNDATION_DRY_RUN_2026-08-07_13-46-25_ICT.md
│       ├── CP08_MVP1_GAIS_SUPABASE_CONNECTIVITY_2026-08-07_13-52-00_ICT.md
│       ├── GAIS-Architektur_2026-08-02.md
│       ├── GAIS-Architektur_2026-08-07.md
│       ├── GAIS-Verzeichnisstruktur_2026-08-02.md
│       ├── GAIS-Verzeichnisstruktur_2026-08-03.md
│       ├── GAIS-Verzeichnisstruktur_2026-08-05.md
│       ├── GAIS-Verzeichnisstruktur_2026-08-07.md
│       ├── GAIS-Verzeichnisstruktur_2026-08-08.md
│       ├── GAIS_GOVERNANCE_SMOKE_TEST_2026-08-08_15-44-05_ICT.md
│       ├── GEMINI_429_TRUE_CAUSE_REPORT.md
│       ├── GITHUB_BACKUP_CP03_PHASE5_2026-08-03_03-18-00_UTC.md
│       ├── GITHUB_BASELINE_CP03_V13_2026-08-03_04-12-00_UTC.md
│       ├── GITHUB_CHECKPOINT_CP03_FINAL_2026-08-03_03-55-00_UTC.md
│       ├── GITHUB_FINAL_STAGING_AUDIT_CP03_2026-08-03_03-41-00_UTC.md
│       ├── GITHUB_ICON_RESTORE_CHECKPOINT_2026-08-03_04-41-46_UTC.md
│       ├── GOOGLE_AI_STUDIO_RESPONSE_ARCHITECTURAL_ANALYSIS.md
│       ├── GOOGLE_AI_STUDIO_RESPONSE_ARCHITECTURE_CORRECTION.md
│       ├── GOOGLE_AI_STUDIO_RESPONSE_OUTPUT_QUALITY_VERIFICATION.md
│       ├── GOOGLE_AI_STUDIO_RESPONSE_VERIFIED_CAPABILITIES.md
│       ├── GOOGLE_AI_STUDIO_RESPONSE_VERIFY_AGENT.md
│       ├── LOCAL_BUILD_HANDOFF.md
│       ├── NEW_CHAT_BOOTSTRAP_ANALYSIS_2026-08-03_08-23-30_UTC.md
│       ├── RELEVANTOR_CONTEXT_RECOVERY_2026-08-03_07-52-56_UTC.md
│       ├── RELEVANTOR_IMPLEMENTATION_CONTEXT_EXPORT.md
│       ├── RELEVANTOR_LAUNCHER_ICON_DIAGNOSIS_2026-08-02.md
│       ├── RELEVANTOR_PROJECT_CONTEXT_EXPORT_2026-08-02.md
│       ├── RELEVANTOR_RESOURCE_FORENSIC_AUDIT_2026-08-02.md
│       ├── RELEVANTOR_SYSTEM_STATE_2026-07-01_01-01-32.md
│       ├── RELEVANTOR_VERIFIED_PROJECT_BASE_2026-08-03_08-01-26_UTC.md
│       ├── RELEVANTOR_VERIFIED_PROJECT_BASE_CORRECTED_2026-08-03_08-07-20_UTC.md
│       ├── RELEVANTOR_VERIFIED_WORKSPACE_STATE_2026-08-02.md
│       ├── RELEVANTOR_WORKSPACE_MASTER_V1_3_2026-08-03_04-10-00_UTC.md
│       ├── WORKSPACE_INVENTORY_2026-08-03_08-54-42_UTC.md
│       ├── ZUSAMMENFASSUNG_ARBEITEN.md
│       ├── b_relevantor_home_coffeehouse_background.png
│       ├── incident_reviews
│       │   └── GAIS_SELF_CRITICAL_INCIDENT_REVIEW_2026-08-03_02-34-11_UTC.md
│       ├── raw_b3_live_response.json
│       ├── raw_b3_live_response_v13.json
│       ├── raw_b3_live_response_v14.json
│       ├── relevantor_home_coffeehouse.png
│       ├── relevantor_home_coffeehouse_background.png
│       ├── relevantor_home_coffeehouse_objective.png
│       └── verzeichnisstruktur-und-dateien.md
├── fix_test.sh
├── gradle
│   └── libs.versions.toml
├── gradle.properties
├── metadata.json
├── settings.gradle.kts
├── supabase
│   ├── config.toml
│   └── migrations
│       └── 20260807000000_mvp1_system_status.sql
└── tools
    ├── build_structure_doc.py
    ├── git_post_ui_push_health_gate.sh
    ├── patch_disambiguator.sh
    ├── patch_placesapi.sh
    ├── patch_tests.sh
    ├── patch_trace.sh
    └── report_generator.py
```

---

## 2. Vollständiges Datei-Inventar (Tabelle aller Dateien)

| Dateipfad & Dateiname | Dateiendung | Größe (Bytes) | Speicherdatum & Uhrzeit (ICT) | Kategorie / Modul |
|---|---|---|---|---|
| `.env.example` | `.example` | 457 | 2026-08-09 16:54:54 ICT | Projekt Root Asset / Konfiguration |
| `.github/workflows/build-apk.yml` | `.yml` | 3,303 | 2026-08-09 16:54:53 ICT | Projekt Root Asset / Konfiguration |
| `.gitignore` | `(keine)` | 543 | 2026-08-09 16:54:54 ICT | Projekt Root Asset / Konfiguration |
| `AGENTS.md` | `.md` | 5,225 | 2026-08-09 16:54:54 ICT | Projekt Root Asset / Konfiguration |
| `ARCHITECTURE_FREEZE.md` | `.md` | 8,370 | 2026-08-09 16:54:54 ICT | Projekt Root Asset / Konfiguration |
| `GEMINI_429_TRUE_CAUSE_REPORT.md` | `.md` | 4,730 | 2026-08-09 16:54:54 ICT | Projekt Root Asset / Konfiguration |
| `app-debug.apk` | `.apk` | 21,903,669 | 2026-08-09 16:58:50 ICT | Projekt Root Asset / Konfiguration |
| `app/.gitignore` | `(keine)` | 63 | 2026-08-09 16:54:54 ICT | Projekt Root Asset / Konfiguration |
| `app/GEMINI_429_TRUE_CAUSE_REPORT.md` | `.md` | 4,730 | 2026-08-09 16:54:54 ICT | Projekt Root Asset / Konfiguration |
| `app/build.gradle.kts` | `.kts` | 5,500 | 2026-08-09 16:54:54 ICT | Gradle Build Configuration |
| `app/finalUserContent.txt` | `.txt` | 4,166 | 2026-08-09 16:54:54 ICT | Projekt Root Asset / Konfiguration |
| `app/proguard-rules.pro` | `.pro` | 751 | 2026-08-09 16:54:54 ICT | Projekt Root Asset / Konfiguration |
| `app/raw_b3_live_response.json` | `.json` | 3,223 | 2026-08-09 16:54:53 ICT | Projekt Root Asset / Konfiguration |
| `app/raw_b3_live_response_v13.json` | `.json` | 2,867 | 2026-08-09 16:54:54 ICT | Projekt Root Asset / Konfiguration |
| `app/raw_b3_live_response_v14.json` | `.json` | 1,638 | 2026-08-09 16:54:54 ICT | Projekt Root Asset / Konfiguration |
| `app/raw_gemini_response.json` | `.json` | 674 | 2026-08-09 16:54:54 ICT | Projekt Root Asset / Konfiguration |
| `app/src/androidTest/java/com/example/ExampleInstrumentedTest.kt` | `.kt` | 630 | 2026-08-09 16:54:54 ICT | Projekt Root Asset / Konfiguration |
| `app/src/main/AndroidManifest.xml` | `.xml` | 2,532 | 2026-08-09 16:54:54 ICT | Projekt Root Asset / Konfiguration |
| `app/src/main/assets/ARCHITECTURE_FREEZE.md` | `.md` | 14,889 | 2026-08-09 16:54:54 ICT | Projekt Root Asset / Konfiguration |
| `app/src/main/assets/GAIS-Arbeitsstandards-Prompt.md` | `.md` | 7,561 | 2026-08-09 16:54:54 ICT | Projekt Root Asset / Konfiguration |
| `app/src/main/assets/change-prompts/CP-01_OPTIMIERUNG_FUNKTIONS_PROMPT.md` | `.md` | 18,991 | 2026-08-09 16:54:54 ICT | GAIS Change Prompts (CP-01 - CP-08) |
| `app/src/main/assets/change-prompts/CP-02_AENDERUNG_ARBEITSWEISE.md` | `.md` | 6,418 | 2026-08-09 16:54:54 ICT | GAIS Change Prompts (CP-01 - CP-08) |
| `app/src/main/assets/change-prompts/CP-03_NEUANLAGE_FUNKTION.md` | `.md` | 7,269 | 2026-08-09 16:54:54 ICT | GAIS Change Prompts (CP-01 - CP-08) |
| `app/src/main/assets/change-prompts/CP-07_UI_UX_AENDERUNG.md` | `.md` | 6,163 | 2026-08-09 16:54:54 ICT | GAIS Change Prompts (CP-01 - CP-08) |
| `app/src/main/assets/change-prompts/CP-08_ARCHITEKTURAENDERUNG_TEMPLATE.md` | `.md` | 2,740 | 2026-08-09 16:54:54 ICT | GAIS Change Prompts (CP-01 - CP-08) |
| `app/src/main/assets/change-prompts/CP_GUIDELINE.md` | `.md` | 6,729 | 2026-08-09 16:54:54 ICT | GAIS Change Prompts (CP-01 - CP-08) |
| `app/src/main/assets/change-prompts/README_CHANGE_PROMPTS.md` | `.md` | 5,872 | 2026-08-09 16:54:54 ICT | GAIS Change Prompts (CP-01 - CP-08) |
| `app/src/main/assets/prompts/FUNCTION_SPEC_TEMPLATE.md` | `.md` | 17,267 | 2026-08-09 16:54:54 ICT | System Prompts & Prompt Registries |
| `app/src/main/assets/prompts/F_AKTUALITAETS_CHECK.md` | `.md` | 10,628 | 2026-08-09 16:54:54 ICT | System Prompts & Prompt Registries |
| `app/src/main/assets/prompts/F_BUSINESS_INKUBATOR.md` | `.md` | 11,865 | 2026-08-09 16:54:54 ICT | System Prompts & Prompt Registries |
| `app/src/main/assets/prompts/F_DOKUMENTE.md` | `.md` | 12,415 | 2026-08-09 16:54:54 ICT | System Prompts & Prompt Registries |
| `app/src/main/assets/prompts/F_FACTS_VS_OPINIONS_ANALYZER.md` | `.md` | 16,378 | 2026-08-09 16:54:54 ICT | System Prompts & Prompt Registries |
| `app/src/main/assets/prompts/F_FEHLINFORMATIONS_RADAR.md` | `.md` | 3,001 | 2026-08-09 16:54:54 ICT | System Prompts & Prompt Registries |
| `app/src/main/assets/prompts/F_FREIE_QUELLENANFRAGE.md` | `.md` | 1,568 | 2026-08-09 16:54:54 ICT | System Prompts & Prompt Registries |
| `app/src/main/assets/prompts/F_GOOGLE_MAPS_ANALYZER.md` | `.md` | 8,638 | 2026-08-09 16:54:54 ICT | System Prompts & Prompt Registries |
| `app/src/main/assets/prompts/F_GOOGLE_MAPS_LOCATION_CONTEXT.md` | `.md` | 1,927 | 2026-08-09 16:54:54 ICT | System Prompts & Prompt Registries |
| `app/src/main/assets/prompts/F_GOOGLE_MAPS_LOCATION_QA.md` | `.md` | 2,833 | 2026-08-09 16:54:54 ICT | System Prompts & Prompt Registries |
| `app/src/main/assets/prompts/F_MULTIMEDIA.md` | `.md` | 6,785 | 2026-08-09 16:54:54 ICT | System Prompts & Prompt Registries |
| `app/src/main/assets/prompts/F_PERSPECTIVES_AND_COUNTERPOSITIONS.md` | `.md` | 7,317 | 2026-08-09 16:54:53 ICT | System Prompts & Prompt Registries |
| `app/src/main/assets/prompts/F_PHOTO_SCREENSHOT_ANALYSIS.md` | `.md` | 2,408 | 2026-08-09 16:54:54 ICT | System Prompts & Prompt Registries |
| `app/src/main/assets/prompts/F_RISIKO_ANALYSE.md` | `.md` | 8,211 | 2026-08-09 16:54:54 ICT | System Prompts & Prompt Registries |
| `app/src/main/assets/prompts/F_STANDARD_WEBSEITE.md` | `.md` | 13,041 | 2026-08-09 16:54:54 ICT | System Prompts & Prompt Registries |
| `app/src/main/assets/prompts/F_TOP_3_KERNAUSSAGEN.md` | `.md` | 2,932 | 2026-08-09 16:54:54 ICT | System Prompts & Prompt Registries |
| `app/src/main/assets/prompts/F_WEITERE_RELEVANTE_ASPEKTE.md` | `.md` | 14,138 | 2026-08-09 16:54:54 ICT | System Prompts & Prompt Registries |
| `app/src/main/assets/prompts/_global_quality_rules.md` | `.md` | 5,319 | 2026-08-09 16:54:53 ICT | System Prompts & Prompt Registries |
| `app/src/main/assets/prompts/function_registry.json` | `.json` | 3,168 | 2026-08-09 16:54:54 ICT | System Prompts & Prompt Registries |
| `app/src/main/assets/prompts/prompt_manifest.json` | `.json` | 1,342 | 2026-08-09 16:54:54 ICT | System Prompts & Prompt Registries |
| `app/src/main/java/com/example/LocalContentExtractionEngine.kt` | `.kt` | 1,091 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/MainActivity.kt` | `.kt` | 157,437 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/RelevantorAccessibilityService.kt` | `.kt` | 413 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/data/AnalysisRuntimeConfigs.kt` | `.kt` | 997 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/data/AnalysisType.kt` | `.kt` | 2,030 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/data/BackendFeatureConfig.kt` | `.kt` | 168 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/data/FileProcessingHelper.kt` | `.kt` | 17,430 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/data/FunctionRegistry.kt` | `.kt` | 1,893 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/data/GatewayDiagnostics.kt` | `.kt` | 10,290 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/data/GeminiModels.kt` | `.kt` | 4,701 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/data/GeminiRepository.kt` | `.kt` | 1,629 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/data/GoogleMapsDisambiguator.kt` | `.kt` | 5,390 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/data/GoogleMapsUrlParser.kt` | `.kt` | 14,020 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/data/PipelineReport.kt` | `.kt` | 13,711 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/data/PipelineReportStore.kt` | `.kt` | 52,656 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/data/PlacesApiService.kt` | `.kt` | 20,861 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/data/PlacesDataMapper.kt` | `.kt` | 2,833 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/data/PromptEngine.kt` | `.kt` | 782 | 2026-08-09 16:54:53 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/data/PromptFallbackProvider.kt` | `.kt` | 380 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/data/PromptLoader.kt` | `.kt` | 18,992 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/data/PublicVideoSourceResolver.kt` | `.kt` | 1,586 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/data/ResponseNormalizer.kt` | `.kt` | 1,853 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/data/RetrofitClient.kt` | `.kt` | 1,015 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/data/RuntimePreflight.kt` | `.kt` | 10,077 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/data/RuntimeSmokeTestHarness.kt` | `.kt` | 39,945 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/data/RuntimeVerificationLayer.kt` | `.kt` | 6,291 | 2026-08-09 16:54:53 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/data/SummaryResponseParser.kt` | `.kt` | 51,636 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/data/WebpageExtractor.kt` | `.kt` | 31,731 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/data/YoutubeTranscriptHelper.kt` | `.kt` | 22,373 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/data/YoutubeUrlDecoder.kt` | `.kt` | 972 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/data/contextengine/ContextEngine.kt` | `.kt` | 2,790 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/data/contextengine/ContextResult.kt` | `.kt` | 487 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/data/contextengine/ContextSource.kt` | `.kt` | 206 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/data/contextengine/ContextSourceType.kt` | `.kt` | 176 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/data/contextengine/GoogleMapsBaseContextSource.kt` | `.kt` | 2,820 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/data/contextengine/GoogleMapsLocationContextService.kt` | `.kt` | 3,778 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/data/contextengine/LocationContextDiagnosticRunner.kt` | `.kt` | 10,163 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/data/contextengine/LocationContextInput.kt` | `.kt` | 306 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/data/contextengine/WikipediaContextSource.kt` | `.kt` | 15,191 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/data/contextengine/WikivoyageContextSource.kt` | `.kt` | 15,301 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/data/diagnostics/DiagnosticContributor.kt` | `.kt` | 257 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/data/diagnostics/LocationContextDiagnosticContributor.kt` | `.kt` | 3,217 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/data/diagnostics/ReportSanitizer.kt` | `.kt` | 16,596 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/data/engine/AnalysisRegistryImpl.kt` | `.kt` | 6,212 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/data/engine/AndroidAssetPromptLoader.kt` | `.kt` | 381 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/data/engine/BaseGeminiEngine.kt` | `.kt` | 41,173 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/data/engine/EngineRunnerImpl.kt` | `.kt` | 755 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/data/engine/document/DocumentAnalysisEngine.kt` | `.kt` | 4,219 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/data/engine/location/LocationQuestionEngine.kt` | `.kt` | 2,599 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/data/engine/top3/Top3KeyPointsEngine.kt` | `.kt` | 1,478 | 2026-08-09 16:54:53 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/data/engine/web/WebpageAnalysisEngine.kt` | `.kt` | 1,581 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/data/extraction/DocumentInputExtractor.kt` | `.kt` | 8,477 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/data/extraction/InputExtractor.kt` | `.kt` | 550 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/data/extraction/InputExtractorRegistry.kt` | `.kt` | 612 | 2026-08-09 16:54:53 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/data/extraction/WebInputExtractor.kt` | `.kt` | 16,274 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/data/extraction/YoutubeInputExtractor.kt` | `.kt` | 5,627 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/data/local/RelevantorDatabase.kt` | `.kt` | 5,849 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/data/local/SessionStorage.kt` | `.kt` | 1,812 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/data/remote/BackendApiService.kt` | `.kt` | 2,602 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/data/remote/SupabaseApiService.kt` | `.kt` | 2,108 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/data/remote/SupabaseSystemStatusChecker.kt` | `.kt` | 2,219 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/data/repository/AnalysisRepositoryImpl.kt` | `.kt` | 1,946 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/data/repository/ContentExtractionRepositoryImpl.kt` | `.kt` | 6,769 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/data/repository/SyncRepositoryImpl.kt` | `.kt` | 1,330 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/data/repository/UserRepositoryImpl.kt` | `.kt` | 1,683 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/data/repository/YoutubeTranscriptProviderAdapter.kt` | `.kt` | 1,276 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/data/sync/SyncScheduler.kt` | `.kt` | 1,420 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/data/sync/SyncWorker.kt` | `.kt` | 369 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/domain/engine/AnalysisEngine.kt` | `.kt` | 269 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/domain/engine/AnalysisRegistry.kt` | `.kt` | 4,451 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/domain/engine/ContractValidator.kt` | `.kt` | 154 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/domain/engine/EngineRunner.kt` | `.kt` | 260 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/domain/engine/PromptAssetLoader.kt` | `.kt` | 107 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/domain/engine/location/LocationQuestionCoordinator.kt` | `.kt` | 12,358 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/domain/engine/location/LocationQuestionPlanner.kt` | `.kt` | 5,370 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/domain/engine/validators/A1ContractValidator.kt` | `.kt` | 2,322 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/domain/engine/validators/A2ContractValidator.kt` | `.kt` | 3,369 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/domain/model/AnalysisTrace.kt` | `.kt` | 2,544 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/domain/model/CanonicalAnalysisInput.kt` | `.kt` | 600 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/domain/model/ContentExtractionResult.kt` | `.kt` | 672 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/domain/model/DomainSummary.kt` | `.kt` | 726 | 2026-08-09 16:54:53 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/domain/model/ExtractedContent.kt` | `.kt` | 248 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/domain/model/PublicVideoSource.kt` | `.kt` | 358 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/domain/model/TranscriptProvider.kt` | `.kt` | 430 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/domain/repository/AnalysisRepository.kt` | `.kt` | 409 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/domain/repository/ContentExtractionRepository.kt` | `.kt` | 385 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/domain/repository/GeminiGateway.kt` | `.kt` | 268 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/domain/repository/SyncRepository.kt` | `.kt` | 94 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/domain/repository/UserRepository.kt` | `.kt` | 292 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/domain/usecase/AnalyzeContentUseCase.kt` | `.kt` | 6,201 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/domain/usecase/ExtractContentUseCase.kt` | `.kt` | 598 | 2026-08-09 16:54:53 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/domain/usecase/LoadHistoryUseCase.kt` | `.kt` | 369 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/domain/usecase/SaveAnalysisUseCase.kt` | `.kt` | 316 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/domain/usecase/SyncUserDataUseCase.kt` | `.kt` | 301 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/share/DirectShareManager.kt` | `.kt` | 6,272 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/ui/MainViewModel.kt` | `.kt` | 61,150 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/ui/components/TakeawayCard.kt` | `.kt` | 5,014 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/ui/metadata/ExportFormatter.kt` | `.kt` | 3,633 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/ui/metadata/FeatureCatalog.kt` | `.kt` | 7,805 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/java/com/example/ui/metadata/OutputPresentationPolicy.kt` | `.kt` | 1,955 | 2026-08-09 16:54:54 ICT | Android App Source Code (Kotlin) |
| `app/src/main/res/drawable-nodpi/a_smartphone_app_ui_screenshot_portrait_with_a_w_1.png` | `.png` | 2,420,445 | 2026-08-09 16:54:54 ICT | Projekt Root Asset / Konfiguration |
| `app/src/main/res/drawable-nodpi/relevantor_home_coffeehouse_background_v2.webp` | `.webp` | 128,538 | 2026-08-09 16:54:54 ICT | Projekt Root Asset / Konfiguration |
| `app/src/main/res/drawable/ic_launcher_background.xml` | `.xml` | 847 | 2026-08-09 16:54:54 ICT | Projekt Root Asset / Konfiguration |
| `app/src/main/res/drawable/ic_launcher_foreground.xml` | `.xml` | 3,713 | 2026-08-09 16:54:54 ICT | Projekt Root Asset / Konfiguration |
| `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` | `.xml` | 273 | 2026-08-09 16:54:54 ICT | Projekt Root Asset / Konfiguration |
| `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml` | `.xml` | 273 | 2026-08-09 16:54:54 ICT | Projekt Root Asset / Konfiguration |
| `app/src/main/res/mipmap-anydpi/ic_launcher.xml` | `.xml` | 255 | 2026-08-09 16:54:54 ICT | Projekt Root Asset / Konfiguration |
| `app/src/main/res/mipmap-anydpi/ic_launcher_round.xml` | `.xml` | 255 | 2026-08-09 16:54:54 ICT | Projekt Root Asset / Konfiguration |
| `app/src/main/res/values/strings.xml` | `.xml` | 112 | 2026-08-09 16:54:54 ICT | Projekt Root Asset / Konfiguration |
| `app/src/main/res/values/themes.xml` | `.xml` | 352 | 2026-08-09 16:54:54 ICT | Projekt Root Asset / Konfiguration |
| `app/src/main/res/xml/accessibility_service_config.xml` | `.xml` | 503 | 2026-08-09 16:54:54 ICT | Projekt Root Asset / Konfiguration |
| `app/src/main/res/xml/backup_rules.xml` | `.xml` | 170 | 2026-08-09 16:54:54 ICT | Projekt Root Asset / Konfiguration |
| `app/src/main/res/xml/data_extraction_rules.xml` | `.xml` | 360 | 2026-08-09 16:54:54 ICT | Projekt Root Asset / Konfiguration |
| `app/src/main/res/xml/shortcuts.xml` | `.xml` | 337 | 2026-08-09 16:54:54 ICT | Projekt Root Asset / Konfiguration |
| `app/src/test/Transkript_Youtube.txt` | `.txt` | 130,824 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/assets/golden/DOCUMENT_SUMMARY/expected_domain_summary.json` | `.json` | 542 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/assets/golden/DOCUMENT_SUMMARY/file_name.txt` | `.txt` | 19 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/assets/golden/DOCUMENT_SUMMARY/gemini_response.json` | `.json` | 545 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/assets/golden/DOCUMENT_SUMMARY/input_document.txt` | `.txt` | 195 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/assets/golden/FACTS_VS_OPINIONS/expected_domain_summary.json` | `.json` | 844 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/assets/golden/FACTS_VS_OPINIONS/gemini_response.json` | `.json` | 847 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/assets/golden/FACTS_VS_OPINIONS/input.html` | `.html` | 264 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/assets/golden/FACTS_VS_OPINIONS/input_url.txt` | `.txt` | 38 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/assets/golden/FREE_SOURCE_QUERY/expected_domain_summary.json` | `.json` | 654 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/assets/golden/FREE_SOURCE_QUERY/gemini_response.json` | `.json` | 657 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/assets/golden/FREE_SOURCE_QUERY/input_text.txt` | `.txt` | 49 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/assets/golden/FREE_SOURCE_QUERY/input_url.txt` | `.txt` | 25 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/assets/golden/FRESHNESS_CHECK/expected_domain_summary.json` | `.json` | 591 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/assets/golden/FRESHNESS_CHECK/gemini_response.json` | `.json` | 594 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/assets/golden/FRESHNESS_CHECK/input.html` | `.html` | 301 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/assets/golden/FRESHNESS_CHECK/input_url.txt` | `.txt` | 34 | 2026-08-09 16:54:53 ICT | Android Unit / Robolectric Tests |
| `app/src/test/assets/golden/KEY_TAKEAWAYS/expected_domain_summary.json` | `.json` | 987 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/assets/golden/KEY_TAKEAWAYS/gemini_response.json` | `.json` | 990 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/assets/golden/KEY_TAKEAWAYS/input.html` | `.html` | 916 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/assets/golden/KEY_TAKEAWAYS/input_url.txt` | `.txt` | 58 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/assets/golden/MISINFORMATION_RADAR/expected_domain_summary.json` | `.json` | 468 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/assets/golden/MISINFORMATION_RADAR/gemini_response.json` | `.json` | 471 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/assets/golden/MISINFORMATION_RADAR/input.html` | `.html` | 209 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/assets/golden/MISINFORMATION_RADAR/input_url.txt` | `.txt` | 39 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/assets/golden/MULTIMEDIA_ANALYSIS/expected_domain_summary.json` | `.json` | 671 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/assets/golden/MULTIMEDIA_ANALYSIS/gemini_response.json` | `.json` | 674 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/assets/golden/MULTIMEDIA_ANALYSIS/input_url.txt` | `.txt` | 34 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/assets/golden/MULTIMEDIA_ANALYSIS/transcript.txt` | `.txt` | 340 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/assets/golden/PERSPECTIVES_COUNTERPOSITIONS/expected_domain_summary.json` | `.json` | 685 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/assets/golden/PERSPECTIVES_COUNTERPOSITIONS/gemini_response.json` | `.json` | 688 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/assets/golden/PERSPECTIVES_COUNTERPOSITIONS/input.html` | `.html` | 358 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/assets/golden/PERSPECTIVES_COUNTERPOSITIONS/input_url.txt` | `.txt` | 37 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/assets/golden/PHOTO_SCREENSHOT_ANALYSIS/expected_domain_summary.json` | `.json` | 564 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/assets/golden/PHOTO_SCREENSHOT_ANALYSIS/gemini_response.json` | `.json` | 567 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/assets/golden/PHOTO_SCREENSHOT_ANALYSIS/input_image_meta.txt` | `.txt` | 64 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/assets/golden/RELEVANT_ASPECTS/expected_domain_summary.json` | `.json` | 625 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/assets/golden/RELEVANT_ASPECTS/gemini_response.json` | `.json` | 628 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/assets/golden/RELEVANT_ASPECTS/input.html` | `.html` | 306 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/assets/golden/RELEVANT_ASPECTS/input_url.txt` | `.txt` | 42 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/assets/golden/RISK_ANALYSIS/expected_domain_summary.json` | `.json` | 536 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/assets/golden/RISK_ANALYSIS/gemini_response.json` | `.json` | 540 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/assets/golden/RISK_ANALYSIS/input.html` | `.html` | 256 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/assets/golden/RISK_ANALYSIS/input_url.txt` | `.txt` | 39 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/assets/golden/WEB_SUMMARY/expected_domain_summary.json` | `.json` | 748 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/assets/golden/WEB_SUMMARY/gemini_response.json` | `.json` | 751 | 2026-08-09 16:54:53 ICT | Android Unit / Robolectric Tests |
| `app/src/test/assets/golden/WEB_SUMMARY/input.html` | `.html` | 995 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/assets/golden/WEB_SUMMARY/input_url.txt` | `.txt` | 50 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/java/com/example/A1ContractValidatorTest.kt` | `.kt` | 5,615 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/java/com/example/A2ContractValidatorTest.kt` | `.kt` | 7,293 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/java/com/example/BaseArchitectureRegressionTest.kt` | `.kt` | 24,293 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/java/com/example/BaseDiagnosticReportTest.kt` | `.kt` | 6,704 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/java/com/example/BulkMigrationVerificationTest.kt` | `.kt` | 3,180 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/java/com/example/ContentExtractionRegressionTest.kt` | `.kt` | 53,671 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/java/com/example/ExampleRobolectricTest.kt` | `.kt` | 64,949 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/java/com/example/ExampleUnitTest.kt` | `.kt` | 44,908 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/java/com/example/FavoritesReorderingTest.kt` | `.kt` | 4,063 | 2026-08-09 16:54:53 ICT | Android Unit / Robolectric Tests |
| `app/src/test/java/com/example/GeminiMissingPartsTest.kt` | `.kt` | 13,962 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/java/com/example/GoogleMapsBaseContextSourceTest.kt` | `.kt` | 4,186 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/java/com/example/GoogleMapsDisambiguatorTest.kt` | `.kt` | 5,951 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/java/com/example/GoogleMapsLocationContextServiceTest.kt` | `.kt` | 26,144 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/java/com/example/GoogleMapsUrlParserTest.kt` | `.kt` | 9,428 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/java/com/example/GreetingScreenshotTest.kt` | `.kt` | 14,643 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/java/com/example/LiveTest.kt` | `.kt` | 1,089 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/java/com/example/LocationContextDiagnosticTest.kt` | `.kt` | 2,270 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/java/com/example/LocationQuestionCoordinatorTest.kt` | `.kt` | 11,455 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/java/com/example/LocationQuestionEngineTest.kt` | `.kt` | 7,683 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/java/com/example/LocationQuestionPlannerTest.kt` | `.kt` | 5,756 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/java/com/example/MultimediaMetadataFallbackTest.kt` | `.kt` | 1,766 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/java/com/example/MultimediaTranscriptReferenceTest.kt` | `.kt` | 2,637 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/java/com/example/PipelineReportTest.kt` | `.kt` | 21,709 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/java/com/example/PlacesApiServiceTest.kt` | `.kt` | 4,926 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/java/com/example/RelevantorSelfTestHarnessTest.kt` | `.kt` | 86,829 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/java/com/example/ReviewTest.kt` | `.kt` | 509 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/java/com/example/TranscriptProviderTest.kt` | `.kt` | 1,716 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/java/com/example/WikipediaContextSourceTest.kt` | `.kt` | 7,088 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/java/com/example/WikivoyageContextSourceTest.kt` | `.kt` | 8,231 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/java/com/example/contextpoc/ContextCandidate.kt` | `.kt` | 325 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/java/com/example/contextpoc/ContextCandidateMatcher.kt` | `.kt` | 2,156 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/java/com/example/contextpoc/ContextMatchResult.kt` | `.kt` | 219 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/java/com/example/contextpoc/ContextMatchingTest.kt` | `.kt` | 1,699 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/java/com/example/contextpoc/ContextPlaceInput.kt` | `.kt` | 244 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/java/com/example/contextpoc/ContextResolutionStatus.kt` | `.kt` | 136 | 2026-08-09 16:54:53 ICT | Android Unit / Robolectric Tests |
| `app/src/test/java/com/example/contextpoc/ContextResolver.kt` | `.kt` | 4,227 | 2026-08-09 16:54:53 ICT | Android Unit / Robolectric Tests |
| `app/src/test/java/com/example/contextpoc/ContextResolverIntegrationTest.kt` | `.kt` | 5,308 | 2026-08-09 16:54:53 ICT | Android Unit / Robolectric Tests |
| `app/src/test/java/com/example/contextpoc/ContextResolverResult.kt` | `.kt` | 244 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/java/com/example/contextpoc/ContextSourceMetadata.kt` | `.kt` | 234 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/java/com/example/contextpoc/ContextSourceType.kt` | `.kt` | 93 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/java/com/example/contextpoc/ContextSuitabilityEvaluator.kt` | `.kt` | 391 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/java/com/example/contextpoc/SimpleTest.kt` | `.kt` | 529 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/java/com/example/contextpoc/WikipediaApiClient.kt` | `.kt` | 4,091 | 2026-08-09 16:54:53 ICT | Android Unit / Robolectric Tests |
| `app/src/test/java/com/example/contextpoc/WikipediaApiClientTest.kt` | `.kt` | 1,553 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/java/com/example/contextpoc/WikipediaMockFixtures.kt` | `.kt` | 6,372 | 2026-08-09 16:54:53 ICT | Android Unit / Robolectric Tests |
| `app/src/test/java/com/example/data/PublicVideoSourceResolverTest.kt` | `.kt` | 1,566 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/java/com/example/data/SupabaseSystemStatusTest.kt` | `.kt` | 6,792 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/java/com/example/share/DirectShareTest.kt` | `.kt` | 2,547 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/java/com/example/share/DirectShareViewModelTest.kt` | `.kt` | 3,476 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/screenshots/greeting.png` | `.png` | 4,737 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `app/src/test/screenshots/gudrun_nolte_pdf_result.png` | `.png` | 373,126 | 2026-08-09 16:54:54 ICT | Android Unit / Robolectric Tests |
| `assets/.aistudio/.gitignore` | `(keine)` | 2 | 2026-08-09 16:54:54 ICT | Projekt Root Asset / Konfiguration |
| `assets/Relevantor-App-Icon.png` | `.png` | 2,531,012 | 2026-08-09 16:54:54 ICT | Projekt Root Asset / Konfiguration |
| `assets/Relevantor-App-Icon_V2.png` | `.png` | 848,644 | 2026-08-09 16:54:54 ICT | Projekt Root Asset / Konfiguration |
| `assets/Relevantor-App-Icon_V3.png` | `.png` | 2,661,058 | 2026-08-09 16:54:54 ICT | Projekt Root Asset / Konfiguration |
| `build.gradle.kts` | `.kts` | 421 | 2026-08-09 16:54:54 ICT | Gradle Build Configuration |
| `build_metadata/EXPORT_VERIFICATION.txt` | `.txt` | 784 | 2026-08-09 16:54:54 ICT | Projekt Root Asset / Konfiguration |
| `build_metadata/metadata.json` | `.json` | 224 | 2026-08-09 16:54:54 ICT | Projekt Root Asset / Konfiguration |
| `debug.keystore` | `.keystore` | 2,666 | 2026-08-09 16:54:55 ICT | Projekt Root Asset / Konfiguration |
| `debug.keystore.base64` | `.base64` | 3,556 | 2026-08-09 17:58:11 ICT | Projekt Root Asset / Konfiguration |
| `docs_md/ABSTRACTOR_SYSTEM_STATE.md` | `.md` | 21,886 | 2026-08-09 16:54:54 ICT | Projektdokumentation & Checkpoints |
| `docs_md/CP00_DOCS_MD_CLEANUP_DRY_RUN_2026-08-09.md` | `.md` | 6,601 | 2026-08-09 17:57:20 ICT | Projektdokumentation & Checkpoints |
| `docs_md/CP00_DOCS_MD_CLEANUP_EXECUTION_2026-08-09.md` | `.md` | 3,639 | 2026-08-09 18:01:41 ICT | Projektdokumentation & Checkpoints |
| `docs_md/CP08_MVP1B_SUPABASE_SCHEMA_FOUNDATION_2026-08-07_14-25-00_ICT.md` | `.md` | 3,284 | 2026-08-09 16:54:54 ICT | Projektdokumentation & Checkpoints |
| `docs_md/CP08_MVP1C_FINAL_ACCEPTANCE_V2_2026-08-07_08-59-45_ICT.md` | `.md` | 3,020 | 2026-08-09 16:54:54 ICT | Projektdokumentation & Checkpoints |
| `docs_md/GAIS-Architektur_2026-08-09.md` | `.md` | 10,071 | 2026-08-09 17:22:11 ICT | Projektdokumentation & Checkpoints |
| `docs_md/GAIS-Verzeichnisstruktur_2026-08-09.md` | `.md` | 81,974 | 2026-08-09 17:57:42 ICT | Projektdokumentation & Checkpoints |
| `docs_md/GAIS_GOVERNANCE_SMOKE_TEST_2026-08-09_17-20-40_ICT.md` | `.md` | 5,083 | 2026-08-09 17:21:36 ICT | Projektdokumentation & Checkpoints |
| `docs_md/PROJECT_CONTEXT_RELEVANTOR.md` | `.md` | 6,468 | 2026-08-09 17:22:24 ICT | Projektdokumentation & Checkpoints |
| `docs_md/README_INSTALL.txt` | `.txt` | 635 | 2026-08-09 16:54:54 ICT | Projektdokumentation & Checkpoints |
| `docs_md/RELEVANTOR_ARCHITECTURE.md` | `.md` | 8,726 | 2026-08-09 16:54:53 ICT | Projektdokumentation & Checkpoints |
| `docs_md/RELEVANTOR_BASELINE_LOCAL_FIRST.md` | `.md` | 12,663 | 2026-08-09 16:54:54 ICT | Projektdokumentation & Checkpoints |
| `docs_md/RELEVANTOR_DEVELOPMENT_STATUS.md` | `.md` | 2,627 | 2026-08-09 17:22:32 ICT | Projektdokumentation & Checkpoints |
| `docs_md/RELEVANTOR_FUNCTION_EXECUTION_MODEL.md` | `.md` | 17,584 | 2026-08-09 16:54:54 ICT | Projektdokumentation & Checkpoints |
| `docs_md/RELEVANTOR_GAIS_WORKSPACE_RULES.md` | `.md` | 5,352 | 2026-08-09 16:54:53 ICT | Projektdokumentation & Checkpoints |
| `docs_md/RELEVANTOR_OUTPUT_SPEC.md` | `.md` | 16,632 | 2026-08-09 16:54:54 ICT | Projektdokumentation & Checkpoints |
| `docs_md/RELEVANTOR_SELF_TEST_MATRIX.md` | `.md` | 4,879 | 2026-08-09 16:54:54 ICT | Projektdokumentation & Checkpoints |
| `docs_md/TEST_COVERAGE_MATRIX.md` | `.md` | 1,483 | 2026-08-09 16:54:54 ICT | Projektdokumentation & Checkpoints |
| `docs_md/archive/CP00_GAIS_ERROR_FORENSIC_AUDIT_2026-08-03_04-00-00_UTC.md` | `.md` | 2,728 | 2026-08-09 16:54:54 ICT | Dokumentations-Archiv |
| `docs_md/archive/CP00_GAIS_PATH_GOVERNANCE_ALIGNMENT_2026-08-08_18-53-33_ICT.md` | `.md` | 4,546 | 2026-08-09 16:54:54 ICT | Dokumentations-Archiv |
| `docs_md/archive/CP00_GHOST_DIRECTORY_AUDIT_2026-08-03_03-48-00_UTC.md` | `.md` | 2,918 | 2026-08-09 16:54:54 ICT | Dokumentations-Archiv |
| `docs_md/archive/CP00_GHOST_DIRECTORY_CLEANUP_2026-08-03_03-52-00_UTC.md` | `.md` | 2,826 | 2026-08-09 16:54:54 ICT | Dokumentations-Archiv |
| `docs_md/archive/CP00_GHOST_DOCUMENTATION_CLEANUP_2026-08-08_18-38-12_ICT.md` | `.md` | 2,740 | 2026-08-09 16:54:54 ICT | Dokumentations-Archiv |
| `docs_md/archive/CP00_GHOST_PATH_RECOVERY_MVP1C_2026-08-07_08-49-42_ICT.md` | `.md` | 729 | 2026-08-09 16:54:54 ICT | Dokumentations-Archiv |
| `docs_md/archive/CP00_GITHUB_CHECKPOINT_PREPARATION_2026-08-08_16-21-31_ICT.md` | `.md` | 4,474 | 2026-08-09 16:54:54 ICT | Dokumentations-Archiv |
| `docs_md/archive/CP00_GITHUB_CHECKPOINT_READY_2026-08-08_18-56-14_ICT.md` | `.md` | 4,773 | 2026-08-09 16:54:53 ICT | Dokumentations-Archiv |
| `docs_md/archive/CP00_GIT_RECOVERY_STRATEGY_2026-08-08_16-03-39_ICT.md` | `.md` | 3,868 | 2026-08-09 16:54:54 ICT | Dokumentations-Archiv |
| `docs_md/archive/CP00_GIT_RECOVERY_STRATEGY_2026-08-08_16-17-13_ICT.md` | `.md` | 4,559 | 2026-08-09 16:54:53 ICT | Dokumentations-Archiv |
| `docs_md/archive/CP00_ICON_FORENSIC_ANALYSIS_2026-08-03_04-30-00_UTC.md` | `.md` | 3,299 | 2026-08-09 16:54:54 ICT | Dokumentations-Archiv |
| `docs_md/archive/CP00_ICON_RESTORE_2026-08-03_04-35-00_UTC.md` | `.md` | 2,659 | 2026-08-09 16:54:54 ICT | Dokumentations-Archiv |
| `docs_md/archive/CP00_MVP1C_STABLE_CHECKPOINT_2026-08-08_15-53-54_ICT.md` | `.md` | 4,741 | 2026-08-09 16:54:54 ICT | Dokumentations-Archiv |
| `docs_md/archive/CP00_POST_ICON_RESTORE_VALIDATION_2026-08-03_04-39-16_UTC.md` | `.md` | 2,806 | 2026-08-09 16:54:54 ICT | Dokumentations-Archiv |
| `docs_md/archive/CP00_POST_PHASE6_INTEGRITY_AUDIT_2026-08-03_03-30-00_UTC.md` | `.md` | 3,015 | 2026-08-09 16:54:54 ICT | Dokumentations-Archiv |
| `docs_md/archive/CP00_REPOSITORY_INTEGRITY_AUDIT_2026-08-02_14-55.md` | `.md` | 15,517 | 2026-08-09 16:54:54 ICT | Dokumentations-Archiv |
| `docs_md/archive/CP00_RUNTIME_REGRESSION_AUDIT_2026-08-03_04-25-00_UTC.md` | `.md` | 4,176 | 2026-08-09 16:54:54 ICT | Dokumentations-Archiv |
| `docs_md/archive/CP00_WORKSPACE_DUPLICATE_ROOT_CAUSE_ANALYSIS_2026-08-08_18-31-53_ICT.md` | `.md` | 5,783 | 2026-08-09 16:54:54 ICT | Dokumentations-Archiv |
| `docs_md/archive/CP00_WORKSPACE_PATH_ANALYSIS_2026-08-08_16-11-41_ICT.md` | `.md` | 2,650 | 2026-08-09 16:54:53 ICT | Dokumentations-Archiv |
| `docs_md/archive/CP00_WORKSPACE_PATH_VERIFICATION_2026-08-08_16-34-20_ICT.md` | `.md` | 4,065 | 2026-08-09 16:54:54 ICT | Dokumentations-Archiv |
| `docs_md/archive/CP03_GOOGLE_MAPS_LOCATION_QUERY_DRY_RUN_FINAL_2026-08-02_15-16-24_UTC.md` | `.md` | 17,445 | 2026-08-09 16:54:53 ICT | Dokumentations-Archiv |
| `docs_md/archive/CP03_GOOGLE_MAPS_LOCATION_QUERY_DRY_RUN_FINAL_2026-08-02_15-26-03_UTC.md` | `.md` | 17,445 | 2026-08-09 16:54:54 ICT | Dokumentations-Archiv |
| `docs_md/archive/CP03_LOCATION_QUESTION_PHASE1_PLANNER_2026-08-02_15-32-14_UTC.md` | `.md` | 4,092 | 2026-08-09 16:54:53 ICT | Dokumentations-Archiv |
| `docs_md/archive/CP03_PHASE2_REGISTRATION_2026-08-03_02-28-20_UTC.md` | `.md` | 3,025 | 2026-08-09 16:54:54 ICT | Dokumentations-Archiv |
| `docs_md/archive/CP03_PHASE3_PRECHECK_2026-08-03_02-33-15_UTC.md` | `.md` | 4,429 | 2026-08-09 16:54:54 ICT | Dokumentations-Archiv |
| `docs_md/archive/CP03_PHASE4_COORDINATOR_2026-08-03_02-48-00_UTC.md` | `.md` | 4,309 | 2026-08-09 16:54:54 ICT | Dokumentations-Archiv |
| `docs_md/archive/CP03_PHASE5_CHANGE_AUDIT_2026-08-03_03-15-00_UTC.md` | `.md` | 4,241 | 2026-08-09 16:54:54 ICT | Dokumentations-Archiv |
| `docs_md/archive/CP03_PHASE5_ENGINE_2026-08-03_03-05-00_UTC.md` | `.md` | 3,221 | 2026-08-09 16:54:54 ICT | Dokumentations-Archiv |
| `docs_md/archive/CP03_PHASE6_E2E_TEST_2026-08-03_03-25-00_UTC.md` | `.md` | 5,178 | 2026-08-09 16:54:54 ICT | Dokumentations-Archiv |
| `docs_md/archive/CP08_BACKEND_ARCHITEKTUR_REVIEW_2026-08-06_05-35-00_UTC.md` | `.md` | 18,504 | 2026-08-09 16:54:53 ICT | Dokumentations-Archiv |
| `docs_md/archive/CP08_MVP1C_APP_SUPABASE_DB_PROOF_DRY_RUN_2026-08-07_14-42-00_ICT.md` | `.md` | 7,535 | 2026-08-09 16:54:54 ICT | Dokumentations-Archiv |
| `docs_md/archive/CP08_MVP1C_APP_SUPABASE_DB_PROOF_DRY_RUN_V2_2026-08-07_14-50-00_ICT.md` | `.md` | 6,118 | 2026-08-09 16:54:54 ICT | Dokumentations-Archiv |
| `docs_md/archive/CP08_MVP1C_APP_SUPABASE_DB_PROOF_IMPLEMENTATION_2026-08-07_14-58-00_ICT.md` | `.md` | 3,589 | 2026-08-09 16:54:54 ICT | Dokumentations-Archiv |
| `docs_md/archive/CP08_MVP1C_BACKEND_VERSION_MAPPING_FIX_2026-08-07_15-41-00_ICT.md` | `.md` | 619 | 2026-08-09 16:54:54 ICT | Dokumentations-Archiv |
| `docs_md/archive/CP08_MVP1_BACKEND_FOUNDATION_DRY_RUN_2026-08-07_13-46-25_ICT.md` | `.md` | 7,740 | 2026-08-09 16:54:53 ICT | Dokumentations-Archiv |
| `docs_md/archive/CP08_MVP1_GAIS_SUPABASE_CONNECTIVITY_2026-08-07_13-52-00_ICT.md` | `.md` | 3,693 | 2026-08-09 16:54:54 ICT | Dokumentations-Archiv |
| `docs_md/archive/GAIS-Architektur_2026-08-02.md` | `.md` | 33,635 | 2026-08-09 16:54:54 ICT | Dokumentations-Archiv |
| `docs_md/archive/GAIS-Architektur_2026-08-07.md` | `.md` | 10,071 | 2026-08-09 17:21:54 ICT | Dokumentations-Archiv |
| `docs_md/archive/GAIS-Verzeichnisstruktur_2026-08-02.md` | `.md` | 23,423 | 2026-08-09 16:54:54 ICT | Dokumentations-Archiv |
| `docs_md/archive/GAIS-Verzeichnisstruktur_2026-08-03.md` | `.md` | 169,709 | 2026-08-09 16:54:54 ICT | Dokumentations-Archiv |
| `docs_md/archive/GAIS-Verzeichnisstruktur_2026-08-05.md` | `.md` | 24,314 | 2026-08-09 16:54:54 ICT | Dokumentations-Archiv |
| `docs_md/archive/GAIS-Verzeichnisstruktur_2026-08-07.md` | `.md` | 81,974 | 2026-08-09 17:57:42 ICT | Dokumentations-Archiv |
| `docs_md/archive/GAIS-Verzeichnisstruktur_2026-08-08.md` | `.md` | 81,974 | 2026-08-09 17:57:42 ICT | Dokumentations-Archiv |
| `docs_md/archive/GAIS_GOVERNANCE_SMOKE_TEST_2026-08-08_15-44-05_ICT.md` | `.md` | 5,151 | 2026-08-09 17:21:25 ICT | Dokumentations-Archiv |
| `docs_md/archive/GEMINI_429_TRUE_CAUSE_REPORT.md` | `.md` | 4,779 | 2026-08-09 16:54:54 ICT | Dokumentations-Archiv |
| `docs_md/archive/GITHUB_BACKUP_CP03_PHASE5_2026-08-03_03-18-00_UTC.md` | `.md` | 2,282 | 2026-08-09 16:54:54 ICT | Dokumentations-Archiv |
| `docs_md/archive/GITHUB_BASELINE_CP03_V13_2026-08-03_04-12-00_UTC.md` | `.md` | 1,942 | 2026-08-09 16:54:54 ICT | Dokumentations-Archiv |
| `docs_md/archive/GITHUB_CHECKPOINT_CP03_FINAL_2026-08-03_03-55-00_UTC.md` | `.md` | 2,033 | 2026-08-09 16:54:54 ICT | Dokumentations-Archiv |
| `docs_md/archive/GITHUB_FINAL_STAGING_AUDIT_CP03_2026-08-03_03-41-00_UTC.md` | `.md` | 4,534 | 2026-08-09 16:54:54 ICT | Dokumentations-Archiv |
| `docs_md/archive/GITHUB_ICON_RESTORE_CHECKPOINT_2026-08-03_04-41-46_UTC.md` | `.md` | 1,422 | 2026-08-09 16:54:54 ICT | Dokumentations-Archiv |
| `docs_md/archive/GOOGLE_AI_STUDIO_RESPONSE_ARCHITECTURAL_ANALYSIS.md` | `.md` | 12,607 | 2026-08-09 16:54:54 ICT | Dokumentations-Archiv |
| `docs_md/archive/GOOGLE_AI_STUDIO_RESPONSE_ARCHITECTURE_CORRECTION.md` | `.md` | 8,667 | 2026-08-09 16:54:54 ICT | Dokumentations-Archiv |
| `docs_md/archive/GOOGLE_AI_STUDIO_RESPONSE_OUTPUT_QUALITY_VERIFICATION.md` | `.md` | 5,747 | 2026-08-09 16:54:54 ICT | Dokumentations-Archiv |
| `docs_md/archive/GOOGLE_AI_STUDIO_RESPONSE_VERIFIED_CAPABILITIES.md` | `.md` | 2,207 | 2026-08-09 16:54:54 ICT | Dokumentations-Archiv |
| `docs_md/archive/GOOGLE_AI_STUDIO_RESPONSE_VERIFY_AGENT.md` | `.md` | 5,983 | 2026-08-09 16:54:53 ICT | Dokumentations-Archiv |
| `docs_md/archive/LOCAL_BUILD_HANDOFF.md` | `.md` | 5,271 | 2026-08-09 16:54:54 ICT | Dokumentations-Archiv |
| `docs_md/archive/NEW_CHAT_BOOTSTRAP_ANALYSIS_2026-08-03_08-23-30_UTC.md` | `.md` | 5,041 | 2026-08-09 16:54:53 ICT | Dokumentations-Archiv |
| `docs_md/archive/RELEVANTOR_CONTEXT_RECOVERY_2026-08-03_07-52-56_UTC.md` | `.md` | 6,975 | 2026-08-09 16:54:54 ICT | Dokumentations-Archiv |
| `docs_md/archive/RELEVANTOR_IMPLEMENTATION_CONTEXT_EXPORT.md` | `.md` | 32,844 | 2026-08-09 16:54:53 ICT | Dokumentations-Archiv |
| `docs_md/archive/RELEVANTOR_LAUNCHER_ICON_DIAGNOSIS_2026-08-02.md` | `.md` | 9,311 | 2026-08-09 16:54:54 ICT | Dokumentations-Archiv |
| `docs_md/archive/RELEVANTOR_PROJECT_CONTEXT_EXPORT_2026-08-02.md` | `.md` | 15,485 | 2026-08-09 16:54:54 ICT | Dokumentations-Archiv |
| `docs_md/archive/RELEVANTOR_RESOURCE_FORENSIC_AUDIT_2026-08-02.md` | `.md` | 10,695 | 2026-08-09 16:54:54 ICT | Dokumentations-Archiv |
| `docs_md/archive/RELEVANTOR_SYSTEM_STATE_2026-07-01_01-01-32.md` | `.md` | 25,356 | 2026-08-09 16:54:54 ICT | Dokumentations-Archiv |
| `docs_md/archive/RELEVANTOR_VERIFIED_PROJECT_BASE_2026-08-03_08-01-26_UTC.md` | `.md` | 5,617 | 2026-08-09 16:54:54 ICT | Dokumentations-Archiv |
| `docs_md/archive/RELEVANTOR_VERIFIED_PROJECT_BASE_CORRECTED_2026-08-03_08-07-20_UTC.md` | `.md` | 4,409 | 2026-08-09 16:54:54 ICT | Dokumentations-Archiv |
| `docs_md/archive/RELEVANTOR_VERIFIED_WORKSPACE_STATE_2026-08-02.md` | `.md` | 14,114 | 2026-08-09 16:54:54 ICT | Dokumentations-Archiv |
| `docs_md/archive/RELEVANTOR_WORKSPACE_MASTER_V1_3_2026-08-03_04-10-00_UTC.md` | `.md` | 4,482 | 2026-08-09 16:54:54 ICT | Dokumentations-Archiv |
| `docs_md/archive/WORKSPACE_INVENTORY_2026-08-03_08-54-42_UTC.md` | `.md` | 8,110 | 2026-08-09 16:54:54 ICT | Dokumentations-Archiv |
| `docs_md/archive/ZUSAMMENFASSUNG_ARBEITEN.md` | `.md` | 9,808 | 2026-08-09 16:54:54 ICT | Dokumentations-Archiv |
| `docs_md/archive/b_relevantor_home_coffeehouse_background.png` | `.png` | 2,856,764 | 2026-08-09 16:54:54 ICT | Dokumentations-Archiv |
| `docs_md/archive/incident_reviews/GAIS_SELF_CRITICAL_INCIDENT_REVIEW_2026-08-03_02-34-11_UTC.md` | `.md` | 172 | 2026-08-09 16:54:54 ICT | Dokumentations-Archiv |
| `docs_md/archive/raw_b3_live_response.json` | `.json` | 3,223 | 2026-08-09 16:54:54 ICT | Dokumentations-Archiv |
| `docs_md/archive/raw_b3_live_response_v13.json` | `.json` | 2,867 | 2026-08-09 16:54:54 ICT | Dokumentations-Archiv |
| `docs_md/archive/raw_b3_live_response_v14.json` | `.json` | 1,638 | 2026-08-09 16:54:53 ICT | Dokumentations-Archiv |
| `docs_md/archive/relevantor_home_coffeehouse.png` | `.png` | 2,856,764 | 2026-08-09 16:54:53 ICT | Dokumentations-Archiv |
| `docs_md/archive/relevantor_home_coffeehouse_background.png` | `.png` | 2,856,764 | 2026-08-09 16:54:53 ICT | Dokumentations-Archiv |
| `docs_md/archive/relevantor_home_coffeehouse_objective.png` | `.png` | 2,420,445 | 2026-08-09 16:54:54 ICT | Dokumentations-Archiv |
| `docs_md/archive/verzeichnisstruktur-und-dateien.md` | `.md` | 17,116 | 2026-08-09 16:54:54 ICT | Dokumentations-Archiv |
| `fix_test.sh` | `.sh` | 247 | 2026-08-09 16:54:54 ICT | Projekt Root Asset / Konfiguration |
| `gradle.properties` | `.properties` | 1,414 | 2026-08-09 16:54:54 ICT | Gradle Build Configuration |
| `gradle/libs.versions.toml` | `.toml` | 6,792 | 2026-08-09 16:54:53 ICT | Gradle Build Configuration |
| `metadata.json` | `.json` | 224 | 2026-08-09 16:54:54 ICT | Projekt Root Asset / Konfiguration |
| `settings.gradle.kts` | `.kts` | 554 | 2026-08-09 16:54:54 ICT | Gradle Build Configuration |
| `supabase/config.toml` | `.toml` | 253 | 2026-08-09 16:54:54 ICT | Supabase Backend Configuration & Migrations |
| `supabase/migrations/20260807000000_mvp1_system_status.sql` | `.sql` | 943 | 2026-08-09 16:54:53 ICT | Supabase Backend Configuration & Migrations |
| `tools/build_structure_doc.py` | `.py` | 5,856 | 2026-08-09 17:22:41 ICT | Governance & Automation Scripts |
| `tools/git_post_ui_push_health_gate.sh` | `.sh` | 2,683 | 2026-08-09 16:54:53 ICT | Governance & Automation Scripts |
| `tools/patch_disambiguator.sh` | `.sh` | 5,495 | 2026-08-09 16:54:54 ICT | Governance & Automation Scripts |
| `tools/patch_placesapi.sh` | `.sh` | 191 | 2026-08-09 16:54:54 ICT | Governance & Automation Scripts |
| `tools/patch_tests.sh` | `.sh` | 1,314 | 2026-08-09 16:54:54 ICT | Governance & Automation Scripts |
| `tools/patch_trace.sh` | `.sh` | 537 | 2026-08-09 16:54:54 ICT | Governance & Automation Scripts |
| `tools/report_generator.py` | `.py` | 4,299 | 2026-08-09 16:54:54 ICT | Governance & Automation Scripts |

---

## 3. Zusammenfassung der Hauptverzeichnisse

- **`app/`**: Das Android Application Module (Jetpack Compose UI, ViewModel, Retrofit/Moshi API Clients, Room Database, Robolectric Unit Tests).
- **`app/src/main/assets/prompts/`**: Die geschützten System-Prompts, Funktion-Registrierungen (`function_registry.json`) und Prompt-Manifeste (`prompt_manifest.json`).
- **`app/src/main/assets/change-prompts/`**: Vorlagen für Change-Prompts (CP-01 bis CP-08).
- **`docs_md/`**: Das kanonische Dokumentationsverzeichnis für Systemarchitektur, Abnahmeberichte, Testmatrizen und Checkpoints (`archive/` enthält historische Berichte).
- **`supabase/`**: Supabase CLI Konfiguration (`config.toml`) und SQL-Migrationen (`20260807000000_mvp1_system_status.sql`).
- **`tools/`**: Automation- und Governance-Skripte (`git_post_ui_push_health_gate.sh`, Patch-Tools).
- **`gradle/`**: Build-Konfigurationen, Dependencies (`libs.versions.toml`) und Gradle Wrapper Properties.

---

*Dieser Bericht wurde automatisiert aus dem Dateisystem des Relevantor-Workspaces generiert.*
