```text
/
├── .env.example
├── .github
│   └── workflows
│       └── build-apk.yml
├── .gitignore
├── .kotlin
│   └── sessions
├── AGENTS.md
├── ARCHITECTURE_FREEZE.md
├── FUNCTION_SPEC_TEMPLATE.md
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
│       │   │   ├── change-prompts
│       │   │   │   ├── CP-01_OPTIMIERUNG_FUNKTIONS_PROMPT.md
│       │   │   │   ├── CP-02_AENDERUNG_ARBEITSWEISE.md
│       │   │   │   ├── CP-03_NEUANLAGE_FUNKTION.md
│       │   │   │   ├── CP-07_UI_UX_AENDERUNG.md
│       │   │   │   ├── CP_GUIDELINE.md
│       │   │   │   └── README_CHANGE_PROMPTS.md
│       │   │   └── prompts
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
│       │   │           │   │   └── BackendApiService.kt
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
│       │           └── data_extraction_rules.xml
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
│           │           └── data
│           │               └── PublicVideoSourceResolverTest.kt
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
│   ├── PROJECT_CONTEXT_RELEVANTOR.md
│   ├── README_INSTALL.txt
│   ├── RELEVANTOR_ARCHITECTURE.md
│   ├── RELEVANTOR_BASELINE_LOCAL_FIRST.md
│   ├── RELEVANTOR_DEVELOPMENT_STATUS.md
│   ├── RELEVANTOR_FUNCTION_EXECUTION_MODEL.md
│   ├── RELEVANTOR_OUTPUT_SPEC.md
│   ├── RELEVANTOR_SELF_TEST_MATRIX.md
│   ├── TEST_COVERAGE_MATRIX.md
│   └── archive
│       ├── CP00_GAIS_ERROR_FORENSIC_AUDIT_2026-08-03_04-00-00_UTC.md
│       ├── CP00_GHOST_DIRECTORY_AUDIT_2026-08-03_03-48-00_UTC.md
│       ├── CP00_GHOST_DIRECTORY_CLEANUP_2026-08-03_03-52-00_UTC.md
│       ├── CP00_ICON_FORENSIC_ANALYSIS_2026-08-03_04-30-00_UTC.md
│       ├── CP00_ICON_RESTORE_2026-08-03_04-35-00_UTC.md
│       ├── CP00_POST_ICON_RESTORE_VALIDATION_2026-08-03_04-39-16_UTC.md
│       ├── CP00_POST_PHASE6_INTEGRITY_AUDIT_2026-08-03_03-30-00_UTC.md
│       ├── CP00_REPOSITORY_INTEGRITY_AUDIT_2026-08-02_14-55.md
│       ├── CP00_RUNTIME_REGRESSION_AUDIT_2026-08-03_04-25-00_UTC.md
│       ├── CP03_GOOGLE_MAPS_LOCATION_QUERY_DRY_RUN_FINAL_2026-08-02_15-16-24_UTC.md
│       ├── CP03_GOOGLE_MAPS_LOCATION_QUERY_DRY_RUN_FINAL_2026-08-02_15-26-03_UTC.md
│       ├── CP03_LOCATION_QUESTION_PHASE1_PLANNER_2026-08-02_15-32-14_UTC.md
│       ├── CP03_PHASE2_REGISTRATION_2026-08-03_02-28-20_UTC.md
│       ├── CP03_PHASE3_PRECHECK_2026-08-03_02-33-15_UTC.md
│       ├── CP03_PHASE4_COORDINATOR_2026-08-03_02-48-00_UTC.md
│       ├── CP03_PHASE5_CHANGE_AUDIT_2026-08-03_03-15-00_UTC.md
│       ├── CP03_PHASE5_ENGINE_2026-08-03_03-05-00_UTC.md
│       ├── CP03_PHASE6_E2E_TEST_2026-08-03_03-25-00_UTC.md
│       ├── GAIS-Architektur_2026-08-02.md
│       ├── GAIS-Verzeichnisstruktur_2026-08-02.md
│       ├── GAIS-Verzeichnisstruktur_2026-08-03.md
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
├── gen_tree.py
├── gradle
│   └── libs.versions.toml
├── gradle.properties
├── metadata.json
├── settings.gradle.kts
├── tools
│   ├── patch_disambiguator.sh
│   ├── patch_placesapi.sh
│   ├── patch_tests.sh
│   ├── patch_trace.sh
│   └── report_generator.py
└── tree_output.txt
```
