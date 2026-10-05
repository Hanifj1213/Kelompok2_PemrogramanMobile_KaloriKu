package com.hajun.kaloriku

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.AnnotatedString
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hajun.kaloriku.data.FoodItem
import com.hajun.kaloriku.ui.EditableItem
import com.hajun.kaloriku.ui.components.KaloriBottomBar
import com.hajun.kaloriku.ui.navigation.HistoryRoute
import com.hajun.kaloriku.ui.navigation.HomeRoute
import com.hajun.kaloriku.ui.navigation.ProfileRoute
import com.hajun.kaloriku.ui.screen.FoodItemCard
import com.hajun.kaloriku.ui.screen.SaveBar
import com.hajun.kaloriku.ui.theme.KaloriKuTheme
import com.hajun.kaloriku.ui.theme.Spacing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Umpan balik komponen produksi diuji dengan state lokal dan perekam haptic. */
@RunWith(AndroidJUnit4::class)
class UiFeedbackRegressionTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun bottomTabs_tickOnceOnChangeAndIgnoreActiveTab() {
        val haptics = RecordingHaptics()
        val selected = mutableStateOf<Any>(HomeRoute)
        val selections = mutableListOf<Any>()
        rule.setContent {
            KaloriKuTheme(darkTheme = false) {
                CompositionLocalProvider(LocalHapticFeedback provides haptics) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                        KaloriBottomBar(selectedRoute = selected.value) {
                            selections += it
                            selected.value = it
                        }
                    }
                }
            }
        }

        rule.onNodeWithTag("tab_Beranda").assertIsSelected().performClick()
        rule.runOnIdle {
            assertTrue(selections.isEmpty())
            assertTrue(haptics.events.isEmpty())
        }

        val changes = listOf(
            "tab_Riwayat" to HistoryRoute,
            "tab_Profil" to ProfileRoute,
            "tab_Beranda" to HomeRoute,
        )
        val expectedRoutes = changes.map { it.second }
        changes.forEachIndexed { index, (tag, _) ->
            rule.onNodeWithTag(tag).performClick().assertIsSelected()
            rule.runOnIdle {
                assertEquals(expectedRoutes.take(index + 1), selections.toList())
                assertEquals(List(index + 1) { HapticFeedbackType.VirtualKey }, haptics.events.toList())
            }
            repeat(2) { rule.onNodeWithTag(tag).performClick() }
            rule.runOnIdle {
                assertEquals("Tab aktif tidak memanggil callback", index + 1, selections.size)
                assertEquals("Tab aktif tidak bergetar", index + 1, haptics.events.size)
            }
        }
    }

    @Test
    fun disabledSaveBar_ignoresTouchWithoutCallbackOrHaptic() {
        val haptics = RecordingHaptics()
        var saveCalls = 0
        showSaveBar(enabled = false, haptics = haptics) { saveCalls++ }

        // Sentuhan nyata memeriksa tombol nonaktif tanpa memaksa aksi semantik OnClick.
        rule.onNodeWithText("Simpan").assertIsNotEnabled().performTouchInput { click() }
        rule.runOnIdle {
            assertEquals(0, saveCalls)
            assertTrue(haptics.events.isEmpty())
        }
    }

    @Test
    fun enabledSaveBar_callsSaveOnceAndEmitsOneCompatibleTick() {
        val haptics = RecordingHaptics()
        var saveCalls = 0
        showSaveBar(enabled = true, haptics = haptics) { saveCalls++ }

        rule.onNodeWithText("Simpan").assertIsEnabled().performClick()
        rule.runOnIdle {
            assertEquals(1, saveCalls)
            assertEquals(listOf(HapticFeedbackType.VirtualKey), haptics.events.toList())
        }
    }

    @Test
    fun quickPortions_recoverBlankGramsAndSynchronizeGramsCaloriesAndTicks() {
        val editor = EditorFixture()
        showFoodEditor(editor)
        val gramsField = rule.onNode(hasSetTextAction())
        gramsField.performTextClearance()
        assertEditableText(gramsField, "")
        rule.onNodeWithText("Simpan").assertIsNotEnabled()

        val portions = listOf(
            Triple(0.5, "½", 50),
            Triple(1.0, "1", 100),
            Triple(1.5, "1½", 150),
            Triple(2.0, "2", 200),
        )
        val expectedFactors = portions.map { it.first }
        portions.forEachIndexed { index, (factor, label, grams) ->
            rule.onNodeWithText(label).performScrollTo().performClick()
            assertEditableText(gramsField, grams.toString())
            rule.onNodeWithText("$grams g · ${grams * 2} kkal").assertExists()
            rule.onNodeWithText("Simpan").assertIsEnabled()
            rule.runOnIdle {
                assertEquals(factor, editor.item.value.portion, 0.0)
                assertTrue(editor.isValid.value)
                assertEquals(expectedFactors.take(index + 1), editor.portionChanges.toList())
                assertEquals(List(index + 1) { HapticFeedbackType.VirtualKey }, editor.haptics.events.toList())
                assertTrue("Sinkronisasi porsi tidak mengirim edit gram terpisah", editor.gramsChanges.isEmpty())
                assertEquals(0, editor.saveCalls)
            }
        }
    }

    @Test
    fun gramsEditing_allowsBlankAndInvalidTextAndOnlyCommitsValidNumbers() {
        val editor = EditorFixture()
        showFoodEditor(editor)
        val gramsField = rule.onNode(hasSetTextAction())
        assertEditableText(gramsField, "100")
        rule.onNodeWithText("Simpan").assertIsEnabled()

        gramsField.performTextClearance()
        assertInvalidGrams(editor, gramsField, "")
        rule.onNodeWithText("Simpan").performScrollTo().performTouchInput { click() }
        listOf("0", "3001", "abc").forEach { input ->
            gramsField.performTextReplacement(input)
            assertInvalidGrams(editor, gramsField, input)
        }
        rule.runOnIdle {
            assertEquals(0, editor.saveCalls)
            assertTrue(editor.haptics.events.isEmpty())
        }

        gramsField.performTextReplacement("250")
        assertEditableText(gramsField, "250")
        rule.onNodeWithText("250 g · 500 kkal").assertExists()
        rule.onNodeWithText("Isi berat 1–3000 g sebelum menyimpan.").assertDoesNotExist()
        rule.onNodeWithText("Simpan").assertIsEnabled().performScrollTo().performClick()
        rule.runOnIdle {
            assertTrue(editor.isValid.value)
            assertEquals(listOf(250.0), editor.gramsChanges.toList())
            assertEquals(2.5, editor.item.value.portion, 0.0)
            assertEquals(1, editor.saveCalls)
            assertEquals(listOf(HapticFeedbackType.VirtualKey), editor.haptics.events.toList())
        }
    }

    private fun showSaveBar(enabled: Boolean, haptics: RecordingHaptics, onSave: () -> Unit) {
        rule.setContent {
            KaloriKuTheme(darkTheme = false) {
                CompositionLocalProvider(LocalHapticFeedback provides haptics) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                        SaveBar(calories = 200.0, enabled = enabled, onSave = onSave)
                    }
                }
            }
        }
    }

    private fun showFoodEditor(editor: EditorFixture) {
        rule.setContent {
            KaloriKuTheme(darkTheme = false) {
                CompositionLocalProvider(LocalHapticFeedback provides editor.haptics) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(Spacing.screen),
                        verticalArrangement = Arrangement.spacedBy(Spacing.md),
                    ) {
                        FoodItemCard(
                            item = editor.item.value,
                            onPortionChange = { factor ->
                                editor.portionChanges += factor
                                editor.item.value = editor.item.value.copy(portion = factor)
                            },
                            onGramsChange = { grams ->
                                editor.gramsChanges += grams
                                editor.item.value = editor.item.value.copy(portion = grams / editor.item.value.base.grams)
                            },
                            onRemove = {},
                        ) { editor.isValid.value = it }
                        SaveBar(
                            calories = editor.item.value.current.calories,
                            enabled = editor.isValid.value,
                        ) { editor.saveCalls++ }
                    }
                }
            }
        }
    }

    private fun assertInvalidGrams(editor: EditorFixture, field: SemanticsNodeInteraction, expected: String) {
        assertEditableText(field, expected)
        rule.onNodeWithText("Isi berat 1–3000 g sebelum menyimpan.").assertExists()
        rule.onNodeWithText("Simpan").assertIsNotEnabled()
        rule.runOnIdle {
            assertFalse(editor.isValid.value)
            assertEquals("Edit tidak valid tidak mengubah berat tersimpan dalam draf", 100.0, editor.item.value.current.grams, 0.0)
            assertEquals(200.0, editor.item.value.current.calories, 0.0)
            assertTrue(editor.gramsChanges.isEmpty())
            assertTrue(editor.portionChanges.isEmpty())
        }
    }

    private fun assertEditableText(field: SemanticsNodeInteraction, expected: String) {
        field.assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString(expected)))
    }

    private class EditorFixture {
        val item = mutableStateOf(
            EditableItem(
                base = FoodItem(
                    name = "Makanan uji",
                    grams = 100.0,
                    calories = 200.0,
                    proteinG = 10.0,
                    carbsG = 30.0,
                    fatG = 5.0,
                ),
                id = 99L,
            ),
        )
        val isValid = mutableStateOf(value = true)
        val haptics = RecordingHaptics()
        val portionChanges = mutableListOf<Double>()
        val gramsChanges = mutableListOf<Double>()
        var saveCalls = 0
    }
}

private class RecordingHaptics : HapticFeedback {
    val events = mutableListOf<HapticFeedbackType>()

    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
        events += hapticFeedbackType
    }
}
