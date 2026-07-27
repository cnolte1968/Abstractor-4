package com.example

import org.junit.Assert.assertEquals
import org.junit.Test

class FavoritesReorderingTest {

    data class DummyFunction(val id: String, val name: String)

    @Test
    fun testMoveFavoriteUpAndDownLogic() {
        var list = listOf("WEB_SUMMARY", "KEY_TAKEAWAYS", "FACTS_VS_OPINIONS")

        // Move "FACTS_VS_OPINIONS" up
        fun moveUp(id: String): List<String> {
            val current = list.toMutableList()
            val index = current.indexOf(id)
            if (index > 0) {
                val temp = current[index]
                current[index] = current[index - 1]
                current[index - 1] = temp
            }
            return current
        }

        fun moveDown(id: String): List<String> {
            val current = list.toMutableList()
            val index = current.indexOf(id)
            if (index in 0 until current.size - 1) {
                val temp = current[index]
                current[index] = current[index + 1]
                current[index + 1] = temp
            }
            return current
        }

        // 1. Move FACTS_VS_OPINIONS up once -> [WEB_SUMMARY, FACTS_VS_OPINIONS, KEY_TAKEAWAYS]
        list = moveUp("FACTS_VS_OPINIONS")
        assertEquals(listOf("WEB_SUMMARY", "FACTS_VS_OPINIONS", "KEY_TAKEAWAYS"), list)

        // 2. Move FACTS_VS_OPINIONS up again -> [FACTS_VS_OPINIONS, WEB_SUMMARY, KEY_TAKEAWAYS]
        list = moveUp("FACTS_VS_OPINIONS")
        assertEquals(listOf("FACTS_VS_OPINIONS", "WEB_SUMMARY", "KEY_TAKEAWAYS"), list)

        // 3. Move FACTS_VS_OPINIONS up at index 0 -> remains unchanged
        list = moveUp("FACTS_VS_OPINIONS")
        assertEquals(listOf("FACTS_VS_OPINIONS", "WEB_SUMMARY", "KEY_TAKEAWAYS"), list)

        // 4. Move FACTS_VS_OPINIONS down once -> [WEB_SUMMARY, FACTS_VS_OPINIONS, KEY_TAKEAWAYS]
        list = moveDown("FACTS_VS_OPINIONS")
        assertEquals(listOf("WEB_SUMMARY", "FACTS_VS_OPINIONS", "KEY_TAKEAWAYS"), list)

        // 5. Move KEY_TAKEAWAYS down at last index -> remains unchanged
        list = moveDown("KEY_TAKEAWAYS")
        assertEquals(listOf("WEB_SUMMARY", "FACTS_VS_OPINIONS", "KEY_TAKEAWAYS"), list)
    }

    @Test
    fun testOrderAuthoritativeMapping() {
        val allFunctions = mapOf(
            "WEB_SUMMARY" to DummyFunction("WEB_SUMMARY", "Zusammenfassung"),
            "KEY_TAKEAWAYS" to DummyFunction("KEY_TAKEAWAYS", "3 Kernaussagen"),
            "FACTS_VS_OPINIONS" to DummyFunction("FACTS_VS_OPINIONS", "Fakten vs. Meinungen")
        )

        val favoritesList = listOf("FACTS_VS_OPINIONS", "WEB_SUMMARY", "KEY_TAKEAWAYS")
        val mapped = favoritesList.mapNotNull { allFunctions[it] }

        assertEquals(3, mapped.size)
        assertEquals("FACTS_VS_OPINIONS", mapped[0].id)
        assertEquals("WEB_SUMMARY", mapped[1].id)
        assertEquals("KEY_TAKEAWAYS", mapped[2].id)
    }

    @Test
    fun testInvalidOrMissingIdsIgnoredWithoutDisruptingOrder() {
        val allFunctions = mapOf(
            "WEB_SUMMARY" to DummyFunction("WEB_SUMMARY", "Zusammenfassung"),
            "KEY_TAKEAWAYS" to DummyFunction("KEY_TAKEAWAYS", "3 Kernaussagen")
        )

        val storedFavoritesWithGarbage = listOf("KEY_TAKEAWAYS", "UNKNOWN_ID", "", "WEB_SUMMARY")
        val mapped = storedFavoritesWithGarbage.mapNotNull { allFunctions[it] }

        assertEquals(2, mapped.size)
        assertEquals("KEY_TAKEAWAYS", mapped[0].id)
        assertEquals("WEB_SUMMARY", mapped[1].id)
    }

    @Test
    fun testAddAndRemoveFavoriteOrderIntegrity() {
        var favorites = listOf("WEB_SUMMARY", "KEY_TAKEAWAYS")

        // Add new favorite
        fun toggle(id: String) {
            favorites = if (favorites.contains(id)) favorites - id else favorites + id
        }

        toggle("FACTS_VS_OPINIONS")
        assertEquals(listOf("WEB_SUMMARY", "KEY_TAKEAWAYS", "FACTS_VS_OPINIONS"), favorites)

        // Remove middle favorite
        toggle("KEY_TAKEAWAYS")
        assertEquals(listOf("WEB_SUMMARY", "FACTS_VS_OPINIONS"), favorites)
    }
}
