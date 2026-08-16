export interface ExtractVideoRequest {
    contractVersion?: string;
    sourceUrl: string;
    sourcePlatform: string;
    requestedCapabilities: string[];
    clientContext?: Record<string, any>;
    requestId?: string;
}

export interface ExtractVideoResponse {
    sourcePlatform: string;
    providerStatus: 'SUCCESS' | 'DEGRADED' | 'FAILED';
    accessStatus: 'PUBLIC' | 'LOGIN_REQUIRED' | 'PRIVATE' | 'BLOCKED';
    capabilityStatus: Record<string, 'AVAILABLE' | 'DEGRADED' | 'UNAVAILABLE'>;
    metadata: {
        title?: string;
        author?: string;
        durationSeconds?: number;
        publishedAt?: string;
    } | null;
    content: {
        transcript?: string;
        language?: string;
        timestamps?: Array<{ time: number; text: string }>;
    } | null;
    errorInformation: {
        code: string;
        message: string;
    } | null;
    diagnostics?: {
        providerName: string;
        processingTimeMs: number;
        diagnosticCode?: string;
    };
}

export interface ProviderMetadata {
    title?: string;
    author?: string;
    durationSeconds?: number;
    publishedAt?: string;
}

export interface ProviderTranscriptSegment {
    time: number;
    text: string;
}

export interface TranscriptProviderResponse {
    success: boolean;
    segments: ProviderTranscriptSegment[];
    language?: string;
    error?: string;
    errorCode?: string;
}

export interface TranscriptProvider {
    fetchTranscript(videoId: string, sourceUrl?: string): Promise<TranscriptProviderResponse>;
}
