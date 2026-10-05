package com.hajun.kaloriku

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.LayoutDirection
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hajun.kaloriku.ui.navigation.HistoryRoute
import com.hajun.kaloriku.ui.navigation.HomeRoute
import com.hajun.kaloriku.ui.navigation.KaloriNavHost
import com.hajun.kaloriku.ui.navigation.MealDetailRoute
import com.hajun.kaloriku.ui.navigation.PAGE_TRANSITION_MILLIS
import com.hajun.kaloriku.ui.navigation.ProfileRoute
import com.hajun.kaloriku.ui.navigation.switchTab
import com.hajun.kaloriku.ui.theme.KaloriKuTheme
import com.hajun.kaloriku.ui.theme.Spacing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Menguji host produksi tanpa ViewModel, jaringan, atau penyimpanan pengguna. */
@RunWith(AndroidJUnit4::class)
class NavigationMotionUiTest {

    @get:Rule
    val rule = createComposeRule()

    private lateinit var navController: NavHostController

    @Test
    fun tabSwitches_slideAccordingToTabOrderInBothDirections() {
        showHost()
        withManualClock {
            assertSettled("home")
            val steps = listOf(
                Triple(HistoryRoute, "history", true),
                Triple(ProfileRoute, "profile", true),
                Triple(HistoryRoute, "history", false),
                Triple(HomeRoute, "home", false),
            )
            var from = "home"
            steps.forEach { (route, to, towardsLeft) ->
                advanceToMiddle { it.switchTab(route) }
                assertSliding(from, to, towardsLeft)
                finishTransition()
                assertSettled(to)
                rule.onNodeWithTag("screen_$from").assertDoesNotExist()
                from = to
            }
        }
    }

    @Test
    fun popFromHistory_slidesBackTowardsHome() {
        showHost()
        switchTabAndWait(HistoryRoute)
        withManualClock {
            advanceToMiddle { assertTrue(it.popBackStack()) }
            assertSliding("history", "home", towardsLeft = false)
            finishTransition()
            assertSettled("home")
            rule.onNodeWithTag("screen_history").assertDoesNotExist()
        }
    }

    @Test
    fun detailForwardAndPop_slideAndKeepTheHistoryDraft() {
        showHost()
        switchTabAndWait(HistoryRoute)
        replaceDraft("history", "Draf riwayat")
        withManualClock {
            advanceToMiddle { it.navigate(MealDetailRoute(entryId = 42L)) }
            assertSliding("history", "detail", towardsLeft = true)
            finishTransition()
            assertSettled("detail")
            rule.onNodeWithTag("screen_history").assertDoesNotExist()

            advanceToMiddle { assertTrue(it.popBackStack()) }
            assertSliding("detail", "history", towardsLeft = false)
            finishTransition()
            assertSettled("history")
            rule.onNodeWithTag("screen_detail").assertDoesNotExist()
            assertDraft("history", "Draf riwayat")
        }
    }

    @Test
    fun rememberSaveableDrafts_surviveRepeatedTabRestoration() {
        showHost()
        replaceDraft("home", "Draf beranda")
        switchTabAndWait(HistoryRoute)
        replaceDraft("history", "Draf riwayat")
        switchTabAndWait(ProfileRoute)
        replaceDraft("profile", "Draf profil")

        repeat(3) {
            switchTabAndWait(HomeRoute)
            assertDraft("home", "Draf beranda")
            switchTabAndWait(HistoryRoute)
            assertDraft("history", "Draf riwayat")
            switchTabAndWait(ProfileRoute)
            assertDraft("profile", "Draf profil")
        }
    }

    @Test
    fun reselectingActiveTab_doesNotRestartEntryOrDispatchNavigation() {
        showHost()
        replaceDraft("home", "Beranda tetap")
        assertReselectionIsIgnored(HomeRoute)
        assertDraft("home", "Beranda tetap")

        switchTabAndWait(HistoryRoute)
        replaceDraft("history", "Riwayat tetap")
        assertReselectionIsIgnored(HistoryRoute)
        assertDraft("history", "Riwayat tetap")

        switchTabAndWait(ProfileRoute)
        replaceDraft("profile", "Profil tetap")
        assertReselectionIsIgnored(ProfileRoute)
        assertDraft("profile", "Profil tetap")
    }

