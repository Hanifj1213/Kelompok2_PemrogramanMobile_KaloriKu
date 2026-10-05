package com.hajun.kaloriku

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToKey
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hajun.kaloriku.data.DailyGoals
import com.hajun.kaloriku.data.FoodItem
import com.hajun.kaloriku.data.MealEntry
import com.hajun.kaloriku.data.Profile
import com.hajun.kaloriku.data.ReminderTimes
import com.hajun.kaloriku.ui.AnalysisState
import com.hajun.kaloriku.ui.MainViewModel
import com.hajun.kaloriku.ui.theme.KaloriKuTheme
import com.hajun.kaloriku.util.formatWhole
import com.hajun.kaloriku.util.toPlainInput
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Alur AppRoot nyata memakai TKPI lokal; foto, galeri, dan API tidak dipanggil. */
@RunWith(AndroidJUnit4::class)
class AppJourneyUiTest {

    @get:Rule
    val rule = createComposeRule()

    private lateinit var application: KaloriKuApp
    private lateinit var vm: MainViewModel
    private val haptics = JourneyHaptics()
    private var dismissKeyboard: () -> Unit = {}
    private val createdEntryIds = mutableSetOf<Long>()
    private var idsBeforePendingSave: Set<Long>? = null

    @Test
    fun lightTheme_profileDraftAndLocalMealJourney() {
        runJourney(darkTheme = false)
    }

    @Test
    fun darkTheme_profileDraftAndLocalMealJourney() {
        runJourney(darkTheme = true)
    }

    @After
    fun removeOnlyEntriesCreatedByThisTest() {
        if (!::vm.isInitialized) return
        // Tetap bersihkan hasil Simpan bila asersi navigasi gagal sebelum id sempat dicatat.
        rule.runOnUiThread {
            captureCreatedEntries()
            val presentIds = vm.entries.value.map { it.id }.toSet()
            createdEntryIds.filter { it in presentIds }.forEach(vm::deleteEntry)
            vm.cancelAnalysis()
            dismissKeyboard()
        }
    }

    private fun runJourney(darkTheme: Boolean) {
        showApp(darkTheme)
        val original = persistedState()

        tapTab("Riwayat")
        scrollLazyTo(hasText("7 hari terakhir")).assertIsDisplayed()
        tapTab("Profil")
        assertBmi("Belum dihitung", status = null)

        // Buka formulir data tubuh & aktivitas
        scrollLazyTo(hasText("Data tubuh & aktivitas")).assertIsDisplayed().performClick()
        rule.onNodeWithText("Menyesuaikan target kalori dengan tubuhmu").assertIsDisplayed()

        replaceProfileInput("Usia (tahun)", "25")
        replaceProfileInput("Berat badan (kg)", "62")
        replaceProfileInput("Tinggi badan (cm)", "172")
        scrollLazyTo(button("Simpan")).assertIsEnabled()

        val weight = scrollLazyTo(input("Berat badan (kg)"))
        weight.performTextClearance()
        hideKeyboard()
        assertEditableText(weight, "")
        scrollLazyTo(button("Simpan")).assertIsNotEnabled()
        assertEquals("Mengedit draf tidak menyimpan profil", original, persistedState())

        replaceProfileInput("Berat badan (kg)", "62")
        scrollLazyTo(button("Simpan")).assertIsEnabled()

        // Kembali ke layar Profil tanpa menyimpan
        rule.onNodeWithContentDescription("Kembali").assertIsDisplayed().performClick()
        rule.waitForIdle()
        scrollLazyTo(hasTestTag("bmi_value")).assertIsDisplayed()

        // Tidak menekan Simpan profil/target atau mengubah pengingat milik pengguna.
        tapTab("Beranda")
        val base = openLocalRiceDraft()
        chooseDoublePortion(base)
        val doubled = rule.runOnIdle { vm.editableItems.single().current }

        val grams = rule.onNode(input("Berat (g)")).performScrollTo()
        grams.performTextClearance()
        hideKeyboard()
        assertEditableText(grams, "")
        rule.onNodeWithText("Isi berat 1–3000 g sebelum menyimpan.").assertExists()
        rule.onNode(button("Simpan")).assertIsNotEnabled()
        rule.runOnIdle {
            assertEquals("Input kosong tidak mengubah gizi draf", doubled, vm.editableItems.single().current)
        }

        // Porsi yang sama juga harus memulihkan kolom kosong, bukan hanya porsi berbeda.
        chooseDoublePortion(base)
        rule.onNodeWithText("Isi berat 1–3000 g sebelum menyimpan.").assertDoesNotExist()
        makeDraftDistinctFromExistingEntries()
        val expectedSavedFood = rule.runOnIdle { vm.editableItems.single().current }
        val save = rule.onNode(button("Simpan")).assertIsEnabled()
        rule.runOnIdle { idsBeforePendingSave = vm.entries.value.map { it.id }.toSet() }
        clickWithTick(save)
        val savedEntry = rule.runOnIdle {
            val additions = captureCreatedEntries()
            assertEquals("Simpan menambah tepat satu catatan baru", 1, additions.size)
            idsBeforePendingSave = null
            additions.single()
        }
        assertEquals(listOf(expectedSavedFood), savedEntry.items)
        assertHomeWithoutDraft()

        tapTab("Riwayat")
        // Key berasal dari id yang benar-benar diterbitkan repository, bukan urutan global.
        verticalLazyList().performScrollToKey(savedEntry.id)
        rule.onNode(entryCard(savedEntry)).performScrollTo().assertIsDisplayed().performClick()
        rule.waitForIdle()
        scrollLazyTo(hasText(expectedSavedFood.name)).assertIsDisplayed()
        scrollLazyTo(button("Hapus catatan")).assertIsEnabled().performClick()
        rule.onNodeWithText("Hapus catatan?").assertIsDisplayed()
        rule.onNode(button("Hapus")).assertIsEnabled().performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Hapus catatan?").assertDoesNotExist()
        rule.onNodeWithTag("tab_Riwayat").assertIsSelected()
        rule.onNode(entryCard(savedEntry)).assertDoesNotExist()
        rule.runOnIdle {
            assertFalse("Id catatan uji sudah dihapus lewat dialog", vm.entries.value.any { it.id == savedEntry.id })
        }
        assertEquals("Seluruh data lama tetap utuh setelah hapus", original, persistedState())

        tapTab("Beranda")
        openLocalRiceDraft()
        // Tombol produksi dipakai; Back perangkat dapat menutup Activity milik createComposeRule.
        rule.onNodeWithContentDescription("Kembali").assertIsDisplayed().performClick()
        assertHomeWithoutDraft()
        assertEquals("Kembali membatalkan draf tanpa menyimpan", original, persistedState())
    }

