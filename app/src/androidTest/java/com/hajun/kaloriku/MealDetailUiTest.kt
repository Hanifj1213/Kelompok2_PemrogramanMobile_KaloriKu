package com.hajun.kaloriku

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hajun.kaloriku.ui.MainViewModel
import com.hajun.kaloriku.ui.navigation.MealDetailRoute
import com.hajun.kaloriku.ui.screen.MealDetailScreen
import com.hajun.kaloriku.ui.theme.KaloriKuTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Memastikan [MealDetailRoute] menerima argumen dan layarnya bisa tampil.
 * Test ini hanya perlu dikompilasi; penjalanan di perangkat tidak wajib.
 */
@RunWith(AndroidJUnit4::class)
class MealDetailUiTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun mealDetailRoute_withArgumentShowsContent() {
        rule.setContent {
            KaloriKuTheme {
                val viewModel: MainViewModel = viewModel()
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = MealDetailRoute(entryId = 0L)
                ) {
                    composable<MealDetailRoute> { entry ->
                        MealDetailScreen(
                            viewModel = viewModel,
                            backStackEntry = entry,
                            onBack = {}
                        )
                    }
                }
            }
        }

        // Catatan dengan id 0 tidak ada, jadi layar menampilkan keadaan kosong dengan aman.
        rule.onNodeWithText("Catatan").assertIsDisplayed()
        rule.onNodeWithText("Catatan ini sudah tidak ada.").assertIsDisplayed()
    }
}
