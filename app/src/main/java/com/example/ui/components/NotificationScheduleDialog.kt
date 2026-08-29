package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.SendToMobile
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.model.*
import com.example.ui.theme.*
import com.example.viewmodel.FitnessViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScheduleDialog(
    viewModel: FitnessViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scheduledReminders by viewModel.scheduledReminders.collectAsState()
    val dailyPreferences by viewModel.dailyNotificationPrefs.collectAsState()
    val notificationHistory by viewModel.notificationHistory.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Schedules, 1: Daily Briefings, 2: History & Test
    var showAddScheduleModal by remember { mutableStateOf(false) }
    var editingReminder by remember { mutableStateOf<ScheduledWorkoutReminder?>(null) }
    var testFeedbackMessage by remember { mutableStateOf<String?>(null) }

    // Check Android 13+ Notification Permission
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .testTag("notification_schedule_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CyberSurface),
            border = BorderStroke(1.dp, CyberBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(NeonCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "PUSH NOTIFICATIONS & REMINDERS",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "Timely Workout Alerts & Daily Activity Briefings",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonCyan
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_notification_dialog_btn")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Permission Warning Banner if not granted
                if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = NeonCoral.copy(alpha = 0.15f)),
                        border = BorderStroke(1.dp, NeonCoral.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.NotificationsOff, contentDescription = null, tint = NeonCoral, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Notification Permission Needed", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text("Enable notifications to receive scheduled alarms & briefings.", fontSize = 11.sp, color = TextMuted)
                                }
                            }
                            Button(
                                onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCoral),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("Allow", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }

                // Tab Row
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = CyberSurfaceElevated,
                    contentColor = NeonCyan,
                    divider = {},
                    indicator = {}
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                "WORKOUT ALARMS (${scheduledReminders.size})",
                                fontSize = 11.sp,
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == 0) NeonCyan else TextMuted
                            )
                        },
                        modifier = Modifier.testTag("tab_workout_alarms")
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                "DAILY BRIEFINGS",
                                fontSize = 11.sp,
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == 1) NeonCyan else TextMuted
                            )
                        },
                        modifier = Modifier.testTag("tab_daily_briefings")
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = {
                            Text(
                                "TEST & LOGS",
                                fontSize = 11.sp,
                                fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == 2) NeonCyan else TextMuted
                            )
                        },
                        modifier = Modifier.testTag("tab_test_logs")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tab Content
                when (selectedTab) {
                    0 -> WorkoutSchedulesTab(
                        reminders = scheduledReminders,
                        onToggle = { id, enabled -> viewModel.toggleScheduledReminder(id, enabled) },
                        onDelete = { id -> viewModel.deleteScheduledReminder(id) },
                        onEdit = { reminder ->
                            editingReminder = reminder
                            showAddScheduleModal = true
                        },
                        onAddNew = {
                            editingReminder = null
                            showAddScheduleModal = true
                        }
                    )
                    1 -> DailyBriefingsTab(
                        preferences = dailyPreferences,
                        onUpdate = { newPrefs -> viewModel.updateDailyNotificationPreferences(newPrefs) }
                    )
                    2 -> TestAndLogsTab(
                        history = notificationHistory,
                        feedbackMessage = testFeedbackMessage,
                        onTestWorkoutAlarm = {
                            viewModel.testWorkoutReminderPush(WorkoutType.RUNNING)
                            testFeedbackMessage = "Push Notification dispatched: Scheduled Running Reminder"
                        },
                        onTestMorningBriefing = {
                            viewModel.testMorningBriefingPush()
                            testFeedbackMessage = "Push Notification dispatched: Morning Briefing & Goals"
                        },
                        onTestEveningRecap = {
                            viewModel.testEveningRecapPush()
                            testFeedbackMessage = "Push Notification dispatched: Evening Progress Wrap-Up"
                        },
                        onTestInactivityAlert = {
                            viewModel.testInactivityAlertPush()
                            testFeedbackMessage = "Push Notification dispatched: Inactivity Recovery Alert"
                        },
                        onClearLogs = { viewModel.clearNotificationHistory() }
                    )
                }
            }
        }
    }

    if (showAddScheduleModal) {
        AddEditReminderModal(
            initialReminder = editingReminder,
            onSave = { reminder ->
                viewModel.addOrUpdateScheduledReminder(reminder)
                showAddScheduleModal = false
                editingReminder = null
            },
            onDismiss = {
                showAddScheduleModal = false
                editingReminder = null
            }
        )
    }
}