    private fun showHost() {
        rule.setContent {
            KaloriKuTheme(darkTheme = false) {
                // Arah kiri/kanan tidak bergantung pada bahasa perangkat penguji.
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    val controller = rememberNavController()
                    SideEffect { navController = controller }
                    Box(modifier = Modifier.fillMaxSize().clipToBounds().testTag("nav_viewport")) {
                        KaloriNavHost(navController = controller) {
                            composable<HomeRoute> {
                                DraftPage("home", "Beranda", MaterialTheme.colorScheme.surface)
                            }
                            composable<HistoryRoute> {
                                DraftPage("history", "Riwayat", MaterialTheme.colorScheme.surfaceContainerLow)
                            }
                            composable<ProfileRoute> {
                                DraftPage("profile", "Profil", MaterialTheme.colorScheme.primaryContainer)
                            }
                            composable<MealDetailRoute> {
                                DraftPage("detail", "Detail catatan", MaterialTheme.colorScheme.surfaceContainerHigh)
                            }
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
    }

    private fun withManualClock(assertions: () -> Unit) {
        rule.mainClock.autoAdvance = false
        try {
            assertions()
        } finally {
            rule.mainClock.autoAdvance = true
        }
    }

    private fun advanceToMiddle(navigate: (NavHostController) -> Unit) {
        rule.runOnIdle { navigate(navController) }
        // Frame pertama menjadwalkan animasi; frame berikutnya menetapkan waktu awalnya.
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()
        rule.mainClock.advanceTimeBy(64L)
        rule.waitForIdle()
    }

    private fun finishTransition() {
        rule.mainClock.advanceTimeBy(PAGE_TRANSITION_MILLIS.toLong() + 64L)
        rule.waitForIdle()
    }

    private fun assertSliding(from: String, to: String, towardsLeft: Boolean) {
        rule.waitForIdle()
        val viewport = rule.onNodeWithTag("nav_viewport").fetchSemanticsNode().boundsInRoot
        val outgoing = rule.onNodeWithTag("screen_$from").fetchSemanticsNode()
        val incoming = rule.onNodeWithTag("screen_$to").fetchSemanticsNode()
        val outgoingBounds = outgoing.boundsInRoot
        val incomingBounds = incoming.boundsInRoot
        val tolerance = 2f

        assertTrue("Wadah navigasi harus sudah diukur", (viewport.width > 0f) && (viewport.height > 0f))
        listOf(outgoing, incoming).forEach { node ->
            assertEquals("Halaman memakai lebar penuh", viewport.width, node.size.width.toFloat(), tolerance)
            assertEquals("Halaman memakai tinggi penuh", viewport.height, node.size.height.toFloat(), tolerance)
            assertEquals(viewport.top, node.boundsInRoot.top, tolerance)
            assertEquals(viewport.bottom, node.boundsInRoot.bottom, tolerance)
            assertTrue("Kedua halaman masih terlihat pada frame antara", node.boundsInRoot.width > tolerance)
            assertTrue("Pergeseran belum selesai", node.boundsInRoot.width < (viewport.width - tolerance))
        }

        // boundsInRoot terpotong oleh wadah; positionInRoot tetap menunjukkan sisi di luar layar.
        if (towardsLeft) {
            assertTrue("Halaman lama bergerak ke kiri", outgoing.positionInRoot.x < viewport.left)
            assertTrue("Halaman baru masuk dari kanan", incoming.positionInRoot.x > viewport.left)
            assertEquals(viewport.left, outgoingBounds.left, tolerance)
            assertEquals(viewport.right, incomingBounds.right, tolerance)
            assertTrue(outgoingBounds.right < (viewport.right - tolerance))
            assertTrue(incomingBounds.left > (viewport.left + tolerance))
            assertEquals("Tepi halaman berdampingan", outgoingBounds.right, incomingBounds.left, tolerance)
        } else {
            assertTrue("Halaman lama bergerak ke kanan", outgoing.positionInRoot.x > viewport.left)
            assertTrue("Halaman baru masuk dari kiri", incoming.positionInRoot.x < viewport.left)
            assertEquals(viewport.right, outgoingBounds.right, tolerance)
            assertEquals(viewport.left, incomingBounds.left, tolerance)
            assertTrue(outgoingBounds.left > (viewport.left + tolerance))
            assertTrue(incomingBounds.right < (viewport.right - tolerance))
            assertEquals("Tepi halaman berdampingan", incomingBounds.right, outgoingBounds.left, tolerance)
        }
    }

    private fun assertSettled(page: String) {
        rule.waitForIdle()
        val viewport = rule.onNodeWithTag("nav_viewport").fetchSemanticsNode().boundsInRoot
        val screen = rule.onNodeWithTag("screen_$page").fetchSemanticsNode().boundsInRoot
        assertSameBounds(viewport, screen)
    }

    private fun assertSameBounds(expected: Rect, actual: Rect) {
        assertEquals(expected.left, actual.left, 2f)
        assertEquals(expected.top, actual.top, 2f)
        assertEquals(expected.right, actual.right, 2f)
        assertEquals(expected.bottom, actual.bottom, 2f)
    }

    private fun switchTabAndWait(route: Any) {
        rule.runOnIdle { navController.switchTab(route) }
        rule.waitForIdle()
    }

    private fun replaceDraft(page: String, text: String) {
        rule.onNodeWithTag("input_$page").performTextReplacement(text)
        rule.onNodeWithTag("input_$page").performImeAction()
        rule.waitForIdle()
        assertDraft(page, text)
    }

    private fun assertDraft(page: String, expected: String) {
        rule.onNodeWithTag("input_$page").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString(expected)),
        )
    }

    private fun assertReselectionIsIgnored(route: Any) {
        var destinationChanges = 0
        val listener = NavController.OnDestinationChangedListener { _, _, _ -> destinationChanges++ }
        rule.runOnIdle { navController.addOnDestinationChangedListener(listener) }
        try {
            val entry = rule.runOnIdle { checkNotNull(navController.currentBackStackEntry) }
            val previousId = rule.runOnIdle { navController.previousBackStackEntry?.id }
            val changesBefore = rule.runOnIdle { destinationChanges }
            repeat(3) { switchTabAndWait(route) }
            rule.runOnIdle {
                assertSame("Tab aktif tidak dibuat ulang", entry, navController.currentBackStackEntry)
                assertEquals(entry.id, navController.currentBackStackEntry?.id)
                assertEquals(previousId, navController.previousBackStackEntry?.id)
                assertEquals("Tidak ada navigasi baru", changesBefore, destinationChanges)
            }
        } finally {
            rule.runOnIdle { navController.removeOnDestinationChangedListener(listener) }
        }
    }
}

@Composable
private fun DraftPage(tag: String, label: String, background: Color) {
    var draft by rememberSaveable { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    Box(
        modifier = Modifier.fillMaxSize().background(background).testTag("screen_$tag"),
        contentAlignment = Alignment.Center,
    ) {
        TextField(
            value = draft,
            onValueChange = { draft = it },
            label = { Text("Draf $label") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    keyboard?.hide()
                },
            ),
            modifier = Modifier.padding(Spacing.screen).testTag("input_$tag"),
        )
    }
}
