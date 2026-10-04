package com.hajun.kaloriku

import com.hajun.kaloriku.ui.components.rememberPhotoInput
import com.hajun.kaloriku.data.MealType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.compose.BackHandler
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hajun.kaloriku.notification.MealReminderWorker
import com.hajun.kaloriku.ui.MainViewModel
import com.hajun.kaloriku.ui.components.AddMealSheet
import com.hajun.kaloriku.ui.components.KaloriBottomBar
import com.hajun.kaloriku.ui.components.bottomTabs
import com.hajun.kaloriku.ui.screen.BarcodeScreen
import com.hajun.kaloriku.ui.screen.FoodSearchScreen
import com.hajun.kaloriku.ui.screen.GoalsScreen
import com.hajun.kaloriku.ui.screen.HistoryScreen
import com.hajun.kaloriku.ui.screen.HomeScreen
import com.hajun.kaloriku.ui.screen.ProfileScreen
import com.hajun.kaloriku.ui.screen.ResultScreen
import com.hajun.kaloriku.ui.screen.VoiceScreen
import com.hajun.kaloriku.ui.screen.WeeklyScreen
import com.hajun.kaloriku.ui.screen.WeightScreen
import com.hajun.kaloriku.ui.theme.KaloriKuTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KaloriKuTheme {
                AppRoot()
            }
        }
    }
}

object Routes {
    const val HOME = "home"
    const val HISTORY = "history"
    const val WEEKLY = "weekly"
    const val PROFILE = "profile"
    const val RESULT = "result"
    const val SEARCH = "search"
    const val BARCODE = "barcode"
    const val VOICE = "voice"
    const val WEIGHT = "weight"
    const val GOALS = "goals"
}

private val tabRoutes = bottomTabs.map { it.route }.toSet()

/**
 * Kerangka aplikasi: satu Scaffold dengan bilah navigasi bawah, lalu NavHost untuk isinya.
 * Layar detail menyembunyikan bilah bawah supaya ruangnya lega.
 */
