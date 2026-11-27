package com.moneytracker.app.ui.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.moneytracker.app.ui.components.MoneyTrackerLogoMark
import com.moneytracker.app.ui.screens.calendar.CalendarScreen
import com.moneytracker.app.ui.screens.dashboard.DashboardScreen
import com.moneytracker.app.ui.screens.expenses.ExpensesScreen
import com.moneytracker.app.ui.screens.income.IncomeScreen
import com.moneytracker.app.ui.screens.login.LoginScreen
import com.moneytracker.app.ui.screens.recurring.RecurringScreen
import com.moneytracker.app.ui.screens.settings.SettingsScreen
import com.moneytracker.app.ui.theme.ObsidianColors
import com.moneytracker.app.viewmodel.AppContainer
import com.moneytracker.app.viewmodel.AuthViewModel
import com.moneytracker.app.viewmodel.CalendarViewModel
import com.moneytracker.app.viewmodel.DashboardViewModel
import com.moneytracker.app.viewmodel.ExpensesViewModel
import com.moneytracker.app.viewmodel.IncomeViewModel
import com.moneytracker.app.viewmodel.RecurringViewModel
import com.moneytracker.app.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

private object Routes {
    const val DASHBOARD = "dashboard"
    const val EXPENSES = "expenses"
    const val INCOME = "income"
    const val CALENDAR = "calendar"
    const val RECURRING = "recurring"
    const val SETTINGS = "settings"
}

private data class PrimaryDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val PRIMARY_DESTINATIONS = listOf(
    PrimaryDestination(Routes.DASHBOARD, "Overview", Icons.Rounded.Dashboard),
    PrimaryDestination(Routes.EXPENSES, "Expenses", Icons.Rounded.ReceiptLong),
    PrimaryDestination(Routes.INCOME, "Income", Icons.Rounded.TrendingUp),
    PrimaryDestination(Routes.CALENDAR, "Calendar", Icons.Rounded.CalendarMonth),
    PrimaryDestination(Routes.RECURRING, "Recurring", Icons.Rounded.Repeat)
)

