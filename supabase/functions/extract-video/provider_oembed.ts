import { ProviderMetadata } from './types.ts';

export async function fetchOEmbedMetadata(url: string): Promise<ProviderMetadata | null> {
    try {
        const oembedUrl = `https://www.youtube.com/oembed?url=${encodeURIComponent(url)}&format=json`;
        
        const controller = new AbortController();
        const timeoutId = setTimeout(() => controller.abort(), 3000); // 3 seconds timeout

        const res = await fetch(oembedUrl, { signal: controller.signal });
        clearTimeout(timeoutId);

        if (!res.ok) {
            return null;
        }

        const data = await res.json();
        return {
            title: data.title,
            author: data.author_name
        };
    } catch (e) {
        return null; // Silent fail, just means we don't have fallback metadata
    }
}
