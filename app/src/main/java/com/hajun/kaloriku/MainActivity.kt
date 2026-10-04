package com.hajun.kaloriku

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.hajun.kaloriku.data.MealType
import com.hajun.kaloriku.notification.MealReminderWorker
import com.hajun.kaloriku.ui.MainViewModel
import com.hajun.kaloriku.ui.components.AddMealSheet
import com.hajun.kaloriku.ui.components.KaloriBottomBar
import com.hajun.kaloriku.ui.components.bottomTabs
import com.hajun.kaloriku.ui.components.rememberPhotoInput
import com.hajun.kaloriku.ui.navigation.BarcodeRoute
import com.hajun.kaloriku.ui.navigation.GoalsRoute
import com.hajun.kaloriku.ui.navigation.HistoryRoute
import com.hajun.kaloriku.ui.navigation.HomeRoute
import com.hajun.kaloriku.ui.navigation.MealDetailRoute
import com.hajun.kaloriku.ui.navigation.ProfileRoute
import com.hajun.kaloriku.ui.navigation.ResultRoute
import com.hajun.kaloriku.ui.navigation.SearchRoute
import com.hajun.kaloriku.ui.navigation.resolveManualMealSelection
import com.hajun.kaloriku.ui.screen.BarcodeScreen
import com.hajun.kaloriku.ui.screen.FoodSearchScreen
import com.hajun.kaloriku.ui.screen.GoalsScreen
import com.hajun.kaloriku.ui.screen.HistoryScreen
import com.hajun.kaloriku.ui.screen.HomeScreen
import com.hajun.kaloriku.ui.screen.MealDetailScreen
import com.hajun.kaloriku.ui.screen.ProfileScreen
import com.hajun.kaloriku.ui.screen.ResultScreen
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
    val currentDestination = backStackEntry?.destination
    val showBottomBar = bottomTabs.any { tab -> currentDestination?.hierarchy?.any { it.hasRoute(tab.route::class) } == true }
    val selectedTab = bottomTabs.firstOrNull { tab ->
        currentDestination?.hierarchy?.any { it.hasRoute(tab.route::class) } == true
    }?.route

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
        navController.navigate(SearchRoute(appendToDraft = !newMeal)) { launchSingleTop = true }
    }

    BackHandler(enabled = currentDestination != null && !isHome(currentDestination) && !showAddSheet) {
        when {
            currentDestination?.hierarchy?.any { it.hasRoute(ResultRoute::class) } == true -> {
                viewModel.cancelAnalysis()
                navController.goHome()
            }
            currentDestination?.hierarchy?.any { it.hasRoute(BarcodeRoute::class) } == true -> {
                viewModel.resetBarcode()
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
                    enter = slideInVertically(tween(200)) { it } + fadeIn(tween(200)),
                    exit = slideOutVertically(tween(160)) { it } + fadeOut(tween(120))
                ) {
                    KaloriBottomBar(
                        selectedRoute = selectedTab,
                        onSelect = { route -> navController.switchTab(route) }
                    )
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = HomeRoute,
                modifier = Modifier.padding(innerPadding),
                enterTransition = { fadeIn(tween(180)) },
                exitTransition = { fadeOut(tween(140)) },
                popEnterTransition = { fadeIn(tween(180)) },
                popExitTransition = { fadeOut(tween(140)) }
            ) {
                composable<HomeRoute> {
                    HomeScreen(
                        viewModel = viewModel,
                        onAddMeal = { type -> requestedMeal = type; showAddSheet = true },
                        onOpenProfile = { navController.switchTab(ProfileRoute) },
                        onOpenHistory = { navController.switchTab(HistoryRoute) },
                        onOpenGoals = { navController.navigate(GoalsRoute) },
                        onOpenEntry = { entryId -> navController.navigate(MealDetailRoute(entryId)) }
                    )
                }
                composable<HistoryRoute> {
                    HistoryScreen(
                        viewModel = viewModel,
                        onAddFood = { requestedMeal = null; showAddSheet = true },
                        onOpenEntry = { entryId -> navController.navigate(MealDetailRoute(entryId)) }
                    )
                }
                composable<ProfileRoute> {
                    ProfileScreen(
                        viewModel = viewModel,
                        onOpenGoals = { navController.navigate(GoalsRoute) }
                    )
                }
                composable<ResultRoute> {
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
                composable<SearchRoute> { entry ->
                    val route = entry.toRoute<SearchRoute>()
                    LaunchedEffect(route) {
                        val selection = resolveManualMealSelection(
                            route = route,
                            currentMealType = viewModel.mealType,
                            freshMealType = MealType.fromHour(java.time.LocalTime.now().hour)
                        )
                        if (selection.startFresh) viewModel.startManualMeal()
                        viewModel.mealType = selection.mealType
                    }
                    FoodSearchScreen(
                        viewModel = viewModel,
                        onDone = { navController.openResult() },
                        onBack = { navController.popBackStackOrHome() }
                    )
                }
                composable<BarcodeRoute> {
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
                composable<GoalsRoute> {
                    GoalsScreen(viewModel = viewModel, onBack = { navController.popBackStackOrHome() })
                }
                composable<MealDetailRoute> { entry ->
                    MealDetailScreen(
                        viewModel = viewModel,
                        backStackEntry = entry,
                        onBack = { navController.popBackStackOrHome() }
                    )
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
                    navController.navigate(BarcodeRoute)
                }
            )
        }
    }
}

private fun isHome(destination: androidx.navigation.NavDestination): Boolean =
    destination.hierarchy.any { it.hasRoute(HomeRoute::class) }

/** Pindah tab tanpa menumpuk layar di belakang. */
private fun NavHostController.switchTab(route: Any) {
    navigate(route) {
        popUpTo(graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * Membuka layar hasil setelah input. Layar input (cari, barcode) dibuang dari
 * tumpukan supaya tombol kembali langsung menuju Beranda.
 */
private fun NavHostController.openResult() {
    if (popBackStack(ResultRoute, inclusive = false)) return
    navigate(ResultRoute) {
        popUpTo(HomeRoute)
        launchSingleTop = true
    }
}

private fun NavHostController.goHome() {
    popBackStack(HomeRoute, inclusive = false)
}

/** Kembali satu langkah; kalau tumpukan kosong, kembali ke Beranda. */
private fun NavHostController.popBackStackOrHome() {
    if (!popBackStack()) {
        navigate(HomeRoute) { launchSingleTop = true }
    }
}