    private fun showApp(darkTheme: Boolean) {
        application = ApplicationProvider.getApplicationContext()
        rule.setContent {
            val actualViewModel: MainViewModel = viewModel(
                factory = ViewModelProvider.AndroidViewModelFactory.getInstance(application),
            )
            val focusManager = LocalFocusManager.current
            val keyboard = LocalSoftwareKeyboardController.current
            SideEffect {
                vm = actualViewModel
                dismissKeyboard = {
                    focusManager.clearFocus(force = true)
                    keyboard?.hide()
                }
            }
            KaloriKuTheme(darkTheme = darkTheme) {
                CompositionLocalProvider(LocalHapticFeedback provides haptics) {
                    AppRoot(viewModel = actualViewModel)
                }
            }
        }
        rule.waitForIdle()
        rule.onNodeWithTag("tab_Beranda").assertIsSelected()
    }

    private fun openLocalRiceDraft(): FoodItem {
        clickWithTick(rule.onNodeWithContentDescription("Catat makanan"))
        rule.onNodeWithText("Cari di daftar makanan").performScrollTo().assertIsDisplayed().performClick()
        rule.onNodeWithText("Cari makanan").assertIsDisplayed()
        rule.onNode(hasSetTextAction()).performTextReplacement("Nasi putih")
        hideKeyboard()
        val riceRow = hasText("Nasi putih") and hasClickAction() and !hasSetTextAction()
        scrollLazyTo(riceRow).assertIsDisplayed().performClick()

        // Takaran pertama foods.csv adalah satu piring; tidak memasukkan fixture ke ViewModel.
        val grams = rule.onNode(input("Berat (g)")).performScrollTo()
        assertEditableText(grams, "150")
        rule.onNodeWithText("1 piring · 150 g").assertIsSelected()
        rule.onNode(hasText("Tambahkan ·", substring = true) and hasClickAction())
            .performScrollTo().assertIsEnabled().performClick()
        rule.onNodeWithText("Hasil analisis").assertIsDisplayed()
        rule.onNode(button("Simpan")).assertIsEnabled()
        return rule.runOnIdle {
            val state = vm.analysisState
            assertTrue("Data lokal menghasilkan state sukses", state is AnalysisState.Success)
            assertTrue((state as AnalysisState.Success).result.note.contains("TKPI"))
            vm.editableItems.single().base.also {
                assertEquals("Nasi putih", it.name)
                assertEquals(150.0, it.grams, 0.0)
                assertEquals(270.0, it.calories, 0.0)
            }
        }
    }

    private fun chooseDoublePortion(base: FoodItem) {
        clickWithTick(rule.onNodeWithText("2").performScrollTo())
        val expectedGrams = base.grams * 2
        val expectedCalories = base.calories * 2
        assertEditableText(rule.onNode(input("Berat (g)")), expectedGrams.toPlainInput())
        rule.onNodeWithText("${expectedGrams.formatWhole()} g · ${expectedCalories.formatWhole()} kkal")
            .performScrollTo().assertIsDisplayed()
        rule.onNode(button("Simpan")).assertIsEnabled()
        rule.runOnIdle {
            val item = vm.editableItems.single()
            assertEquals(2.0, item.portion, 0.0)
            assertEquals(expectedGrams, item.current.grams, 0.0)
            assertEquals(expectedCalories, item.current.calories, 0.0)
        }
    }

