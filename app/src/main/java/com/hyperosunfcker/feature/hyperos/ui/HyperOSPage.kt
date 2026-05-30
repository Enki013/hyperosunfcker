package com.hyperosunfcker.feature.hyperos.ui

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DisplaySettings
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hyperosunfcker.feature.hyperos.preset.HyperOSNamedPreset
import com.hyperosunfcker.feature.hyperos.preset.HyperOSPreset
import com.hyperosunfcker.feature.hyperos.preset.HyperOSSnapshotManager
import com.hyperosunfcker.ui.component.IconClickButton
import com.hyperosunfcker.ui.component.ScreenTopBar
import com.hyperosunfcker.ui.component.SettingsItem
import com.hyperosunfcker.ui.dialog.ShizukuRequirementDialog
import com.hyperosunfcker.util.shizuku.ShizukuPermission

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HyperOSPage(
    onNavigateBack: () -> Unit,
    showBackButton: Boolean = true,
    viewModel: HyperOSViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    var showShizukuDialog by remember { mutableStateOf(false) }
    var showExperimentalVisualTweaks by remember { mutableStateOf(false) }
    var showPresetMenu by remember { mutableStateOf(false) }
    var showNamedPresetPage by remember { mutableStateOf(false) }
    val controlsEnabled = !uiState.isRunning && !uiState.isLoadingState

    LaunchedEffect(uiState.lastActionErrorMessage) {
        uiState.lastActionErrorMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(uiState.lastInfoMessage) {
        uiState.lastInfoMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        if (ShizukuPermission.isAppAuthorized()) {
            viewModel.loadState()
        } else {
            showShizukuDialog = true
        }
    }

    if (showShizukuDialog) {
        ShizukuRequirementDialog(
            shizukuStatus = ShizukuPermission.checkShizukuActive(context.packageManager),
            onClose = { granted ->
                showShizukuDialog = false
                if (granted || ShizukuPermission.isAppAuthorized()) {
                    viewModel.loadState()
                }
            }
        )
    }

    val runWithShizukuCheck = { action: () -> Unit ->
        if (!ShizukuPermission.isAppAuthorized()) {
            showShizukuDialog = true
        } else {
            action()
        }
    }

    if (showNamedPresetPage) {
        HyperOSNamedPresetsPage(
            controlsEnabled = controlsEnabled,
            viewModel = viewModel,
            onNavigateBack = { showNamedPresetPage = false },
            runWithShizukuCheck = runWithShizukuCheck
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                title = { Text("HyperOS Optimization") },
                navigationIcon = {
                    if (showBackButton) {
                        IconClickButton(
                            onClick = onNavigateBack,
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconClickButton(
                        onClick = { showPresetMenu = !showPresetMenu },
                        icon = Icons.Default.MoreVert,
                        contentDescription = "Presets"
                    )
                    HyperOSPresetMenu(
                        showMenu = showPresetMenu,
                        controlsEnabled = controlsEnabled,
                        onApplyPreset = { preset ->
                            runWithShizukuCheck { viewModel.applyPreset(preset) }
                        },
                        onOpenNamedPresets = {
                            showNamedPresetPage = true
                        },
                        onDismiss = { showPresetMenu = false }
                    )
                }
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = uiState.isLoadingState,
            onRefresh = { runWithShizukuCheck { viewModel.loadState(showToast = true) } },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {

            // --- Visual & Display ---
            Text(
                text = "Display",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
            )
            SettingsItem(
                title = "Force 120Hz Everywhere",
                description = "Bypasses system restrictions to lock refresh rate to 120Hz for all apps.",
                icon = Icons.Default.DisplaySettings,
                isSwitch = true,
                checked = uiState.is120HzForced,
                enabled = controlsEnabled,
                onCheckedChange = { runWithShizukuCheck { viewModel.toggle120Hz(it) } }
            )
            SettingsItem(
                title = "Stacked Recent Apps",
                description = "Writes global task_stack_view_layout_style 2 for supported System Launcher and POCO Launcher builds. Turning it off deletes the global setting.",
                icon = Icons.Default.DisplaySettings,
                isSwitch = true,
                checked = uiState.isStackedRecentsEnabled,
                enabled = controlsEnabled,
                onCheckedChange = { runWithShizukuCheck { viewModel.toggleStackedRecents(it) } }
            )

            SettingsItem(
                title = "Open Hidden Performance Menu",
                description = "Launches com.android.settings.fuelgauge.PowerModeSettings when the activity exists on the device.",
                icon = Icons.Default.Settings,
                enabled = controlsEnabled,
                onClick = { runWithShizukuCheck { viewModel.openHiddenPerformanceMenu() } }
            )

            // --- Performance Levels ---
            Text(
                text = "Performance Levels",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
            )
            LevelSelector(
                title = "CPU Computility Level",
                description = "Sets persist.sys.computility.cpulevel. 0 = stock/off, 1-2 = light boost, 3 = balanced, 4-5 = high, 6 = maximum CPU performance tier on supported HyperOS builds.",
                currentValue = uiState.cpuLevel,
                options = listOf(
                    LevelOption("0", 0),
                    LevelOption("1", 1),
                    LevelOption("2", 2),
                    LevelOption("3", 3),
                    LevelOption("4", 4),
                    LevelOption("5", 5),
                    LevelOption("6", 6)
                ),
                enabled = controlsEnabled,
                onSelect = { level -> runWithShizukuCheck { viewModel.setCpuLevel(level ?: 0) } }
            )
            LevelSelector(
                title = "GPU Computility Level",
                description = "Sets persist.sys.computility.gpulevel. 0 = stock/off, 1-2 = light graphics boost, 3 = balanced, 4-5 = high, 6 = maximum GPU/UI animation tier on supported HyperOS builds.",
                currentValue = uiState.gpuLevel,
                options = listOf(
                    LevelOption("0", 0),
                    LevelOption("1", 1),
                    LevelOption("2", 2),
                    LevelOption("3", 3),
                    LevelOption("4", 4),
                    LevelOption("5", 5),
                    LevelOption("6", 6)
                ),
                enabled = controlsEnabled,
                onSelect = { level -> runWithShizukuCheck { viewModel.setGpuLevel(level ?: 0) } }
            )

            // --- Memory & RAM ---
            Text(
                text = "Memory & Multitasking",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
            )
            SettingsItem(
                title = "Restrict PowerKeeper (AppOps)",
                description = "Revokes WRITE_SETTINGS and RUN_IN_BACKGROUND for com.miui.powerkeeper. Completely stops the aggressive killing of background apps.",
                icon = Icons.Default.Memory,
                isSwitch = true,
                checked = uiState.isPowerKeeperRestricted,
                enabled = controlsEnabled,
                onCheckedChange = { runWithShizukuCheck { viewModel.togglePowerKeeper(it) } }
            )
            LevelSelector(
                title = "Phantom Process Limit",
                description = "Modifies device_config activity_manager max_phantom_processes. Higher limits help heavy multitasking, terminal sessions, and emulators stay alive.",
                currentValue = uiState.phantomProcessLimit.takeIf { it > 32 },
                options = listOf(
                    LevelOption("Default", null),
                    LevelOption("128", 128),
                    LevelOption("512", 512),
                    LevelOption("1024", 1024)
                ),
                enabled = controlsEnabled,
                onSelect = { limit -> runWithShizukuCheck { viewModel.setPhantomProcessLimit(limit) } }
            )
            // --- Battery & Standby ---
            Text(
                text = "Battery & Standby (Wakelock Fixes)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
            )
            SettingsItem(
                title = "Optimize Doze Whitelist",
                description = "Removes Facebook and network-heavy services from Doze exemption to fix severe idle discharge (battery drain when screen is off).",
                icon = Icons.Default.BatteryChargingFull,
                isSwitch = true,
                checked = uiState.isDozeWhitelistOptimized,
                enabled = controlsEnabled,
                onCheckedChange = { runWithShizukuCheck { viewModel.toggleDozeWhitelist(it) } }
            )
            SettingsItem(
                title = "Restrict GMS Standby",
                description = "Sets Google Play Services to rare standby bucket to prevent constant awakening loops (wakelocks).",
                icon = Icons.Default.Security,
                isSwitch = true,
                checked = uiState.isGmsStandbyRestricted,
                enabled = controlsEnabled,
                onCheckedChange = { runWithShizukuCheck { viewModel.toggleGmsStandby(it) } }
            )
            SettingsItem(
                title = "VoLTE Carrier Check Code",
                description = "UI-only helper from the notes: dial *#*#86583#*#* in the Phone app to toggle carrier VoLTE checks.",
                icon = Icons.Default.Settings
            )
            
            // --- Debloat ---
            Text(
                text = "Telemetry",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
            )
            SettingsItem(
                title = "Freeze Telemetry & Ads",
                description = "Disables background analytic daemons (msa.global and miui.daemon) to protect privacy and save resources.",
                icon = Icons.Default.Security,
                isSwitch = true,
                checked = uiState.isTelemetryFrozen,
                enabled = controlsEnabled,
                onCheckedChange = { runWithShizukuCheck { viewModel.toggleTelemetry(it) } }
            )

            // --- Experimental Visual Tweaks ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showExperimentalVisualTweaks = !showExperimentalVisualTweaks
                            }
                    ) {
                        Text(
                            text = "Experimental Visual Tweaks",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (showExperimentalVisualTweaks) {
                                "Tap to collapse. These options are experimental and hidden by default."
                            } else {
                                "Hidden by default. Tap to expand advanced visual flags."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (showExperimentalVisualTweaks) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Experimental: these texture, blur, corner, and shadow flags can cause visual glitches, missing effects, lag, launcher issues, or no visible change on some HyperOS builds. They are not included in presets. Enable only one option at a time if you are testing.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                uriHandler.openUri(
                                    "https://www.reddit.com/r/PocoPhones/comments/1rs45hj/enable_advanced_textures_blur_on_any_xiaomi/?tl=tr"
                                )
                            },
                            enabled = controlsEnabled
                        ) {
                            Text("Open Visual Tweaks Guide")
                        }
                        SettingsItem(
                            title = "Control Center Blur (Glassy Blur)",
                            description = "Triggers service call miui.mqsas.IMQSNative 21 to enable advanced transparency (blur) across the Control Center and folders.",
                            icon = Icons.Default.Wallpaper,
                            isSwitch = true,
                            checked = uiState.isBlurEnabled,
                            enabled = controlsEnabled,
                            onCheckedChange = { runWithShizukuCheck { viewModel.toggleBlur(it) } }
                        )
                        LevelSelector(
                            title = "Advanced Visual Release",
                            description = "Sets persist.sys.advanced_visual_release. Higher values target newer HyperOS visual stacks and may be ignored or unstable on unsupported builds.",
                            currentValue = uiState.advancedVisualRelease,
                            options = listOf(
                                LevelOption("Off", 0),
                                LevelOption("HyperOS 2", 3),
                                LevelOption("HyperOS 3", 4)
                            ),
                            enabled = controlsEnabled,
                            onSelect = { level -> runWithShizukuCheck { viewModel.setAdvancedVisualRelease(level ?: 0) } }
                        )
                        SettingsItem(
                            title = "View Smooth Corners",
                            description = "Sets persist.sys.support_view_smoothcorner. Improves rounded corner rendering for supported HyperOS UI views.",
                            icon = Icons.Default.Wallpaper,
                            isSwitch = true,
                            checked = uiState.isViewSmoothCornerEnabled,
                            enabled = controlsEnabled,
                            onCheckedChange = { runWithShizukuCheck { viewModel.toggleViewSmoothCorner(it) } }
                        )
                        SettingsItem(
                            title = "Window Smooth Corners",
                            description = "Sets persist.sys.support_window_smoothcorner. Applies smoother rounded corners to system windows where the ROM supports it.",
                            icon = Icons.Default.Wallpaper,
                            isSwitch = true,
                            checked = uiState.isWindowSmoothCornerEnabled,
                            enabled = controlsEnabled,
                            onCheckedChange = { runWithShizukuCheck { viewModel.toggleWindowSmoothCorner(it) } }
                        )
                        SettingsItem(
                            title = "MI Shadow Renderer",
                            description = "Writes persist.sys.mi_shadow_supported to enable richer HyperOS interface shadows.",
                            icon = Icons.Default.Wallpaper,
                            isSwitch = true,
                            checked = uiState.isMiShadowEnabled,
                            enabled = controlsEnabled,
                            onCheckedChange = { runWithShizukuCheck { viewModel.toggleMiShadow(it) } }
                        )
                        SettingsItem(
                            title = "Default Blur Status",
                            description = "Sets persist.sys.background_blur_status_default. Makes supported blur surfaces default to enabled.",
                            icon = Icons.Default.Wallpaper,
                            isSwitch = true,
                            checked = uiState.isDefaultBlurStatusEnabled,
                            enabled = controlsEnabled,
                            onCheckedChange = { runWithShizukuCheck { viewModel.toggleDefaultBlurStatus(it) } }
                        )
                        SettingsItem(
                            title = "Blur Noise",
                            description = "Sets persist.sys.add_blurnoise_supported. Adds the noise/glass texture layer used by some HyperOS blur effects.",
                            icon = Icons.Default.Wallpaper,
                            isSwitch = true,
                            checked = uiState.isBlurNoiseEnabled,
                            enabled = controlsEnabled,
                            onCheckedChange = { runWithShizukuCheck { viewModel.toggleBlurNoise(it) } }
                        )
                        SettingsItem(
                            title = "Enhanced Device Level List",
                            description = "Writes deviceLevelList v:1,c:3,g:3. Can unlock folder blur, lock-screen visual animation, and launcher visual tiers.",
                            icon = Icons.Default.Wallpaper,
                            isSwitch = true,
                            checked = uiState.isEnhancedDeviceLevelListEnabled,
                            enabled = controlsEnabled,
                            onCheckedChange = { runWithShizukuCheck { viewModel.toggleEnhancedDeviceLevelList(it) } }
                        )
                        SettingsItem(
                            title = "Linkage State",
                            description = "Writes secure linkage_state 1. Reddit guide pairs it with the advanced texture stack on supported HyperOS builds.",
                            icon = Icons.Default.Wallpaper,
                            isSwitch = true,
                            checked = uiState.isLinkageStateEnabled,
                            enabled = controlsEnabled,
                            onCheckedChange = { runWithShizukuCheck { viewModel.toggleLinkageState(it) } }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
}

@Composable
private fun HyperOSPresetMenu(
    showMenu: Boolean,
    controlsEnabled: Boolean,
    onApplyPreset: (HyperOSPreset) -> Unit,
    onOpenNamedPresets: () -> Unit,
    onDismiss: () -> Unit
) {
    DropdownMenu(
        expanded = showMenu,
        onDismissRequest = onDismiss
    ) {
        DropdownMenuItem(
            text = { Text("Max Perf") },
            enabled = controlsEnabled,
            onClick = {
                onApplyPreset(HyperOSPreset.PERFORMANCE)
                onDismiss()
            }
        )
        DropdownMenuItem(
            text = { Text("Balanced") },
            enabled = controlsEnabled,
            onClick = {
                onApplyPreset(HyperOSPreset.BALANCED)
                onDismiss()
            }
        )
        DropdownMenuItem(
            text = { Text("Gaming") },
            enabled = controlsEnabled,
            onClick = {
                onApplyPreset(HyperOSPreset.GAMING)
                onDismiss()
            }
        )
        DropdownMenuItem(
            text = { Text("Battery") },
            enabled = controlsEnabled,
            onClick = {
                onApplyPreset(HyperOSPreset.BATTERY)
                onDismiss()
            }
        )
        DropdownMenuItem(
            text = { Text("Stock (Revert)") },
            enabled = controlsEnabled,
            onClick = {
                onApplyPreset(HyperOSPreset.STOCK)
                onDismiss()
            }
        )
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text("HyperOS Presets") },
            onClick = {
                onOpenNamedPresets()
                onDismiss()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HyperOSNamedPresetsPage(
    controlsEnabled: Boolean,
    viewModel: HyperOSViewModel,
    onNavigateBack: () -> Unit,
    runWithShizukuCheck: (() -> Unit) -> Unit
) {
    val context = LocalContext.current
    var presets by remember { mutableStateOf(HyperOSSnapshotManager.loadPresets(context)) }
    var editingPreset by remember { mutableStateOf<HyperOSNamedPreset?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    val refreshPresets = {
        presets = HyperOSSnapshotManager.loadPresets(context)
    }

    Scaffold(
        topBar = {
            ScreenTopBar(
                onNavigateBack = onNavigateBack,
                title = { Text("HyperOS Presets") }
            )
        },
        floatingActionButton = {
            if (presets.isNotEmpty()) {
                FloatingActionButton(onClick = { showCreateDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Create preset")
                }
            }
        }
    ) { padding ->
        if (presets.isEmpty()) {
            HyperOSEmptyPresetsState(onCreateClick = { showCreateDialog = true })
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(presets) { preset ->
                    HyperOSPresetCard(
                        preset = preset,
                        controlsEnabled = controlsEnabled,
                        formatDate = HyperOSSnapshotManager::formatDate,
                        onApply = {
                            runWithShizukuCheck {
                                viewModel.applyNamedPreset(preset)
                                onNavigateBack()
                            }
                        },
                        onEdit = { editingPreset = preset },
                        onDelete = {
                            if (HyperOSSnapshotManager.deletePreset(context, preset)) {
                                refreshPresets()
                                Toast.makeText(context, "Preset deleted", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Could not delete preset", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        HyperOSPresetEditDialog(
            title = "Create Preset",
            initialName = "",
            initialDescription = "",
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, description ->
                runWithShizukuCheck {
                    viewModel.saveNamedPreset(context, name, description) { saved ->
                        if (saved) {
                            refreshPresets()
                            showCreateDialog = false
                        }
                    }
                }
            }
        )
    }

    editingPreset?.let { preset ->
        HyperOSPresetEditDialog(
            title = "Edit Preset",
            initialName = preset.name,
            initialDescription = preset.description,
            onDismiss = { editingPreset = null },
            onConfirm = { name, description ->
                if (HyperOSSnapshotManager.updatePreset(context, preset, name, description)) {
                    refreshPresets()
                    editingPreset = null
                    Toast.makeText(context, "Preset updated", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Could not update preset", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

@Composable
private fun HyperOSEmptyPresetsState(onCreateClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(80.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "HyperOS Presets",
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "No HyperOS Presets",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Save the current HyperOS tuning state as a named preset and apply it later.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onCreateClick, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add, contentDescription = "Create preset")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create Preset")
        }
    }
}

@Composable
private fun HyperOSPresetCard(
    preset: HyperOSNamedPreset,
    controlsEnabled: Boolean,
    formatDate: (Long) -> String,
    onApply: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = preset.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (preset.description.isNotEmpty()) {
                        Text(
                            text = preset.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Captured settings",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "18 settings",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = formatDate(preset.createdDate),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                var showMenu by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "More options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            onClick = {
                                showMenu = false
                                onEdit()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Edit, contentDescription = "Edit")
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = {
                                showMenu = false
                                onDelete()
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            colors = MenuDefaults.itemColors(
                                textColor = MaterialTheme.colorScheme.error
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onApply,
                enabled = controlsEnabled,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Apply preset",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Apply Preset")
            }
        }
    }
}

@Composable
private fun HyperOSPresetEditDialog(
    title: String,
    initialName: String,
    initialDescription: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String, description: String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var description by remember { mutableStateOf(initialDescription) }
    var nameError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Name this HyperOS tuning preset. The current device state will be captured when you save.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = it.isBlank()
                    },
                    label = { Text("Preset name") },
                    placeholder = { Text("Gaming daily") },
                    isError = nameError,
                    supportingText = if (nameError) {
                        { Text("Preset name is required") }
                    } else {
                        null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Optional description") },
                    placeholder = { Text("What this preset is for") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name.trim(), description.trim())
                    } else {
                        nameError = true
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private data class LevelOption(
    val label: String,
    val value: Int?
)

@Composable
private fun LevelSelector(
    title: String,
    description: String,
    currentValue: Int?,
    options: List<LevelOption>,
    enabled: Boolean,
    onSelect: (Int?) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        var expanded by remember { mutableStateOf(false) }
        val selectedOption = options.firstOrNull { it.value == currentValue }
        val selectedLabel = selectedOption?.label ?: currentValue?.toString() ?: "Default"

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { expanded = true },
                enabled = enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("Selected: $selectedLabel")
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    val itemLabel = if (option.value == currentValue) {
                        "${option.label} • current"
                    } else {
                        option.label
                    }
                    DropdownMenuItem(
                        text = { Text(itemLabel) },
                        onClick = {
                            expanded = false
                            onSelect(option.value)
                        },
                        enabled = enabled
                    )
                }
            }
        }
    }
}
