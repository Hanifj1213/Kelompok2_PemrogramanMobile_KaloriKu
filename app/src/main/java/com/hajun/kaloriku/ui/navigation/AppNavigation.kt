package com.hajun.kaloriku.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost

internal const val PAGE_TRANSITION_MILLIS = 300

/** Satu NavHost untuk aplikasi dan tes, sehingga transisi yang diuji sama dengan UI nyata. */
@Composable
internal fun KaloriNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: Any = HomeRoute,
    builder: NavGraphBuilder.() -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier.fillMaxSize(),
        enterTransition = { pageEnter(isPop = false) },
        exitTransition = { pageExit(isPop = false) },
        popEnterTransition = { pageEnter(isPop = true) },
        popExitTransition = { pageExit(isPop = true) },
        builder = builder
    )
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.pageEnter(isPop: Boolean): EnterTransition {
    if (initialState.destination == targetState.destination) return EnterTransition.None
    // Halaman tidak dipudarkan agar pergeserannya tetap terlihat pada skala animasi 0,5x.
    return slideIntoContainer(
        towards = pageDirection(isPop),
        animationSpec = tween(PAGE_TRANSITION_MILLIS, easing = FastOutSlowInEasing)
    )
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.pageExit(isPop: Boolean): ExitTransition {
    if (initialState.destination == targetState.destination) return ExitTransition.None
    return slideOutOfContainer(
        towards = pageDirection(isPop),
        animationSpec = tween(PAGE_TRANSITION_MILLIS, easing = FastOutSlowInEasing)
    )
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.pageDirection(isPop: Boolean): SlideDirection {
    val from = tabIndexOf(initialState.destination)
    val to = tabIndexOf(targetState.destination)
    return when {
        from != null && to != null -> if (to > from) SlideDirection.Start else SlideDirection.End
        isPop -> SlideDirection.End
        else -> SlideDirection.Start
    }
}

private fun tabIndexOf(destination: NavDestination): Int? = when {
    destination.hierarchy.any { it.hasRoute(HomeRoute::class) } -> 0
    destination.hierarchy.any { it.hasRoute(HistoryRoute::class) } -> 1
    destination.hierarchy.any { it.hasRoute(ProfileRoute::class) } -> 2
    else -> null
}

/** Pindah tab tanpa mengulang tab aktif, menumpuk layar, atau kehilangan input dan posisi gulir. */
internal fun NavHostController.switchTab(route: Any) {
    if (currentDestination?.hasRoute(route::class) == true) return
    navigate(route) {
        popUpTo(graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
