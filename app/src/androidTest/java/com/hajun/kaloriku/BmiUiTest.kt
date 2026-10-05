package com.hajun.kaloriku

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.toSize
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hajun.kaloriku.ui.components.BmiCard
import com.hajun.kaloriku.ui.theme.KaloriKuTheme
import com.hajun.kaloriku.ui.theme.Spacing
import com.hajun.kaloriku.util.calculateBmi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Nilai, semantik aksesibilitas, dan tata letak IMT tidak memerlukan ViewModel. */
@RunWith(AndroidJUnit4::class)
class BmiUiTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun nullAndInvalidBmi_showNoCategoryOrPretendMarker() {
        val bmi = mutableStateOf<Double?>(null)
        showBmi(bmi)

        listOf(null, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 0.0, -1.0).forEach { value ->
            rule.runOnIdle { bmi.value = value }
            rule.onNodeWithTag("bmi_value").assertTextEquals("Belum dihitung")
            rule.onNodeWithTag("bmi_status").assertDoesNotExist()
            assertScaleState("Belum dihitung; tanpa penanda")
            rule.onNodeWithText("Isi berat dan tinggi badan untuk menghitung IMT secara otomatis.").assertExists()
        }
    }

    @Test
    fun weight62Height172_showIndonesianNormalValueAndMarker() {
        showBmi(mutableStateOf(calculateBmi(weightKg = 62.0, heightCm = 172.0)))

        rule.onNodeWithTag("bmi_value").assertTextEquals("21,0").assertIsDisplayed()
        rule.onNodeWithTag("bmi_status").assertTextEquals("Normal").assertIsDisplayed()
        assertScaleState("Penanda IMT 21,0")
        rule.onNodeWithTag("bmi_scale").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf("Skala IMT 15 sampai 35")),
        )
    }

    @Test
    fun bmiAtOrAbove25_showObesityIncludingUpperScaleOverflow() {
        val bmi = mutableStateOf<Double?>(25.0)
        showBmi(bmi)
        val values = listOf(25.0 to "25,0", 30.0 to "30,0", 35.0 to "35,0", 40.0 to "40,0")

        values.forEach { (value, text) ->
            rule.runOnIdle { bmi.value = value }
            rule.onNodeWithTag("bmi_value").assertTextEquals(text)
            rule.onNodeWithTag("bmi_status").assertTextEquals("Obesitas")
            if (value > 35.0) {
                assertScaleState("IMT 40,0; penanda di batas atas skala")
                rule.onNodeWithText("Nilai IMT di luar skala 15–35; penanda ditampilkan di tepi skala.").assertExists()
            } else {
                assertScaleState("Penanda IMT $text")
            }
        }
    }

    @Test
    fun darkThemeAtFontScale15_keepsValuesStatusAndScaleInsideCard() {
        val bmi = mutableStateOf(calculateBmi(weightKg = 62.0, heightCm = 172.0))
        showBmi(bmi, darkTheme = true, fontScale = 1.5f)

        listOf(calculateBmi(62.0, 172.0), 25.0, null).forEach { value ->
            rule.runOnIdle { bmi.value = value }
            rule.waitForIdle()
            val valueNode = rule.onNodeWithTag("bmi_value", useUnmergedTree = true)
            valueNode.assertIsDisplayed()
            assertInsideCard("bmi_value")
            assertInsideCard("bmi_scale")
            assertNoTextOverflow(valueNode)
            assertNoTextOverflow(rule.onNodeWithText("Indeks massa tubuh (IMT)", useUnmergedTree = true))
            if (value != null) {
                val status = rule.onNodeWithTag("bmi_status", useUnmergedTree = true)
                status.assertIsDisplayed()
                assertInsideCard("bmi_status")
                assertNoTextOverflow(status)
            } else {
                rule.onNodeWithTag("bmi_status").assertDoesNotExist()
                assertScaleState("Belum dihitung; tanpa penanda")
            }
        }
    }

    private fun showBmi(bmi: State<Double?>, darkTheme: Boolean = false, fontScale: Float = 1f) {
        rule.setContent {
            val density = LocalDensity.current
            // Hanya lingkungan komposisi tes yang berubah; pengaturan emulator tetap utuh.
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = fontScale)) {
                KaloriKuTheme(darkTheme = darkTheme) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("bmi_container")
                            .verticalScroll(rememberScrollState())
                            .padding(Spacing.screen),
                    ) {
                        BmiCard(bmi = bmi.value, modifier = Modifier.testTag("bmi_card"))
                    }
                }
            }
        }
    }

    private fun assertScaleState(expected: String) {
        rule.onNodeWithTag("bmi_scale").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, expected),
        )
    }

    private fun assertInsideCard(tag: String) {
        rule.waitForIdle()
        val container = rule.onNodeWithTag("bmi_container", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val card = rule.onNodeWithTag("bmi_card", useUnmergedTree = true).fetchSemanticsNode()
        val child = rule.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode()
        val cardBounds = Rect(card.positionInRoot, card.size.toSize())
        val childBounds = Rect(child.positionInRoot, child.size.toSize())
        val tolerance = 2f

        // Batas tanpa clipping mencegah konten meluber terlihat lulus hanya karena terpotong.
        assertTrue("Kartu tidak melewati sisi kiri wadah", cardBounds.left >= (container.left - tolerance))
        assertTrue("Kartu tidak melewati sisi kanan wadah", cardBounds.right <= (container.right + tolerance))
        assertTrue("$tag sudah diukur", (childBounds.width > 0f) && (childBounds.height > 0f))
        assertTrue("$tag tetap terlihat", (child.boundsInRoot.width > 0f) && (child.boundsInRoot.height > 0f))
        assertTrue("$tag tidak melewati sisi kiri kartu", childBounds.left >= (cardBounds.left - tolerance))
        assertTrue("$tag tidak melewati sisi kanan kartu", childBounds.right <= (cardBounds.right + tolerance))
        assertTrue("$tag tidak melewati sisi atas kartu", childBounds.top >= (cardBounds.top - tolerance))
        assertTrue("$tag tidak melewati sisi bawah kartu", childBounds.bottom <= (cardBounds.bottom + tolerance))
    }

    private fun assertNoTextOverflow(node: SemanticsNodeInteraction) {
        val results = mutableListOf<TextLayoutResult>()
        node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { getResult ->
            assertTrue("Hasil tata letak teks tersedia", getResult(results))
        }
        assertEquals(1, results.size)
        assertFalse("Teks tidak terpotong pada font 1,5×", results.single().hasVisualOverflow)
    }
}
