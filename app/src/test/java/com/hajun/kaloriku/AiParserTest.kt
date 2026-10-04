package com.hajun.kaloriku

import com.hajun.kaloriku.data.AiException
import com.hajun.kaloriku.data.AiParser
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiParserTest {

    @Test
    fun extractText_readsMessageContent() {
        val body = """
            {"choices":[{"message":{"role":"assistant","content":"{\"is_food\":true,\"items\":[]}"}}]}
        """.trimIndent()
        assertEquals("{\"is_food\":true,\"items\":[]}", AiParser.extractText(body))
    }

    @Test
    fun extractText_skipsReasoningParts() {
        val parts = JSONArray()
            .put(JSONObject().put("type", "reasoning").put("text", "berpikir..."))
            .put(JSONObject().put("type", "text").put("text", "{\"is_food\":true,\"items\":[]}"))
        val body = JSONObject()
            .put("choices", JSONArray().put(JSONObject().put("message", JSONObject().put("content", parts))))
            .toString()
        assertEquals("{\"is_food\":true,\"items\":[]}", AiParser.extractText(body))
    }

    @Test(expected = AiException::class)
    fun extractText_noChoices_throws() {
        AiParser.extractText("""{"error":{"message":"boom"}}""")
    }

    @Test(expected = AiException::class)
    fun extractText_emptyContent_throws() {
        AiParser.extractText("""{"choices":[{"message":{"role":"assistant","content":""}}]}""")
    }

    @Test
    fun parseAnalysis_readsItems() {
        val text = """
            {"is_food":true,"items":[
              {"name":"Nasi putih","grams":150,"calories":195,"protein_g":4.1,"carbs_g":42.3,"fat_g":0.3},
              {"name":"Ayam goreng","grams":80,"calories":230,"protein_g":20,"carbs_g":5,"fat_g":14}
            ],"note":"Porsi sedang"}
        """.trimIndent()
        val result = AiParser.parseAnalysis(text)
        assertTrue(result.isFood)
        assertEquals(2, result.items.size)
        assertEquals("Nasi putih", result.items[0].name)
        assertEquals(425.0, result.items.sumOf { it.calories }, 0.001)
        assertEquals("Porsi sedang", result.note)
    }

    @Test
    fun parseAnalysis_handlesMarkdownFenceAndBadNumbers() {
        val text = """
            ```json
            {"is_food":true,"items":[{"name":"Sate","grams":"abc","calories":-10,"protein_g":5,"carbs_g":2,"fat_g":3},
              {"name":"","grams":10,"calories":10,"protein_g":0,"carbs_g":0,"fat_g":0}]}
            ```
        """.trimIndent()
        val result = AiParser.parseAnalysis(text)
        assertEquals(1, result.items.size)
        assertEquals(0.0, result.items[0].grams, 0.0)
        assertEquals(0.0, result.items[0].calories, 0.0)
    }

    @Test
    fun parseAnalysis_notFood() {
        val result = AiParser.parseAnalysis("""{"is_food":false,"items":[],"note":"Ini foto kucing"}""")
        assertFalse(result.isFood)
        assertTrue(result.items.isEmpty())
    }

    @Test(expected = AiException::class)
    fun parseAnalysis_garbage_throws() {
        AiParser.parseAnalysis("maaf saya tidak bisa")
    }

    @Test
    fun extractErrorMessage() {
        assertEquals("API key not valid", AiParser.extractErrorMessage("""{"error":{"message":"API key not valid"}}"""))
        assertNull(AiParser.extractErrorMessage("<html>"))
    }

    @Test
    fun extractErrorMessage_supportsStringError() {
        assertEquals(
            "API key required for remote API access",
            AiParser.extractErrorMessage("""{"error":"API key required for remote API access"}""")
        )
    }
}
