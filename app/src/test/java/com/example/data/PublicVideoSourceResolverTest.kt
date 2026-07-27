package com.example.data

import com.example.data.PublicVideoSourceResolver
import com.example.domain.model.VideoSourceType
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PublicVideoSourceResolverTest {
    @Test
    fun testYouTube() {
        val source = PublicVideoSourceResolver.resolve("https://youtube.com/watch?v=123")
        assertEquals(VideoSourceType.YouTube, source.type)
        assertEquals("YouTube", source.platform)
    }

    @Test
    fun testDirectVideo() {
        val source = PublicVideoSourceResolver.resolve("https://example.com/video.mp4")
        assertEquals(VideoSourceType.DirectVideo, source.type)
        assertEquals(".mp4", source.extension)
    }

    @Test
    fun testDirectSubtitle() {
        val source = PublicVideoSourceResolver.resolve("https://example.com/subs.vtt")
        assertEquals(VideoSourceType.DirectSubtitle, source.type)
        assertEquals(".vtt", source.extension)
    }

    @Test
    fun testPublicWebPage() {
        val source = PublicVideoSourceResolver.resolve("https://example.com/page")
        assertEquals(VideoSourceType.PublicWebPage, source.type)
    }

    @Test
    fun testUnsupported() {
        val source = PublicVideoSourceResolver.resolve("invalid-url")
        assertEquals(VideoSourceType.Unsupported, source.type)
        assertEquals("Unrecognized URL format", source.reason)
    }
}
