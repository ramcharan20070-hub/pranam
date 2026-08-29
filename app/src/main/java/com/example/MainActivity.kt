package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.example.model.WorkoutType
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.viewmodel.FitnessViewModel

enum class AppDestination(val route: String, val title: String, val icon: ImageVector) {
    TRACKER("tracker", "Tracker", Icons.AutoMirrored.Filled.DirectionsRun),
    AI_COACH("ai_coach", "AI Coach", Icons.Default.Psychology),
    HISTORY("history", "Analytics", Icons.Default.Assessment),
    PROFILE("profile", "Profile", Icons.Default.Person)
}

class MainActivity : ComponentActivity() {

    private val viewModel: FitnessViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle possible Notification intent actions
        handleNotificationIntent(intent)

        setContent {
            val appThemeMode by viewModel.appThemeMode.collectAsState()
            val isDarkTheme = when (appThemeMode) {
                com.example.model.AppThemeMode.CYBER_BLACK -> true
                com.example.model.AppThemeMode.HIGH_TECH_WHITE -> false
                com.example.model.AppThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            PranamTheme(darkTheme = isDarkTheme) {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: android.content.Intent?) {
        if (intent == null) return
        val action = intent.action
        val workoutTypeName = intent.getStringExtra(com.example.tracker.NotificationHelper.EXTRA_WORKOUT_TYPE)

        if (action == com.example.tracker.NotificationHelper.ACTION_START_WORKOUT && workoutTypeName != null) {
            val type = try {
                WorkoutType.valueOf(workoutTypeName)
            } catch (e: Exception) {
                WorkoutType.RUNNING
            }
            viewModel.startWorkout(type)
        }
    }
}

@Composable
fun MainAppScreen(viewModel: FitnessViewModel) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: AppDestination.TRACKER.route

    val completedSession by viewModel.completedSessionSummary.collectAsState()
    val completedAnalysis by viewModel.completedAnalysis.collectAsState()
    val isPostAnalysisLoading by viewModel.isPostAnalysisLoading.collectAsState()
    val liveState by viewModel.liveState.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val showSetupWizard by viewModel.showSetupWizard.collectAsState()

    // If setup is not completed yet (e.g. first login/launch) or user requested the setup wizard
    if (!userProfile.isSetupCompleted || showSetupWizard) {
        ProfileFitnessSetupScreen(
            viewModel = viewModel,
            onFinish = { viewModel.closeSetupWizard() }
        )
        return
    }

    var lastBackPressTime by remember { mutableLongStateOf(0L) }

    // Safe BackHandler: If on Tracker and not tracking, double-press to exit prevents sudden closes
    BackHandler(enabled = currentRoute == AppDestination.TRACKER.route && !liveState.isTracking) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastBackPressTime < 2000L) {
            (context as? ComponentActivity)?.finish()
        } else {
            lastBackPressTime = currentTime
            Toast.makeText(context, "Press back again to exit Pranam", Toast.LENGTH_SHORT).show()
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 600.dp

        if (isWideScreen) {
            // Tablet / Foldable (unfolded) / Landscape Layout: NavigationRail on side
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(PulseTheme.colors.background)
            ) {
                NavigationRail(
                    containerColor = PulseTheme.colors.surfaceElevated,
                    header = {
                        Column(
                            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(PulseTheme.colors.primary.copy(alpha = 0.2f)),
                                contentAlignment = androidx.compose.ui.Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.DirectionsRun,
                                    contentDescription = "Pranam",
                                    tint = PulseTheme.colors.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "PRANAM",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = PulseTheme.colors.primary,
                                fontSize = 9.sp
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxHeight()
                        .border(
                            width = 1.dp,
                            color = PulseTheme.colors.border
                        )
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    AppDestination.entries.forEach { destination ->
                        val isSelected = currentRoute == destination.route
                        NavigationRailItem(
                            selected = isSelected,
                            onClick = {
                                if (currentRoute != destination.route) {
                                    navController.navigate(destination.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = destination.icon,
                                    contentDescription = destination.title,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = destination.title,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 11.sp
                                )
                            },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = PulseTheme.colors.primary,
                                selectedTextColor = PulseTheme.colors.primary,
                                indicatorColor = if (PulseTheme.colors.isDark) CyberSurfaceCard else LightSurfaceVariant,
                                unselectedIconColor = PulseTheme.colors.textMuted,
                                unselectedTextColor = PulseTheme.colors.textMuted
                            ),
                            modifier = Modifier.testTag("nav_rail_${destination.route}")
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                }

                NavHost(
                    navController = navController,
                    startDestination = AppDestination.TRACKER.route,
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    composable(AppDestination.TRACKER.route) {
                        LiveTrackerScreen(viewModel = viewModel)
                    }
                    composable(AppDestination.AI_COACH.route) {
                        AiCoachScreen(
                            viewModel = viewModel,
                            onStartWorkout = { type ->
                                viewModel.startWorkout(type)
                                navController.navigate(AppDestination.TRACKER.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                }
                            }
                        )
                    }
                    composable(AppDestination.HISTORY.route) {
                        HistoryScreen(viewModel = viewModel)
                    }
                    composable(AppDestination.PROFILE.route) {
                        ProfileScreen(viewModel = viewModel)
                    }
                }
            }
        } else {
            // Handheld Vertical Layout: Bottom NavigationBar
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = PulseTheme.colors.background,
                bottomBar = {
                    NavigationBar(
                        containerColor = PulseTheme.colors.surfaceElevated,
                        tonalElevation = 8.dp,
                        modifier = Modifier
                            .border(
                                width = 1.dp,
                                color = PulseTheme.colors.border
                            )
                    ) {
                        AppDestination.entries.forEach { destination ->
                            val isSelected = currentRoute == destination.route
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = {
                                    if (currentRoute != destination.route) {
                                        navController.navigate(destination.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = destination.icon,
                                        contentDescription = destination.title,
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = destination.title,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 10.sp
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = PulseTheme.colors.primary,
                                    selectedTextColor = PulseTheme.colors.primary,
                                    indicatorColor = if (PulseTheme.colors.isDark) CyberSurfaceCard else LightSurfaceVariant,
                                    unselectedIconColor = PulseTheme.colors.textMuted,
                                    unselectedTextColor = PulseTheme.colors.textMuted
                                ),
                                modifier = Modifier.testTag("nav_tab_${destination.route}")
                            )
                        }
                    }
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = AppDestination.TRACKER.route,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    composable(AppDestination.TRACKER.route) {
                        LiveTrackerScreen(viewModel = viewModel)
                    }
                    composable(AppDestination.AI_COACH.route) {
                        AiCoachScreen(
                            viewModel = viewModel,
                            onStartWorkout = { type ->
                                viewModel.startWorkout(type)
                                navController.navigate(AppDestination.TRACKER.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                }
                            }
                        )
                    }
                    composable(AppDestination.HISTORY.route) {
                        HistoryScreen(viewModel = viewModel)
                    }
                    composable(AppDestination.PROFILE.route) {
                        ProfileScreen(viewModel = viewModel)
                    }
                }
            }
        }

        // Post-Workout Summary Dialog
        if (completedSession != null) {
            WorkoutSummaryDialog(
                session = completedSession!!,
                analysis = completedAnalysis,
                isLoading = isPostAnalysisLoading,
                onDismiss = { viewModel.dismissCompletedSummary() }
            )
        }
    }
}
