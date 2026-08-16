import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { ExtractVideoRequest } from "./types.ts";
import { createErrorResponse, mapProviderResponse } from "./contract_mapper.ts";
import { RapidApiProvider } from "./provider_rapidapi.ts";
import { fetchOEmbedMetadata } from "./provider_oembed.ts";

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
};

// Extremely basic extraction of video ID. In production, use a robust parser.
function extractVideoId(url: string): string | null {
    if (!url) return null;
    try {
        const regex = /(?:youtube\.com\/(?:[^\/]+\/\S+\/|(?:v|e(?:mbed)?|shorts|live)\/|\S*?[?&]v=)|youtu\.be\/)([a-zA-Z0-9_-]{11})/;
        const match = url.match(regex);
        if (match && match[1]) {
            return match[1];
        }

        const urlObj = new URL(url.startsWith('http') ? url : `https://${url}`);
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
        return null;
    } catch {
        return null;
    }
}

serve(async (req) => {
  // CORS Preflight
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders });
  }

  try {
    if (req.method !== 'POST') {
        throw new Error('Method not allowed');
    }

    const requestData: ExtractVideoRequest = await req.json();

    // Minimal Request Validation
    if (!requestData.sourceUrl) {
        return new Response(
            JSON.stringify(createErrorResponse('UNKNOWN', 'INVALID_REQUEST', 'Missing sourceUrl')),
            { headers: { ...corsHeaders, 'Content-Type': 'application/json' }, status: 400 }
        );
    }

    const startTime = Date.now();
    
    // 1. Extract Video ID
    const videoId = extractVideoId(requestData.sourceUrl);
    if (!videoId) {
         return new Response(
            JSON.stringify(createErrorResponse(requestData.sourcePlatform || 'UNKNOWN', 'INVALID_URL', 'Could not extract video ID from URL')),
            { headers: { ...corsHeaders, 'Content-Type': 'application/json' }, status: 400 }
        );
    }

    // 2. Fetch Transcript and Metadata (can be done in parallel)
    const provider = new RapidApiProvider();
    
    const [transcriptResult, metadataResult] = await Promise.all([
        provider.fetchTranscript(videoId, requestData.sourceUrl),
        fetchOEmbedMetadata(requestData.sourceUrl)
    ]);

    const processingTimeMs = Date.now() - startTime;

    // 3. Map to Contract Response
    const responseData = mapProviderResponse(requestData, transcriptResult, metadataResult, processingTimeMs);

    return new Response(
      JSON.stringify(responseData),
      { headers: { ...corsHeaders, 'Content-Type': 'application/json' }, status: 200 }
    );
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : String(error);
    return new Response(
      JSON.stringify(createErrorResponse('UNKNOWN', 'INTERNAL_SERVER_ERROR', errorMessage)),
      { headers: { ...corsHeaders, 'Content-Type': 'application/json' }, status: 500 }
    );
  }
});
