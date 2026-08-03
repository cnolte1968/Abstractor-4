```text
/
├── .build-outputs/
│   └── app-debug.apk
├── .env.example
├── .github/
│   └── workflows/
│       └── build-apk.yml
├── .gitignore
├── AGENTS.md
├── ARCHITECTURE_FREEZE.md
├── FUNCTION_SPEC_TEMPLATE.md
├── GEMINI_429_TRUE_CAUSE_REPORT.md
├── app/
│   ├── .gitignore
│   ├── GEMINI_429_TRUE_CAUSE_REPORT.md
│   ├── applet/
│   ├── build.gradle.kts
│   ├── finalUserContent.txt
│   ├── proguard-rules.pro
│   ├── raw_b3_live_response.json
│   ├── raw_b3_live_response_v13.json
│   ├── raw_b3_live_response_v14.json
│   ├── raw_gemini_response.json
│   └── src/
│       ├── androidTest/
│       │   └── java/
│       │       └── com/
│       │           └── example/
│       │               └── ExampleInstrumentedTest.kt
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── assets/
│       │   │   ├── ARCHITECTURE_FREEZE.md
│       │   │   ├── change-prompts/
│       │   │   │   ├── CP-01_OPTIMIERUNG_FUNKTIONS_PROMPT.md
│       │   │   │   ├── CP-02_AENDERUNG_ARBEITSWEISE.md
│       │   │   │   ├── CP-03_NEUANLAGE_FUNKTION.md
│       │   │   │   ├── CP-07_UI_UX_AENDERUNG.md
│       │   │   │   ├── CP_GUIDELINE.md
│       │   │   │   └── README_CHANGE_PROMPTS.md
│       │   │   └── prompts/
│       │   │       ├── F_AKTUALITAETS_CHECK.md
│       │   │       ├── F_BUSINESS_INKUBATOR.md
│       │   │       ├── F_DOKUMENTE.md
│       │   │       ├── F_FACTS_VS_OPINIONS_ANALYZER.md
│       │   │       ├── F_FEHLINFORMATIONS_RADAR.md
│       │   │       ├── F_FREIE_QUELLENANFRAGE.md
│       │   │       ├── F_GOOGLE_MAPS_ANALYZER.md
│       │   │       ├── F_GOOGLE_MAPS_LOCATION_CONTEXT.md
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
│       │   ├── java/
│       │   │   └── com/
│       │   │       └── example/
│       │   │           ├── LocalContentExtractionEngine.kt
│       │   │           ├── MainActivity.kt
│       │   │           ├── RelevantorAccessibilityService.kt
│       │   │           ├── data/
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
│       │   │           │   ├── contextengine/
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
│       │   │           │   ├── diagnostics/
│       │   │           │   │   ├── DiagnosticContributor.kt
│       │   │           │   │   ├── LocationContextDiagnosticContributor.kt
│       │   │           │   │   └── ReportSanitizer.kt
│       │   │           │   ├── engine/
│       │   │           │   │   ├── AnalysisRegistryImpl.kt
│       │   │           │   │   ├── AndroidAssetPromptLoader.kt
│       │   │           │   │   ├── BaseGeminiEngine.kt
│       │   │           │   │   ├── EngineRunnerImpl.kt
│       │   │           │   │   ├── document/
│       │   │           │   │   │   └── DocumentAnalysisEngine.kt
│       │   │           │   │   ├── top3/
│       │   │           │   │   │   └── Top3KeyPointsEngine.kt
│       │   │           │   │   └── web/
│       │   │           │   │       └── WebpageAnalysisEngine.kt
│       │   │           │   ├── extraction/
│       │   │           │   │   ├── DocumentInputExtractor.kt
│       │   │           │   │   ├── InputExtractor.kt
│       │   │           │   │   ├── InputExtractorRegistry.kt
│       │   │           │   │   ├── WebInputExtractor.kt
│       │   │           │   │   └── YoutubeInputExtractor.kt
│       │   │           │   ├── local/
│       │   │           │   │   ├── RelevantorDatabase.kt
│       │   │           │   │   └── SessionStorage.kt
│       │   │           │   ├── remote/
│       │   │           │   │   └── BackendApiService.kt
│       │   │           │   ├── repository/
│       │   │           │   │   ├── AnalysisRepositoryImpl.kt
│       │   │           │   │   ├── ContentExtractionRepositoryImpl.kt
│       │   │           │   │   ├── SyncRepositoryImpl.kt
│       │   │           │   │   ├── UserRepositoryImpl.kt
│       │   │           │   │   └── YoutubeTranscriptProviderAdapter.kt
│       │   │           │   └── sync/
│       │   │           │       ├── SyncScheduler.kt
│       │   │           │       └── SyncWorker.kt
│       │   │           ├── domain/
│       │   │           │   ├── engine/
│       │   │           │   │   ├── AnalysisEngine.kt
│       │   │           │   │   ├── AnalysisRegistry.kt
│       │   │           │   │   ├── ContractValidator.kt
│       │   │           │   │   ├── EngineRunner.kt
│       │   │           │   │   ├── PromptAssetLoader.kt
│       │   │           │   │   └── validators/
│       │   │           │   │       ├── A1ContractValidator.kt
│       │   │           │   │       └── A2ContractValidator.kt
│       │   │           │   ├── model/
│       │   │           │   │   ├── AnalysisTrace.kt
│       │   │           │   │   ├── CanonicalAnalysisInput.kt
│       │   │           │   │   ├── ContentExtractionResult.kt
│       │   │           │   │   ├── DomainSummary.kt
│       │   │           │   │   ├── ExtractedContent.kt
│       │   │           │   │   ├── PublicVideoSource.kt
│       │   │           │   │   └── TranscriptProvider.kt
│       │   │           │   ├── repository/
│       │   │           │   │   ├── AnalysisRepository.kt
│       │   │           │   │   ├── ContentExtractionRepository.kt
│       │   │           │   │   ├── GeminiGateway.kt
│       │   │           │   │   ├── SyncRepository.kt
│       │   │           │   │   └── UserRepository.kt
│       │   │           │   └── usecase/
│       │   │           │       ├── AnalyzeContentUseCase.kt
│       │   │           │       ├── ExtractContentUseCase.kt
│       │   │           │       ├── LoadHistoryUseCase.kt
│       │   │           │       ├── SaveAnalysisUseCase.kt
│       │   │           │       └── SyncUserDataUseCase.kt
│       │   │           └── ui/
│       │   │               ├── MainViewModel.kt
│       │   │               ├── components/
│       │   │               │   └── TakeawayCard.kt
│       │   │               └── metadata/
│       │   │                   ├── ExportFormatter.kt
│       │   │                   ├── FeatureCatalog.kt
│       │   │                   └── OutputPresentationPolicy.kt
│       │   └── res/
│       │       ├── drawable/
│       │       │   ├── ic_launcher_background.xml
│       │       │   ├── ic_launcher_foreground.xml
│       │       │   ├── relevantor_app_icon.png
│       │       │   ├── relevantor_app_icon_v2.png
│       │       │   └── relevantor_app_icon_v3.png
│       │       ├── drawable-nodpi/
│       │       │   ├── a_smartphone_app_ui_screenshot_portrait_with_a_w_1.png
│       │       │   ├── b_relevantor_home_coffeehouse_background.png
│       │       │   ├── relevantor_app_icon.png
│       │       │   ├── relevantor_home_coffeehouse.png
│       │       │   ├── relevantor_home_coffeehouse_background.png
│       │       │   └── relevantor_home_coffeehouse_objective.png
│       │       ├── mipmap-anydpi-v26/
│       │       │   ├── ic_launcher.xml
│       │       │   └── ic_launcher_round.xml
│       │       ├── mipmap-hdpi/
│       │       │   ├── ic_launcher.png
│       │       │   └── ic_launcher_round.png
│       │       ├── mipmap-mdpi/
│       │       │   ├── ic_launcher.png
│       │       │   └── ic_launcher_round.png
│       │       ├── mipmap-xhdpi/
│       │       │   ├── ic_launcher.png
│       │       │   └── ic_launcher_round.png
│       │       ├── mipmap-xxhdpi/
│       │       │   ├── ic_launcher.png
│       │       │   └── ic_launcher_round.png
│       │       ├── mipmap-xxxhdpi/
│       │       │   ├── ic_launcher.png
│       │       │   └── ic_launcher_round.png
│       │       ├── values/
│       │       │   ├── strings.xml
│       │       │   └── themes.xml
│       │       └── xml/
│       │           ├── accessibility_service_config.xml
│       │           ├── backup_rules.xml
│       │           └── data_extraction_rules.xml
│       └── test/
│           ├── Transkript_Youtube.txt
│           ├── assets/
│           │   └── golden/
│           │       ├── DOCUMENT_SUMMARY/
│           │       │   ├── expected_domain_summary.json
│           │       │   ├── file_name.txt
│           │       │   ├── gemini_response.json
│           │       │   └── input_document.txt
│           │       ├── FACTS_VS_OPINIONS/
│           │       │   ├── expected_domain_summary.json
│           │       │   ├── gemini_response.json
│           │       │   ├── input.html
│           │       │   └── input_url.txt
│           │       ├── FREE_SOURCE_QUERY/
│           │       │   ├── expected_domain_summary.json
│           │       │   ├── gemini_response.json
│           │       │   ├── input_text.txt
│           │       │   └── input_url.txt
│           │       ├── FRESHNESS_CHECK/
│           │       │   ├── expected_domain_summary.json
│           │       │   ├── gemini_response.json
│           │       │   ├── input.html
│           │       │   └── input_url.txt
│           │       ├── KEY_TAKEAWAYS/
│           │       │   ├── expected_domain_summary.json
│           │       │   ├── gemini_response.json
│           │       │   ├── input.html
│           │       │   └── input_url.txt
│           │       ├── MISINFORMATION_RADAR/
│           │       │   ├── expected_domain_summary.json
│           │       │   ├── gemini_response.json
│           │       │   ├── input.html
│           │       │   └── input_url.txt
│           │       ├── MULTIMEDIA_ANALYSIS/
│           │       │   ├── expected_domain_summary.json
│           │       │   ├── gemini_response.json
│           │       │   ├── input_url.txt
│           │       │   └── transcript.txt
│           │       ├── PERSPECTIVES_COUNTERPOSITIONS/
│           │       │   ├── expected_domain_summary.json
│           │       │   ├── gemini_response.json
│           │       │   ├── input.html
│           │       │   └── input_url.txt
│           │       ├── PHOTO_SCREENSHOT_ANALYSIS/
│           │       │   ├── expected_domain_summary.json
│           │       │   ├── gemini_response.json
│           │       │   └── input_image_meta.txt
│           │       ├── RELEVANT_ASPECTS/
│           │       │   ├── expected_domain_summary.json
│           │       │   ├── gemini_response.json
│           │       │   ├── input.html
│           │       │   └── input_url.txt
│           │       ├── RISK_ANALYSIS/
│           │       │   ├── expected_domain_summary.json
│           │       │   ├── gemini_response.json
│           │       │   ├── input.html
│           │       │   └── input_url.txt
│           │       └── WEB_SUMMARY/
│           │           ├── expected_domain_summary.json
│           │           ├── gemini_response.json
│           │           ├── input.html
│           │           └── input_url.txt
│           ├── java/
│           │   └── com/
│           │       └── example/
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
│           │           ├── MultimediaMetadataFallbackTest.kt
│           │           ├── MultimediaTranscriptReferenceTest.kt
│           │           ├── PipelineReportTest.kt
│           │           ├── PlacesApiServiceTest.kt
│           │           ├── RelevantorSelfTestHarnessTest.kt
│           │           ├── ReviewTest.kt
│           │           ├── TranscriptProviderTest.kt
│           │           ├── WikipediaContextSourceTest.kt
│           │           ├── WikivoyageContextSourceTest.kt
│           │           ├── contextpoc/
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
│           │           └── data/
│           │               └── PublicVideoSourceResolverTest.kt
│           └── screenshots/
│               ├── greeting.png
│               └── gudrun_nolte_pdf_result.png
├── app-debug.apk
├── assets/
│   ├── .aistudio/
│   │   └── .gitignore
│   ├── Relevantor-App-Icon.png
│   ├── Relevantor-App-Icon_V2.png
│   └── Relevantor-App-Icon_V3.png
├── build.gradle.kts
├── build_metadata/
│   ├── EXPORT_VERIFICATION.txt
│   └── metadata.json
├── debug.keystore
├── debug.keystore.base64
├── debug_archive/
│   ├── ABSTRACTOR_ARCHITECTURE_BACKUP_2026-06-13_08-18.md
│   ├── ABSTRACTOR_INSTALL_DIAGNOSTIC_BUNDLE.md
│   ├── API_KEY_RUNTIME_VERIFICATION_2026-06-12_06-50.md
│   ├── CURRENT_STATE_2026-06-12_06-37.md
│   ├── CURRENT_STATE_DE_2026-06-12_06-37.md
│   ├── DEBUG_REPORT_INDEX_2026-06-12_06-37.md
│   ├── FAKT_ODER_MEINUNG_FORMAT_REPORT_2026-06-12_06-37.md
│   ├── FUNCTION_503_COMPARISON_REPORT_2026-06-12_06-37.md
│   ├── FUNCTION_FACTS_VS_OPINIONS_ANALYZER_IMPLEMENTATION_REPORT_2026-06-12_06-37.md
│   ├── FUNCTION_PERSPECTIVES_AND_COUNTERPOSITIONS_IMPLEMENTATION_REPORT_2026-06-12_06-37.md
│   ├── GAIS_INSTALL_PIPELINE_FINAL_QH_RESOLUTION.md
│   ├── GEMINI_429_TRUE_CAUSE_REPORT.md
│   ├── GEMINI_429_TRUE_CAUSE_REPORT_2026-06-12_06-37.md
│   ├── GEMINI_MODEL_ROOT_CAUSE_REPORT_2026-06-12_06-37.md
│   ├── GOOGLE_AI_STUDIO_RESPONSE_RECOVERY_AUDIT_1.md
│   ├── Google-AI-Specs_2026-06-12_06-37.md
│   ├── PROJECT_CONTEXT_ABSTRACTOR_FULL_2026-06-12_19-30.md
│   ├── PROMPT_ARCHITECTURE_INVENTORY_2026-06-12_19-15.md
│   ├── PROMPT_ASSET_LIBRARY_CREATION_REPORT_2026-06-12_12-45.md
│   ├── SMARTPHONE_BUILD_TEST_2026-06-12_06-54.md
│   ├── TOP3_ROOT_CAUSE_2026-06-12_06-37.md
│   ├── UI_UX_OPTIMIZATION_REPORT_2026-06-12_17-55.md
│   ├── app_CURRENT_STATE_2026-06-12_06-37.md
│   └── app_GEMINI_429_TRUE_CAUSE_REPORT_2026-06-12_06-37.md
├── docs_md/
│   ├── ABSTRACTOR_SYSTEM_STATE.md
│   ├── GAIS-Architektur_2026-08-02.md
│   ├── GAIS-Verzeichnisstruktur_2026-08-02.md
│   ├── GOOGLE_AI_STUDIO_RESPONSE_ARCHITECTURAL_ANALYSIS.md
│   ├── GOOGLE_AI_STUDIO_RESPONSE_ARCHITECTURE_CORRECTION.md
│   ├── GOOGLE_AI_STUDIO_RESPONSE_OUTPUT_QUALITY_VERIFICATION.md
│   ├── GOOGLE_AI_STUDIO_RESPONSE_VERIFIED_CAPABILITIES.md
│   ├── GOOGLE_AI_STUDIO_RESPONSE_VERIFY_AGENT.md
│   ├── LOCAL_BUILD_HANDOFF.md
│   ├── PROJECT_CONTEXT_RELEVANTOR.md
│   ├── README_INSTALL.txt
│   ├── RELEVANTOR_ARCHITECTURE.md
│   ├── RELEVANTOR_BASELINE_LOCAL_FIRST.md
│   ├── RELEVANTOR_DEVELOPMENT_STATUS.md
│   ├── RELEVANTOR_FUNCTION_EXECUTION_MODEL.md
│   ├── RELEVANTOR_IMPLEMENTATION_CONTEXT_EXPORT.md
│   ├── RELEVANTOR_OUTPUT_SPEC.md
│   ├── RELEVANTOR_SELF_TEST_MATRIX.md
│   ├── RELEVANTOR_SYSTEM_STATE_2026-07-01_01-01-32.md
│   ├── TEST_COVERAGE_MATRIX.md
│   ├── ZUSAMMENFASSUNG_ARBEITEN.md
│   └── verzeichnisstruktur-und-dateien.md
├── fix_test.sh
├── gradle/
│   └── libs.versions.toml
├── gradle.properties
├── metadata.json
├── patch_disambiguator.sh
├── patch_placesapi.sh
├── patch_tests.sh
├── patch_trace.sh
├── raw_b3_live_response.json
├── raw_b3_live_response_v13.json
├── raw_b3_live_response_v14.json
├── recovery_backup_2026-07-20/
│   ├── ghost_directory_step3c1.tar.gz
│   └── ghost_directory_step3c1_manifest.txt
├── settings.gradle.kts
└── test_output.log
```
