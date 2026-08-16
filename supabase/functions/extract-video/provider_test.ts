// Minimal test scaffolding for Deno test runner
// Run with: deno test supabase/functions/extract-video/provider_test.ts

import { assertEquals } from "https://deno.land/std@0.168.0/testing/asserts.ts";
import { mapProviderResponse } from "./contract_mapper.ts";
import { ExtractVideoRequest, TranscriptProviderResponse, ProviderMetadata } from "./types.ts";

Deno.test("contract_mapper - success response mapping", () => {
    const request: ExtractVideoRequest = { sourceUrl: "http://test", sourcePlatform: "YOUTUBE", requestedCapabilities: [] };
    const transcript: TranscriptProviderResponse = {
        success: true,
        segments: [{ time: 0, text: "Hello" }],
        language: "en"
    };
    const metadata: ProviderMetadata = { title: "Test Title" };

    const mapped = mapProviderResponse(request, transcript, metadata, 10);
    assertEquals(mapped.providerStatus, "SUCCESS");
    assertEquals(mapped.capabilityStatus.TRANSCRIPT, "AVAILABLE");
    assertEquals(mapped.capabilityStatus.METADATA, "AVAILABLE");
    assertEquals(mapped.content?.transcript, "Hello");
});

Deno.test("contract_mapper - degraded response mapping (no transcript, has metadata)", () => {
    const request: ExtractVideoRequest = { sourceUrl: "http://test", sourcePlatform: "YOUTUBE", requestedCapabilities: [] };
    const transcript: TranscriptProviderResponse = {
        success: false,
        segments: [],
        error: "Not found",
        errorCode: "NOT_FOUND"
    };
    const metadata: ProviderMetadata = { title: "Test Title" };

    const mapped = mapProviderResponse(request, transcript, metadata, 10);
    assertEquals(mapped.providerStatus, "DEGRADED");
    assertEquals(mapped.capabilityStatus.TRANSCRIPT, "UNAVAILABLE");
    assertEquals(mapped.capabilityStatus.METADATA, "AVAILABLE");
    assertEquals(mapped.metadata?.title, "Test Title");
    assertEquals(mapped.content, null);
});

Deno.test("contract_mapper - failed response mapping (no transcript, no metadata)", () => {
    const request: ExtractVideoRequest = { sourceUrl: "http://test", sourcePlatform: "YOUTUBE", requestedCapabilities: [] };
    const transcript: TranscriptProviderResponse = {
        success: false,
        segments: [],
        error: "Secret missing",
        errorCode: "MISSING_SECRET"
    };

    const mapped = mapProviderResponse(request, transcript, null, 10);
    assertEquals(mapped.providerStatus, "FAILED");
    assertEquals(mapped.accessStatus, "BLOCKED"); // due to MISSING_SECRET
    assertEquals(mapped.capabilityStatus.TRANSCRIPT, "UNAVAILABLE");
    assertEquals(mapped.capabilityStatus.METADATA, "UNAVAILABLE");
});