    private fun makeDraftDistinctFromExistingEntries() {
        val uniqueGrams = rule.runOnIdle {
            val item = vm.editableItems.single()
            val occupiedCalories = vm.entries.value
                .filter { it.items.joinToString { food -> food.name } == item.base.name }
                .map { it.totalCalories.formatWhole() }
                .toSet()
            if (item.current.calories.formatWhole() !in occupiedCalories) {
                null
            } else {
                // Kartu belum memiliki tag id: bedakan catatan uji agar tidak menghapus nasi lama.
                val start = item.current.grams.toInt().coerceIn(1, 3000)
                val candidates = (start..3000).asSequence() + (1 until start).asSequence()
                checkNotNull(candidates.firstOrNull { grams ->
                    item.base.scaled(grams.toDouble() / item.base.grams).calories.formatWhole() !in occupiedCalories
                }) { "Tidak ada berat unik yang aman untuk membuka catatan uji." }
            }
        }
        if (uniqueGrams != null) {
            val field = rule.onNode(input("Berat (g)")).performScrollTo()
            field.performTextReplacement(uniqueGrams.toString())
            hideKeyboard()
            assertEditableText(field, uniqueGrams.toString())
            rule.onNode(button("Simpan")).assertIsEnabled()
            rule.runOnIdle {
                assertEquals(uniqueGrams.toDouble(), vm.editableItems.single().current.grams, 0.0001)
            }
        }
    }

    private fun assertHomeWithoutDraft() {
        rule.waitForIdle()
        rule.onNodeWithTag("tab_Beranda").assertIsSelected().assertIsDisplayed()
        rule.onNodeWithText("Hasil analisis").assertDoesNotExist()
        rule.runOnIdle {
            assertEquals(AnalysisState.Idle, vm.analysisState)
            assertTrue("Draf dibersihkan", vm.editableItems.isEmpty())
        }
    }

    private fun tapTab(label: String) {
        clickWithTick(rule.onNodeWithTag("tab_$label"))
        rule.onNodeWithTag("tab_$label").assertIsSelected()
    }

    private fun clickWithTick(node: SemanticsNodeInteraction) {
        val before = rule.runOnIdle { haptics.events.size }
        node.assertIsDisplayed().assertIsEnabled().performClick()
        rule.waitForIdle()
        rule.runOnIdle {
            assertEquals("Aksi produksi mengirim satu tick", listOf(HapticFeedbackType.VirtualKey), haptics.events.drop(before))
        }
    }

    private fun replaceProfileInput(label: String, value: String) {
        val field = scrollLazyTo(input(label))
        field.performTextReplacement(value)
        hideKeyboard()
        assertEditableText(field, value)
    }

    private fun assertBmi(value: String, status: String?) {
        scrollLazyTo(hasTestTag("bmi_value")).assertTextEquals(value).assertIsDisplayed()
        if (status == null) {
            rule.onNodeWithTag("bmi_status").assertDoesNotExist()
        } else {
            scrollLazyTo(hasTestTag("bmi_status")).assertTextEquals(status).assertIsDisplayed()
        }
    }

    private fun hideKeyboard() {
        rule.runOnIdle { dismissKeyboard() }
        rule.waitForIdle()
    }

    private fun input(label: String) = hasText(label) and hasSetTextAction()

    private fun button(text: String) = hasText(text) and hasClickAction()

    private fun entryCard(entry: MealEntry) =
        hasText(entry.items.joinToString { it.name }) and hasText(entry.totalCalories.formatWhole()) and hasClickAction()

    private fun verticalLazyList() = rule.onNode(
        SemanticsMatcher.keyIsDefined(SemanticsActions.ScrollToIndex) and
            SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange),
    )

    private fun scrollLazyTo(matcher: SemanticsMatcher): SemanticsNodeInteraction {
        verticalLazyList().performScrollToNode(matcher)
        return rule.onNode(matcher).performScrollTo()
    }

    private fun assertEditableText(field: SemanticsNodeInteraction, expected: String) {
        field.assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString(expected)))
    }

    // Dipanggil hanya di thread UI; snapshot sebelum Simpan tidak mencakup catatan lama.
    private fun captureCreatedEntries(): List<MealEntry> {
        val before = idsBeforePendingSave ?: return emptyList()
        return vm.entries.value.filterNot { it.id in before }.also { additions ->
            createdEntryIds += additions.map { it.id }
        }
    }

    private fun persistedState() = rule.runOnIdle {
        PersistedState(
            entries = vm.entries.value.toList(),
            profile = vm.profile.value,
            goals = application.container.repository.goals.value,
            remindersEnabled = vm.remindersEnabled.value,
            reminderTimes = vm.reminderTimes.value,
        )
    }

    private data class PersistedState(
        val entries: List<MealEntry>,
        val profile: Profile?,
        val goals: DailyGoals?,
        val remindersEnabled: Boolean,
        val reminderTimes: ReminderTimes,
    )

    private class JourneyHaptics : HapticFeedback {
        val events = mutableListOf<HapticFeedbackType>()

        override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
            events += hapticFeedbackType
        }
    }
}
