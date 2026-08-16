import { ExtractVideoRequest, ExtractVideoResponse, TranscriptProviderResponse, ProviderMetadata } from './types.ts';

export function mapProviderResponse(
    request: ExtractVideoRequest,
    transcriptResult: TranscriptProviderResponse,
    metadataResult: ProviderMetadata | null,
    processingTimeMs: number
): ExtractVideoResponse {
    const sourcePlatform = request.sourcePlatform || 'YOUTUBE';
    
    // Default Metadata if none returned by oEmbed
    const metadata = metadataResult ? {
        title: metadataResult.title || 'Unknown Title',
        author: metadataResult.author || 'Unknown Author',
        durationSeconds: metadataResult.durationSeconds,
        publishedAt: metadataResult.publishedAt
    } : null;

    if (transcriptResult.success && transcriptResult.segments.length > 0) {
        // SUCCESS CASE
        const fullText = transcriptResult.segments.map(s => s.text).join(' ');
        
        return {
            sourcePlatform,
            providerStatus: 'SUCCESS',
            accessStatus: 'PUBLIC',
            capabilityStatus: {
                TRANSCRIPT: 'AVAILABLE',
                METADATA: metadata ? 'AVAILABLE' : 'DEGRADED'
            },
            metadata,
            content: {
                transcript: fullText,
                language: transcriptResult.language || 'unknown',
                timestamps: transcriptResult.segments
            },
            errorInformation: null,
            diagnostics: {
                providerName: 'RapidApiProvider',
                processingTimeMs,
                diagnosticCode: 'OK'
            }
        };
    } else {
        // ERROR / DEGRADED CASE
        if (metadataResult) {
            // We have no transcript, but we have metadata -> DEGRADED
            return {
                sourcePlatform,
                providerStatus: 'DEGRADED',
                accessStatus: 'PUBLIC', // Assuming we got metadata, it's public
                capabilityStatus: {
                    TRANSCRIPT: 'UNAVAILABLE',
                    METADATA: 'AVAILABLE'
                },
                metadata,
                content: null,
                errorInformation: {
                    code: transcriptResult.errorCode || 'TRANSCRIPT_UNAVAILABLE',
                    message: transcriptResult.error || 'Transcript could not be extracted'
                },
                diagnostics: {
                    providerName: 'RapidApiProvider (Fallback oEmbed)',
                    processingTimeMs,
                    diagnosticCode: 'FALLBACK_METADATA_ONLY'
                }
            };
        } else {
            // We have neither transcript nor metadata -> FAILED
            const isBlocked = transcriptResult.errorCode === 'PROVIDER_UNAUTHORIZED' || transcriptResult.errorCode === 'MISSING_SECRET';
            
            return {
                sourcePlatform,
                providerStatus: 'FAILED',
                accessStatus: isBlocked ? 'BLOCKED' : 'PUBLIC',
                capabilityStatus: {
                    TRANSCRIPT: 'UNAVAILABLE',
                    METADATA: 'UNAVAILABLE'
                },
                metadata: null,
                content: null,
                errorInformation: {
                    code: transcriptResult.errorCode || 'FETCH_FAILED',
                    message: transcriptResult.error || 'Failed to extract any information'
                },
                diagnostics: {
                    providerName: 'RapidApiProvider',
                    processingTimeMs,
                    diagnosticCode: 'COMPLETE_FAILURE'
                }
            };
        }
    }
}

export function createTestSuccessResponse(req: ExtractVideoRequest): ExtractVideoResponse {
    return {
        sourcePlatform: req.sourcePlatform || 'YOUTUBE',
        providerStatus: 'SUCCESS',
        accessStatus: 'PUBLIC',
        capabilityStatus: {
            TRANSCRIPT: 'AVAILABLE',
            METADATA: 'AVAILABLE'
        },
        metadata: {
            title: 'Dummy Test Video',
            author: 'Test Author',
            durationSeconds: 120,
            publishedAt: '2023-01-01T12:00:00Z'
        },
        content: {
            transcript: 'Dies ist ein hartkodiertes Test-Transkript für die Architektur-Phase A1.',
            language: 'de',
            timestamps: [
                { time: 0, text: 'Dies ist ein hartkodiertes' },
                { time: 2.5, text: 'Test-Transkript für die Architektur-Phase A1.' }
            ]
        },
        errorInformation: null,
        diagnostics: {
            providerName: 'TestMockProvider',
            processingTimeMs: 15,
            diagnosticCode: 'MOCK_RESPONSE'
        }
    };
}

export function createErrorResponse(
    sourcePlatform: string,
    errorCode: string,
    errorMessage: string
): ExtractVideoResponse {
    return {
        sourcePlatform: sourcePlatform || 'UNKNOWN',
        providerStatus: 'FAILED',
        accessStatus: 'BLOCKED',
        capabilityStatus: {
            TRANSCRIPT: 'UNAVAILABLE',
            METADATA: 'UNAVAILABLE'
        },
        metadata: null,
        content: null,
        errorInformation: {
            code: errorCode,
            message: errorMessage
        }
    };
}