@Composable
fun AppRoot(viewModel: MainViewModel = viewModel()) {
    val navController = rememberNavController()
    val context = LocalContext.current

    val remindersEnabled by viewModel.remindersEnabled.collectAsStateWithLifecycle()
    LaunchedEffect(remindersEnabled) {
        if (remindersEnabled) MealReminderWorker.scheduleAll(context) else MealReminderWorker.cancelAll(context)
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute == null || currentRoute in tabRoutes

    var showAddSheet by rememberSaveable { mutableStateOf(false) }
    var requestedMeal by rememberSaveable { mutableStateOf<MealType?>(null) }
    val photoInput = rememberPhotoInput(
        onSelected = { uri ->
            val meal = requestedMeal
            viewModel.analyze(uri)
            if (meal != null) viewModel.mealType = meal
            requestedMeal = null
            navController.openResult()
        },
        onError = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
    )

    fun openSearch(newMeal: Boolean) {
        val meal = requestedMeal
        if (newMeal) viewModel.startManualMeal()
        if (meal != null) viewModel.mealType = meal
        requestedMeal = null
        navController.navigate(Routes.SEARCH) { launchSingleTop = true }
    }

    BackHandler(enabled = currentRoute != null && currentRoute != Routes.HOME && !showAddSheet) {
        when (currentRoute) {
            Routes.RESULT -> {
                viewModel.cancelAnalysis()
                navController.goHome()
            }
            Routes.BARCODE -> {
                viewModel.resetBarcode()
                navController.popBackStackOrHome()
            }
            Routes.VOICE -> {
                viewModel.resetVoice()
                navController.popBackStackOrHome()
            }
            else -> navController.popBackStackOrHome()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Inset ditangani masing-masing layar, jadi Scaffold ini tidak menambah padding sendiri.
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                AnimatedVisibility(
                    visible = showBottomBar,
                    enter = slideInVertically(tween(220)) { it } + fadeIn(tween(220)),
                    exit = slideOutVertically(tween(180)) { it } + fadeOut(tween(120))
                ) {
                    KaloriBottomBar(
                        currentRoute = currentRoute,
                        onSelect = { route -> navController.switchTab(route) },
                        onAdd = { requestedMeal = null; showAddSheet = true }
                    )
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Routes.HOME,
                modifier = Modifier.padding(innerPadding),
                enterTransition = { fadeIn(tween(180)) },
                exitTransition = { fadeOut(tween(140)) },
                popEnterTransition = { fadeIn(tween(180)) },
                popExitTransition = { fadeOut(tween(140)) }
            ) {
                composable(Routes.HOME) {
                    HomeScreen(
                        viewModel = viewModel,
                        onCamera = { requestedMeal = null; photoInput.takePhoto() },
                        onGallery = { requestedMeal = null; photoInput.pickGallery() },
                        onAddMeal = { type -> requestedMeal = type; showAddSheet = true },
                        onOpenProfile = { navController.switchTab(Routes.PROFILE) },
                        onOpenHistory = { navController.switchTab(Routes.HISTORY) },
                        onOpenSearch = { requestedMeal = null; openSearch(newMeal = true) },
                        onOpenBarcode = { navController.navigate(Routes.BARCODE) },
                        onOpenVoice = { navController.navigate(Routes.VOICE) },
                        onOpenGoals = { navController.navigate(Routes.GOALS) }
                    )
                }
                composable(Routes.HISTORY) {
                    HistoryScreen(
                        viewModel = viewModel,
                        onOpenWeekly = { navController.switchTab(Routes.WEEKLY) },
                        onAddFood = { requestedMeal = null; showAddSheet = true }
                    )
                }
                composable(Routes.WEEKLY) {
                    WeeklyScreen(viewModel = viewModel, onOpenWeight = { navController.navigate(Routes.WEIGHT) })
                }
                composable(Routes.PROFILE) {
                    ProfileScreen(
                        viewModel = viewModel,
                        onOpenWeight = { navController.navigate(Routes.WEIGHT) },
                        onOpenGoals = { navController.navigate(Routes.GOALS) }
                    )
                }
                composable(Routes.RESULT) {
                    ResultScreen(
                        viewModel = viewModel,
                        onAddAnother = { openSearch(newMeal = false) },
                        onBack = {
                            viewModel.cancelAnalysis()
                            navController.goHome()
                        },
                        onSaved = { navController.goHome() }
                    )
                }
                composable(Routes.SEARCH) {
                    FoodSearchScreen(
                        viewModel = viewModel,
                        onDone = { navController.openResult() },
                        onBack = { navController.popBackStackOrHome() }
                    )
                }
                composable(Routes.BARCODE) {
                    BarcodeScreen(
                        viewModel = viewModel,
                        onSearch = { viewModel.resetBarcode(); openSearch(newMeal = true) },
                        onResult = {
                            viewModel.confirmBarcodePortion()
                            navController.openResult()
                        },
                        onBack = {
                            viewModel.resetBarcode()
                            navController.popBackStackOrHome()
                        }
                    )
                }
                composable(Routes.VOICE) {
                    VoiceScreen(
                        viewModel = viewModel,
                        onResult = { navController.openResult() },
                        onBack = {
                            viewModel.resetVoice()
                            navController.popBackStackOrHome()
                        }
                    )
                }
                composable(Routes.WEIGHT) {
                    WeightScreen(viewModel = viewModel, onBack = { navController.popBackStackOrHome() })
                }
                composable(Routes.GOALS) {
                    GoalsScreen(viewModel = viewModel, onBack = { navController.popBackStackOrHome() })
                }
            }
        }

        if (showAddSheet) {
            AddMealSheet(
                onDismiss = { showAddSheet = false; requestedMeal = null },
                onCamera = {
                    showAddSheet = false
                    photoInput.takePhoto()
                },
                onGallery = {
                    showAddSheet = false
                    photoInput.pickGallery()
                },
                onSearch = {
                    showAddSheet = false
                    openSearch(newMeal = true)
                },
                onBarcode = {
                    showAddSheet = false
                    navController.navigate(Routes.BARCODE)
                },
                onVoice = {
                    showAddSheet = false
                    navController.navigate(Routes.VOICE)
                }
            )
        }
    }
}

/** Pindah tab tanpa menumpuk layar di belakang. */
private fun NavHostController.switchTab(route: String) {
    navigate(route) {
        popUpTo(graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * Membuka layar hasil setelah input. Layar input (cari, barcode, suara) dibuang dari
 * tumpukan supaya tombol kembali langsung menuju Beranda.
 */
private fun NavHostController.openResult() {
    if (popBackStack(Routes.RESULT, inclusive = false)) return
    navigate(Routes.RESULT) {
        popUpTo(Routes.HOME) { inclusive = false }
        launchSingleTop = true
    }
}

private fun NavHostController.goHome() {
    popBackStack(Routes.HOME, inclusive = false)
}

/** Kembali satu langkah; kalau tumpukan kosong, kembali ke Beranda. */
private fun NavHostController.popBackStackOrHome() {
    if (!popBackStack()) {
        navigate(Routes.HOME) { launchSingleTop = true }
    }
}