@Composable
fun MoneyTrackerRoot(container: AppContainer) {
    val authViewModel: AuthViewModel = viewModel(factory = container.viewModelFactory)
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianColors.ObsidianCanvas)
    ) {
        if (!authState.isAuthenticated) {
            LoginScreen(viewModel = authViewModel)
        } else {
            AuthenticatedShell(
                container = container,
                onSignOut = { authViewModel.signOut() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedShell(
    container: AppContainer,
    onSignOut: () -> Unit
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val showSnackbar: (String) -> Unit = remember(snackbarHostState, coroutineScope) {
        { message ->
            coroutineScope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    // Scope feature ViewModels to the authenticated shell so tab switches retain state smoothly.
    val dashboardViewModel: DashboardViewModel = viewModel(factory = container.viewModelFactory)
    val expensesViewModel: ExpensesViewModel = viewModel(factory = container.viewModelFactory)
    val incomeViewModel: IncomeViewModel = viewModel(factory = container.viewModelFactory)
    val calendarViewModel: CalendarViewModel = viewModel(factory = container.viewModelFactory)
    val recurringViewModel: RecurringViewModel = viewModel(factory = container.viewModelFactory)
    val settingsViewModel: SettingsViewModel = viewModel(factory = container.viewModelFactory)

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Routes.DASHBOARD
    val isSettingsRoute = currentRoute == Routes.SETTINGS

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun navigateToSettings() {
        if (!isSettingsRoute) {
            navController.navigate(Routes.SETTINGS) {
                launchSingleTop = true
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 840.dp

        Row(modifier = Modifier.fillMaxSize()) {
            if (useNavRail) {
                NavigationRail(
                    modifier = Modifier
                        .fillMaxHeight()
                        .statusBarsPadding(),
                    containerColor = ObsidianColors.JadeSurface,
                    contentColor = ObsidianColors.TextPrimary,
                    header = {
                        Box(
                            modifier = Modifier.padding(top = 12.dp, bottom = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            MoneyTrackerLogoMark(size = 36.dp)
                        }
                    }
                ) {
                    PRIMARY_DESTINATIONS.forEach { dest ->
                        val selected = currentRoute == dest.route
                        NavigationRailItem(
                            selected = selected,
                            onClick = { navigateToTab(dest.route) },
                            icon = {
                                Icon(
                                    imageVector = dest.icon,
                                    contentDescription = dest.label
                                )
                            },
                            label = {
                                Text(
                                    text = dest.label,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = ObsidianColors.EmeraldPrimary,
                                selectedTextColor = ObsidianColors.EmeraldPrimary,
                                indicatorColor = ObsidianColors.EmeraldPrimary.copy(alpha = 0.16f),
                                unselectedIconColor = ObsidianColors.TextSecondary,
                                unselectedTextColor = ObsidianColors.TextSecondary
                            )
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    NavigationRailItem(
                        selected = isSettingsRoute,
                        onClick = { navigateToSettings() },
                        icon = {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = "Settings"
                            )
                        },
                        label = {
                            Text(
                                text = "Settings",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = ObsidianColors.EmeraldPrimary,
                            selectedTextColor = ObsidianColors.EmeraldPrimary,
                            indicatorColor = ObsidianColors.EmeraldPrimary.copy(alpha = 0.16f),
                            unselectedIconColor = ObsidianColors.TextSecondary,
                            unselectedTextColor = ObsidianColors.TextSecondary
                        )
                    )
                }
                VerticalDivider(
                    thickness = 1.dp,
                    color = ObsidianColors.Hairline
                )
            }

            Scaffold(
                modifier = Modifier.weight(1f),
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                topBar = {
                    val topBarTitle = when (currentRoute) {
                        Routes.SETTINGS -> "Settings"
                        Routes.EXPENSES -> "Expenses"
                        Routes.INCOME -> "Income"
                        Routes.CALENDAR -> "Calendar"
                        Routes.RECURRING -> "Recurring"
                        else -> "Overview"
                    }
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = ObsidianColors.ObsidianCanvas,
                            titleContentColor = ObsidianColors.TextPrimary,
                            actionIconContentColor = ObsidianColors.TextSecondary,
                            navigationIconContentColor = ObsidianColors.TextPrimary
                        ),
                        navigationIcon = {
                            if (isSettingsRoute) {
                                IconButton(
                                    onClick = { navController.popBackStack() },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                        contentDescription = "Back"
                                    )
                                }
                            }
                        },
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (!isSettingsRoute && !useNavRail && currentRoute == Routes.DASHBOARD) {
                                    MoneyTrackerLogoMark(size = 26.dp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                }
                                Text(
                                    text = topBarTitle,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = ObsidianColors.TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        },
                        actions = {
                            if (!isSettingsRoute && !useNavRail) {
                                IconButton(
                                    onClick = { navigateToSettings() },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Settings,
                                        contentDescription = "Settings",
                                        tint = ObsidianColors.TextSecondary
                                    )
                                }
                            }
                        }
                    )
                },
                bottomBar = {
                    if (!useNavRail && !isSettingsRoute) {
                        Surface(
                            color = ObsidianColors.JadeSurface,
                            tonalElevation = 0.dp
                        ) {
                            Box {
                                HorizontalDivider(
                                    thickness = 1.dp,
                                    color = ObsidianColors.Hairline,
                                    modifier = Modifier.align(Alignment.TopCenter)
                                )
                                NavigationBar(
                                    containerColor = Color.Transparent,
                                    contentColor = ObsidianColors.TextPrimary,
                                    tonalElevation = 0.dp
                                ) {
                                    PRIMARY_DESTINATIONS.forEach { dest ->
                                        val selected = currentRoute == dest.route
                                        NavigationBarItem(
                                            selected = selected,
                                            onClick = { navigateToTab(dest.route) },
                                            icon = {
                                                Icon(
                                                    imageVector = dest.icon,
                                                    contentDescription = dest.label
                                                )
                                            },
                                            label = {
                                                Text(
                                                    text = dest.label,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            },
                                            alwaysShowLabel = true,
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = ObsidianColors.EmeraldPrimary,
                                                selectedTextColor = ObsidianColors.EmeraldPrimary,
                                                indicatorColor = ObsidianColors.EmeraldPrimary.copy(alpha = 0.16f),
                                                unselectedIconColor = ObsidianColors.TextSecondary,
                                                unselectedTextColor = ObsidianColors.TextSecondary
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                snackbarHost = {
                    SnackbarHost(hostState = snackbarHostState) { data ->
                        Surface(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = ObsidianColors.JadeElevated,
                            border = BorderStroke(1.dp, ObsidianColors.HairlineHover),
                            shadowElevation = 8.dp
                        ) {
                            Snackbar(
                                snackbarData = data,
                                containerColor = Color.Transparent,
                                contentColor = ObsidianColors.TextPrimary,
                                actionColor = ObsidianColors.EmeraldPrimary
                            )
                        }
                    }
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = Routes.DASHBOARD,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .then(
                            if (useNavRail || isSettingsRoute) Modifier.navigationBarsPadding()
                            else Modifier
                        )
                ) {
                    composable(Routes.DASHBOARD) {
                        DashboardScreen(
                            viewModel = dashboardViewModel,
                            onNavigateToSettings = { navigateToSettings() },
                            onNavigateToRecurring = { navigateToTab(Routes.RECURRING) },
                            onNavigateToExpenses = { navigateToTab(Routes.EXPENSES) },
                            onSnackbar = showSnackbar
                        )
                    }
                    composable(Routes.EXPENSES) {
                        ExpensesScreen(
                            viewModel = expensesViewModel,
                            onSnackbar = showSnackbar
                        )
                    }
                    composable(Routes.INCOME) {
                        IncomeScreen(
                            viewModel = incomeViewModel,
                            onSnackbar = showSnackbar
                        )
                    }
                    composable(Routes.CALENDAR) {
                        CalendarScreen(
                            viewModel = calendarViewModel,
                            onSnackbar = showSnackbar
                        )
                    }
                    composable(Routes.RECURRING) {
                        RecurringScreen(
                            viewModel = recurringViewModel,
                            onSnackbar = showSnackbar
                        )
                    }
                    composable(Routes.SETTINGS) {
                        SettingsScreen(
                            viewModel = settingsViewModel,
                            onSignOut = onSignOut,
                            onSnackbar = showSnackbar
                        )
                    }
                }
            }
        }
    }
}