@Composable
fun WorkoutSchedulesTab(
    reminders: List<ScheduledWorkoutReminder>,
    onToggle: (Long, Boolean) -> Unit,
    onDelete: (Long) -> Unit,
    onEdit: (ScheduledWorkoutReminder) -> Unit,
    onAddNew: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SCHEDULED ATHLETIC SESSIONS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 0.5.sp
            )

            Button(
                onClick = onAddNew,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("add_new_schedule_btn")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = CyberBackground, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Reminder", color = CyberBackground, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (reminders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.Alarm, contentDescription = null, tint = TextMuted, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No scheduled reminders", color = TextMuted, fontSize = 14.sp)
                    Text("Tap 'Add Reminder' to schedule weekly workouts", color = TextSecondary, fontSize = 12.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(reminders, key = { it.id }) { reminder ->
                    ScheduledReminderCard(
                        reminder = reminder,
                        onToggle = { enabled -> onToggle(reminder.id, enabled) },
                        onDelete = { onDelete(reminder.id) },
                        onEdit = { onEdit(reminder) }
                    )
                }
            }
        }
    }
}

@Composable
fun ScheduledReminderCard(
    reminder: ScheduledWorkoutReminder,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    val borderColor = if (reminder.isEnabled) NeonCyan.copy(alpha = 0.6f) else CyberBorder

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("reminder_card_${reminder.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSurfaceCard),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (reminder.isEnabled) NeonCyan.copy(alpha = 0.15f)
                                else CyberSurfaceElevated
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (reminder.workoutType) {
                                WorkoutType.RUNNING -> Icons.AutoMirrored.Filled.DirectionsRun
                                WorkoutType.CYCLING -> Icons.AutoMirrored.Filled.DirectionsBike
                                WorkoutType.POWER_WALK -> Icons.AutoMirrored.Filled.DirectionsWalk
                                WorkoutType.HIIT_SPRINT -> Icons.Default.FlashOn
                                WorkoutType.TRAIL_RUN -> Icons.Default.Terrain
                                WorkoutType.HIKING -> Icons.Default.Landscape
                                WorkoutType.ROWING -> Icons.Default.Rowing
                            },
                            contentDescription = null,
                            tint = if (reminder.isEnabled) NeonCyan else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = reminder.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (reminder.isEnabled) TextPrimary else TextMuted
                        )
                        Text(
                            text = "${reminder.getFormattedTime()} • ${reminder.targetDurationMin} min",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (reminder.isEnabled) NeonCyan else TextMuted
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = reminder.isEnabled,
                        onCheckedChange = onToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyberBackground,
                            checkedTrackColor = NeonCyan,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = CyberSurfaceElevated
                        ),
                        modifier = Modifier.testTag("switch_reminder_${reminder.id}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Days of week chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    val daysMap = listOf(
                        Calendar.MONDAY to "M",
                        Calendar.TUESDAY to "T",
                        Calendar.WEDNESDAY to "W",
                        Calendar.THURSDAY to "T",
                        Calendar.FRIDAY to "F",
                        Calendar.SATURDAY to "S",
                        Calendar.SUNDAY to "S"
                    )
                    daysMap.forEach { (calDay, label) ->
                        val isSelected = reminder.daysOfWeek.contains(calDay)
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected && reminder.isEnabled) NeonCyan.copy(alpha = 0.25f)
                                    else if (isSelected) CyberSurfaceElevated
                                    else Color.Transparent
                                )
                                .border(
                                    1.dp,
                                    if (isSelected && reminder.isEnabled) NeonCyan else CyberBorder,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected && reminder.isEnabled) NeonCyan else TextMuted
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = NeonCyan, modifier = Modifier.size(16.dp))
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = NeonCoral, modifier = Modifier.size(16.dp))
                    }
                }
            }

            if (reminder.focusRationale.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "🎯 Focus: ${reminder.focusRationale}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun DailyBriefingsTab(
    preferences: DailyNotificationPreference,
    onUpdate: (DailyNotificationPreference) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            // Master Switch
            CyberCard(borderColor = if (preferences.notificationsMasterEnabled) NeonCyan.copy(alpha = 0.5f) else CyberBorder) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "MASTER NOTIFICATION ENGINE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Controls all push notifications and status broadcasts",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                    Switch(
                        checked = preferences.notificationsMasterEnabled,
                        onCheckedChange = { onUpdate(preferences.copy(notificationsMasterEnabled = it)) },
                        colors = SwitchDefaults.colors(checkedThumbColor = CyberBackground, checkedTrackColor = NeonCyan),
                        modifier = Modifier.testTag("master_notification_switch")
                    )
                }
            }
        }

        item {
            // Morning Briefing Card
            CyberCard(borderColor = if (preferences.morningBriefingEnabled) NeonGreen.copy(alpha = 0.5f) else CyberBorder) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.WbSunny, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Morning Readiness Briefing", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                                Text("Daily goals, recovery score & tactical advice", fontSize = 11.sp, color = TextMuted)
                            }
                        }
                        Switch(
                            checked = preferences.morningBriefingEnabled,
                            onCheckedChange = { onUpdate(preferences.copy(morningBriefingEnabled = it)) },
                            colors = SwitchDefaults.colors(checkedThumbColor = CyberBackground, checkedTrackColor = NeonGreen),
                            modifier = Modifier.testTag("morning_briefing_switch")
                        )
                    }

                    if (preferences.morningBriefingEnabled) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Delivery Time:", fontSize = 12.sp, color = TextSecondary)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val morningTimes = listOf(Pair(7, 0), Pair(7, 30), Pair(8, 0), Pair(8, 30))
                                morningTimes.forEach { (h, m) ->
                                    val isSelected = preferences.morningHour == h && preferences.morningMinute == m
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { onUpdate(preferences.copy(morningHour = h, morningMinute = m)) },
                                        label = { Text("${if (h < 10) "0$h" else "$h"}:${if (m == 0) "00" else "$m"} AM", fontSize = 10.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = NeonGreen.copy(alpha = 0.2f),
                                            selectedLabelColor = NeonGreen,
                                            containerColor = CyberSurfaceElevated,
                                            labelColor = TextMuted
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = isSelected,
                                            borderColor = if (isSelected) NeonGreen else CyberBorder
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            // Evening Recap Card
            CyberCard(borderColor = if (preferences.eveningRecapEnabled) NeonAmber.copy(alpha = 0.5f) else CyberBorder) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.NightsStay, contentDescription = null, tint = NeonAmber, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Evening Progress Wrap-Up", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                                Text("Daily steps, total active calories & milestone recap", fontSize = 11.sp, color = TextMuted)
                            }
                        }
                        Switch(
                            checked = preferences.eveningRecapEnabled,
                            onCheckedChange = { onUpdate(preferences.copy(eveningRecapEnabled = it)) },
                            colors = SwitchDefaults.colors(checkedThumbColor = CyberBackground, checkedTrackColor = NeonAmber),
                            modifier = Modifier.testTag("evening_recap_switch")
                        )
                    }

                    if (preferences.eveningRecapEnabled) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Delivery Time:", fontSize = 12.sp, color = TextSecondary)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val eveningTimes = listOf(Pair(19, 0), Pair(20, 0), Pair(21, 0))
                                eveningTimes.forEach { (h, m) ->
                                    val isSelected = preferences.eveningHour == h && preferences.eveningMinute == m
                                    val displayH = if (h > 12) h - 12 else h
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { onUpdate(preferences.copy(eveningHour = h, eveningMinute = m)) },
                                        label = { Text("$displayH:00 PM", fontSize = 10.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = NeonAmber.copy(alpha = 0.2f),
                                            selectedLabelColor = NeonAmber,
                                            containerColor = CyberSurfaceElevated,
                                            labelColor = TextMuted
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = isSelected,
                                            borderColor = if (isSelected) NeonAmber else CyberBorder
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            // Inactivity Alert Card
            CyberCard(borderColor = if (preferences.inactivityAlertEnabled) NeonCoral.copy(alpha = 0.5f) else CyberBorder) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.DirectionsWalk, contentDescription = null, tint = NeonCoral, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Inactivity Recovery Nudge", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                                Text("Alerts when training habit lapses > 2 days", fontSize = 11.sp, color = TextMuted)
                            }
                        }
                        Switch(
                            checked = preferences.inactivityAlertEnabled,
                            onCheckedChange = { onUpdate(preferences.copy(inactivityAlertEnabled = it)) },
                            colors = SwitchDefaults.colors(checkedThumbColor = CyberBackground, checkedTrackColor = NeonCoral),
                            modifier = Modifier.testTag("inactivity_nudge_switch")
                        )
                    }

                    if (preferences.inactivityAlertEnabled) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Rest Threshold:", fontSize = 12.sp, color = TextSecondary)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(2, 3, 4).forEach { days ->
                                    val isSelected = preferences.inactivityThresholdDays == days
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { onUpdate(preferences.copy(inactivityThresholdDays = days)) },
                                        label = { Text("$days Days Rest", fontSize = 10.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = NeonCoral.copy(alpha = 0.2f),
                                            selectedLabelColor = NeonCoral,
                                            containerColor = CyberSurfaceElevated,
                                            labelColor = TextMuted
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = isSelected,
                                            borderColor = if (isSelected) NeonCoral else CyberBorder
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            // AI Tone Selection
            CyberCard {
                Column {
                    Text(
                        text = "AI COACHING REMINDER TONE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val tones = listOf(
                        "Scientific & High Performance",
                        "High-Energy Athletic",
                        "Calm & Mindful Endurance",
                        "Tough Love / Drill Sergeant"
                    )
                    tones.forEach { tone ->
                        val isSelected = preferences.aiPersonalizedTone == tone
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) NeonCyan.copy(alpha = 0.12f) else Color.Transparent)
                                .clickable { onUpdate(preferences.copy(aiPersonalizedTone = tone)) }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { onUpdate(preferences.copy(aiPersonalizedTone = tone)) },
                                colors = RadioButtonDefaults.colors(selectedColor = NeonCyan)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = tone, fontSize = 12.sp, color = if (isSelected) TextPrimary else TextSecondary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TestAndLogsTab(
    history: List<NotificationHistoryItem>,
    feedbackMessage: String?,
    onTestWorkoutAlarm: () -> Unit,
    onTestMorningBriefing: () -> Unit,
    onTestEveningRecap: () -> Unit,
    onTestInactivityAlert: () -> Unit,
    onClearLogs: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            // Test Notification Console
            CyberCard(borderColor = NeonPurple.copy(alpha = 0.5f)) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.SendToMobile, contentDescription = null, tint = NeonPurple, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "INSTANT PUSH NOTIFICATION TRIGGER",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    Text(
                        text = "Trigger immediate live Android notifications to test visual layout, sound, and quick action buttons:",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onTestWorkoutAlarm,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("test_push_workout_alarm"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                        ) {
                            Text("Workout Alert", color = CyberBackground, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onTestMorningBriefing,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("test_push_morning_briefing"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
                        ) {
                            Text("Morning Briefing", color = CyberBackground, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onTestEveningRecap,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("test_push_evening_recap"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonAmber)
                        ) {
                            Text("Evening Recap", color = CyberBackground, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onTestInactivityAlert,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("test_push_inactivity_alert"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCoral)
                        ) {
                            Text("Inactivity Nudge", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (feedbackMessage != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "✓ $feedbackMessage",
                            color = NeonGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "NOTIFICATION DISPATCH LOG (${history.size})",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
                if (history.isNotEmpty()) {
                    TextButton(
                        onClick = onClearLogs,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Clear Logs", color = NeonCoral, fontSize = 10.sp)
                    }
                }
            }
        }

        if (history.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No push notification events logged yet.", color = TextMuted, fontSize = 12.sp)
                }
            }
        } else {
            items(history) { logItem ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberSurfaceCard),
                    border = BorderStroke(1.dp, CyberBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(
                                    when (logItem.category) {
                                        "WORKOUT_REMINDER" -> NeonCyan.copy(alpha = 0.2f)
                                        "DAILY_BRIEFING" -> NeonGreen.copy(alpha = 0.2f)
                                        "TREND_UPDATE" -> NeonAmber.copy(alpha = 0.2f)
                                        else -> NeonCoral.copy(alpha = 0.2f)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (logItem.category) {
                                    "WORKOUT_REMINDER" -> Icons.AutoMirrored.Filled.DirectionsRun
                                    "DAILY_BRIEFING" -> Icons.Default.WbSunny
                                    "TREND_UPDATE" -> Icons.Default.Assessment
                                    else -> Icons.Default.Notifications
                                },
                                contentDescription = null,
                                tint = when (logItem.category) {
                                    "WORKOUT_REMINDER" -> NeonCyan
                                    "DAILY_BRIEFING" -> NeonGreen
                                    "TREND_UPDATE" -> NeonAmber
                                    else -> NeonCoral
                                },
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = logItem.title,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                                Text(
                                    text = sdf.format(Date(logItem.timestamp)),
                                    fontSize = 10.sp,
                                    color = TextMuted
                                )
                            }
                            Text(
                                text = logItem.message,
                                fontSize = 11.sp,
                                color = TextSecondary,
                                maxLines = 2
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddEditReminderModal(
    initialReminder: ScheduledWorkoutReminder?,
    onSave: (ScheduledWorkoutReminder) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(initialReminder?.title ?: "Morning Pace Run") }
    var selectedType by remember { mutableStateOf(initialReminder?.workoutType ?: WorkoutType.RUNNING) }
    var hour by remember { mutableIntStateOf(initialReminder?.hour ?: 7) }
    var minute by remember { mutableIntStateOf(initialReminder?.minute ?: 0) }
    var durationMin by remember { mutableIntStateOf(initialReminder?.targetDurationMin ?: 35) }
    var selectedDays by remember {
        mutableStateOf(
            initialReminder?.daysOfWeek ?: listOf(Calendar.MONDAY, Calendar.WEDNESDAY, Calendar.FRIDAY)
        )
    }
    var focusRationale by remember { mutableStateOf(initialReminder?.focusRationale ?: "Zone 2 Base Building") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("add_edit_reminder_modal"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CyberSurfaceElevated),
            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.6f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (initialReminder == null) "NEW WORKOUT REMINDER" else "EDIT REMINDER",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Reminder Title", fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = CyberBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Workout Type Selector
                Text("Workout Type:", fontSize = 11.sp, color = TextSecondary)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    items(WorkoutType.entries.toTypedArray()) { type ->
                        val isSelected = selectedType == type
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedType = type },
                            label = { Text(type.title, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonCyan.copy(alpha = 0.2f),
                                selectedLabelColor = NeonCyan,
                                containerColor = CyberSurfaceCard,
                                labelColor = TextMuted
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) NeonCyan else CyberBorder
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Time Selection (Hour / Minute / AM-PM)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Alarm Time:", fontSize = 11.sp, color = TextSecondary)

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val popularHours = listOf(Pair(6, 30), Pair(7, 0), Pair(7, 30), Pair(18, 0), Pair(19, 0))
                        popularHours.forEach { (h, m) ->
                            val isSel = hour == h && minute == m
                            val displayH = if (h > 12) h - 12 else if (h == 0) 12 else h
                            val amPm = if (h >= 12) "PM" else "AM"
                            FilterChip(
                                selected = isSel,
                                onClick = {
                                    hour = h
                                    minute = m
                                },
                                label = { Text("$displayH:${if (m == 0) "00" else "$m"} $amPm", fontSize = 9.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeonCyan.copy(alpha = 0.25f),
                                    selectedLabelColor = NeonCyan,
                                    containerColor = CyberSurfaceCard,
                                    labelColor = TextMuted
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSel,
                                    borderColor = if (isSel) NeonCyan else CyberBorder
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Repeat Days
                Text("Repeat on Days:", fontSize = 11.sp, color = TextSecondary)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val daysList = listOf(
                        Calendar.MONDAY to "Mon",
                        Calendar.TUESDAY to "Tue",
                        Calendar.WEDNESDAY to "Wed",
                        Calendar.THURSDAY to "Thu",
                        Calendar.FRIDAY to "Fri",
                        Calendar.SATURDAY to "Sat",
                        Calendar.SUNDAY to "Sun"
                    )
                    daysList.forEach { (calDay, name) ->
                        val isSelected = selectedDays.contains(calDay)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) NeonCyan else CyberSurfaceCard)
                                .border(1.dp, if (isSelected) NeonCyan else CyberBorder, RoundedCornerShape(6.dp))
                                .clickable {
                                    selectedDays = if (isSelected) {
                                        selectedDays - calDay
                                    } else {
                                        selectedDays + calDay
                                    }
                                }
                                .padding(horizontal = 6.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = name,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) CyberBackground else TextMuted
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Duration
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Target Duration:", fontSize = 11.sp, color = TextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(20, 30, 45, 60).forEach { dur ->
                            val isSel = durationMin == dur
                            FilterChip(
                                selected = isSel,
                                onClick = { durationMin = dur },
                                label = { Text("$dur min", fontSize = 9.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeonCyan.copy(alpha = 0.2f),
                                    selectedLabelColor = NeonCyan,
                                    containerColor = CyberSurfaceCard,
                                    labelColor = TextMuted
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSel,
                                    borderColor = if (isSel) NeonCyan else CyberBorder
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = TextMuted)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val reminder = ScheduledWorkoutReminder(
                                id = initialReminder?.id ?: System.currentTimeMillis(),
                                title = title.ifBlank { "${selectedType.title} Session" },
                                workoutType = selectedType,
                                hour = hour,
                                minute = minute,
                                daysOfWeek = if (selectedDays.isEmpty()) listOf(Calendar.MONDAY) else selectedDays,
                                targetDurationMin = durationMin,
                                isEnabled = true,
                                focusRationale = focusRationale
                            )
                            onSave(reminder)
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        modifier = Modifier.testTag("save_reminder_btn")
                    ) {
                        Text("Save Alarm", color = CyberBackground, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
