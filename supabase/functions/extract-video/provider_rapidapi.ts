import { TranscriptProvider, TranscriptProviderResponse, ProviderTranscriptSegment } from './types.ts';

function parseYoutubeVideoId(input: string): string | null {
    if (!input) return null;
    const regex = /(?:youtube\.com\/(?:[^\/]+\/\S+\/|(?:v|e(?:mbed)?|shorts|live)\/|\S*?[?&]v=)|youtu\.be\/)([a-zA-Z0-9_-]{11})/;
    const match = input.match(regex);
    if (match && match[1]) {
        return match[1];
    }
    try {
        const urlObj = new URL(input.startsWith('http') ? input : `https://${input}`);
        if (urlObj.hostname.includes('youtube.com')) {
            const v = urlObj.searchParams.get('v');
            if (v) return v.split('?')[0].split('&')[0].split('#')[0];

            const parts = urlObj.pathname.split('/').filter(Boolean);
            if (parts.length >= 2 && ['shorts', 'embed', 'v', 'live'].includes(parts[0])) {
                return parts[1].split('?')[0].split('&')[0].split('#')[0];
            }
        } else if (urlObj.hostname.includes('youtu.be')) {
            const parts = urlObj.pathname.split('/').filter(Boolean);
            if (parts.length >= 1) {
                return parts[0].split('?')[0].split('&')[0].split('#')[0];
            }
        }
    } catch {
        // ignore
    }
    return null;
}

function toCanonicalYoutubeUrl(videoId: string, sourceUrl?: string): { canonicalUrl: string; cleanVideoId: string } {
    let cleanVideoId = (sourceUrl ? parseYoutubeVideoId(sourceUrl) : null) || parseYoutubeVideoId(videoId) || videoId;

    cleanVideoId = cleanVideoId.split('?')[0].split('&')[0].split('#')[0].split('/')[0];
    const canonicalUrl = `https://www.youtube.com/watch?v=${cleanVideoId}`;

    return { canonicalUrl, cleanVideoId };
}

export class RapidApiProvider implements TranscriptProvider {
    async fetchTranscript(videoId: string, sourceUrl?: string): Promise<TranscriptProviderResponse> {
        const apiKey = Deno.env.get('RAPIDAPI_KEY');
        const apiHost = Deno.env.get('RAPIDAPI_HOST') || 'youtube-transcript3.p.rapidapi.com';

        if (!apiKey) {
            return {
                success: false,
                segments: [],
                error: 'RAPIDAPI_KEY secret is not configured in Supabase.',
                errorCode: 'MISSING_SECRET'
            };
        }

        const { canonicalUrl, cleanVideoId } = toCanonicalYoutubeUrl(videoId, sourceUrl);

        try {
            // Solid API / RapidAPI Youtube Transcript endpoints: GET with normalized canonical 'url' or 'cleanVideoId'
            const endpointsToTry = [
                `https://${apiHost}/api/transcript-with-url?url=${encodeURIComponent(canonicalUrl)}`,
                `https://${apiHost}/api/transcript?url=${encodeURIComponent(canonicalUrl)}`,
                `https://${apiHost}/api/transcript?videoId=${encodeURIComponent(cleanVideoId)}`
            ];

            let lastResponse: Response | null = null;
            let data: any = null;

            for (const url of endpointsToTry) {
                const controller = new AbortController();
                const timeoutId = setTimeout(() => controller.abort(), 12000); // 12 seconds timeout for external API hop

                try {
                    const res = await fetch(url, {
                        method: 'GET',
                        headers: {
                            'X-RapidAPI-Key': apiKey,
                            'X-RapidAPI-Host': apiHost
                        },
                        signal: controller.signal
                    });
                    clearTimeout(timeoutId);
                    lastResponse = res;

                    if (res.ok) {
                        data = await res.json();
                        break;
                    }

                    // If not 404, stop trying alternative endpoints
                    if (res.status !== 404) {
                        break;
                    }
                } catch (e: any) {
                    clearTimeout(timeoutId);
                    if (e.name === 'AbortError') {
                        lastResponse = null;
                        continue;
                    }
                    throw e;
                }
            }

            if (!lastResponse) {
                return {
                    success: false,
                    segments: [],
                    error: 'Provider request failed to initiate',
                    errorCode: 'PROVIDER_NETWORK_ERROR'
                };
            }

            if (!lastResponse.ok) {
                let errorCode = 'PROVIDER_HTTP_ERROR';
                let errorMessage = `Provider returned HTTP ${lastResponse.status}: ${lastResponse.statusText}`;

                if (lastResponse.status === 403 || lastResponse.status === 401) {
                    errorCode = 'PROVIDER_UNAUTHORIZED';
                } else if (lastResponse.status === 404) {
                    errorCode = 'TRANSCRIPT_NOT_FOUND';
                    errorMessage = 'No transcript available for this video or endpoint not found (HTTP 404)';
                }

                return {
                    success: false,
                    segments: [],
                    error: errorMessage,
                    errorCode: errorCode
                };
            }

            let segments: ProviderTranscriptSegment[] = [];
            let language = 'en';

            if (Array.isArray(data)) {
                segments = data.map((item: any) => ({
                    time: Number(item.offset ?? item.start ?? item.time ?? 0),
                    text: String(item.text ?? '')
                }));
            } else if (data && typeof data === 'object') {
                if (data.language || data.lang) {
                    language = String(data.language || data.lang);
                }
                const rawSegments = data.transcript || data.segments || data.result || data.data;
                if (Array.isArray(rawSegments)) {
                    segments = rawSegments.map((item: any) => ({
                        time: Number(item.offset ?? item.start ?? item.time ?? 0),
                        text: String(item.text ?? '')
                    }));
                } else if (typeof data.text === 'string' && data.text.trim().length > 0) {
                    segments = [{ time: 0, text: data.text.trim() }];
                }
            }

            if (segments.length === 0) {
                return {
                    success: false,
                    segments: [],
                    error: 'Provider returned empty or unrecognized transcript format',
                    errorCode: 'EMPTY_TRANSCRIPT'
                };
            }

            return {
                success: true,
                segments: segments,
                language: language
            };

        } catch (e: any) {
            const isTimeout = e.name === 'AbortError';
            return {
                success: false,
                segments: [],
                error: isTimeout ? 'Provider request timed out' : e.message,
                errorCode: isTimeout ? 'PROVIDER_TIMEOUT' : 'PROVIDER_NETWORK_ERROR'
            };
        }
    }
}
