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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hyperosunfcker.R
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
    val deviceStateRefreshedMessage = stringResource(R.string.hyperos_device_state_refreshed)
    val readDeviceStateErrorMessage = stringResource(R.string.hyperos_read_device_state_error)
    val commandFailedUnknownMessage = stringResource(R.string.hyperos_command_failed_unknown)
    val presetSavedMessage = stringResource(R.string.hyperos_preset_saved)
    val presetSaveErrorMessage = stringResource(R.string.hyperos_preset_save_error)

    LaunchedEffect(uiState.lastActionErrorMessage) {
        uiState.lastActionErrorMessage?.let { message ->
            val localizedMessage = when (message) {
                "Could not read current device state" -> readDeviceStateErrorMessage
                "Command failed for an unknown reason" -> commandFailedUnknownMessage
                "Could not save preset" -> presetSaveErrorMessage
                else -> message
            }
            Toast.makeText(context, localizedMessage, Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(uiState.lastInfoMessage) {
        uiState.lastInfoMessage?.let { message ->
            val localizedMessage = when (message) {
                "Device state refreshed" -> deviceStateRefreshedMessage
                "Preset saved" -> presetSavedMessage
                else -> message
            }
            Toast.makeText(context, localizedMessage, Toast.LENGTH_SHORT).show()
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
                title = { Text(stringResource(R.string.hyperos_optimization)) },
                navigationIcon = {
                    if (showBackButton) {
                        IconClickButton(
                            onClick = onNavigateBack,
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    IconClickButton(
                        onClick = { showPresetMenu = !showPresetMenu },
                        icon = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.presets)
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
                text = stringResource(R.string.hyperos_display),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
            )
            SettingsItem(
                title = stringResource(R.string.hyperos_force_120hz),
                description = stringResource(R.string.hyperos_force_120hz_description),
                icon = Icons.Default.DisplaySettings,
                isSwitch = true,
                checked = uiState.is120HzForced,
                enabled = controlsEnabled,
                onCheckedChange = { runWithShizukuCheck { viewModel.toggle120Hz(it) } }
            )
            SettingsItem(
                title = stringResource(R.string.hyperos_stacked_recent_apps),
                description = stringResource(R.string.hyperos_stacked_recent_apps_description),
                icon = Icons.Default.DisplaySettings,
                isSwitch = true,
                checked = uiState.isStackedRecentsEnabled,
                enabled = controlsEnabled,
                onCheckedChange = { runWithShizukuCheck { viewModel.toggleStackedRecents(it) } }
            )

            SettingsItem(
                title = stringResource(R.string.hyperos_open_hidden_performance_menu),
                description = stringResource(R.string.hyperos_open_hidden_performance_menu_description),
                icon = Icons.Default.Settings,
                enabled = controlsEnabled,
                onClick = { runWithShizukuCheck { viewModel.openHiddenPerformanceMenu() } }
            )

            // --- Performance Levels ---
            Text(
                text = stringResource(R.string.hyperos_performance_levels),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
            )
            LevelSelector(
                title = stringResource(R.string.hyperos_cpu_computility_level),
                description = stringResource(R.string.hyperos_cpu_computility_level_description),
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
                title = stringResource(R.string.hyperos_gpu_computility_level),
                description = stringResource(R.string.hyperos_gpu_computility_level_description),
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
                text = stringResource(R.string.hyperos_memory_multitasking),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
            )
            SettingsItem(
                title = stringResource(R.string.hyperos_restrict_powerkeeper),
                description = stringResource(R.string.hyperos_restrict_powerkeeper_description),
                icon = Icons.Default.Memory,
                isSwitch = true,
                checked = uiState.isPowerKeeperRestricted,
                enabled = controlsEnabled,
                onCheckedChange = { runWithShizukuCheck { viewModel.togglePowerKeeper(it) } }
            )
            LevelSelector(
                title = stringResource(R.string.hyperos_phantom_process_limit),
                description = stringResource(R.string.hyperos_phantom_process_limit_description),
                currentValue = uiState.phantomProcessLimit.takeIf { it > 32 },
                options = listOf(
                    LevelOption(stringResource(R.string.default_option), null),
                    LevelOption("128", 128),
                    LevelOption("512", 512),
                    LevelOption("1024", 1024)
                ),
                enabled = controlsEnabled,
                onSelect = { limit -> runWithShizukuCheck { viewModel.setPhantomProcessLimit(limit) } }
            )
            // --- Battery & Standby ---
            Text(
                text = stringResource(R.string.hyperos_battery_standby),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
            )
            SettingsItem(
                title = stringResource(R.string.hyperos_optimize_doze_whitelist),
                description = stringResource(R.string.hyperos_optimize_doze_whitelist_description),
                icon = Icons.Default.BatteryChargingFull,
                isSwitch = true,
                checked = uiState.isDozeWhitelistOptimized,
                enabled = controlsEnabled,
                onCheckedChange = { runWithShizukuCheck { viewModel.toggleDozeWhitelist(it) } }
            )
            SettingsItem(
                title = stringResource(R.string.hyperos_restrict_gms_standby),
                description = stringResource(R.string.hyperos_restrict_gms_standby_description),
                icon = Icons.Default.Security,
                isSwitch = true,
                checked = uiState.isGmsStandbyRestricted,
                enabled = controlsEnabled,
                onCheckedChange = { runWithShizukuCheck { viewModel.toggleGmsStandby(it) } }
            )
            SettingsItem(
                title = stringResource(R.string.hyperos_volte_carrier_check_code),
                description = stringResource(R.string.hyperos_volte_carrier_check_code_description),
                icon = Icons.Default.Settings
            )
            
            // --- Debloat ---
            Text(
                text = stringResource(R.string.hyperos_telemetry),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
            )
            SettingsItem(
                title = stringResource(R.string.hyperos_freeze_telemetry_ads),
                description = stringResource(R.string.hyperos_freeze_telemetry_ads_description),
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
                            text = stringResource(R.string.hyperos_experimental_visual_tweaks),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (showExperimentalVisualTweaks) {
                                stringResource(R.string.hyperos_experimental_visual_tweaks_collapse)
                            } else {
                                stringResource(R.string.hyperos_experimental_visual_tweaks_expand)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (showExperimentalVisualTweaks) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.hyperos_experimental_visual_tweaks_warning),
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
                            Text(stringResource(R.string.hyperos_open_visual_tweaks_guide))
                        }
                        SettingsItem(
                            title = stringResource(R.string.hyperos_control_center_blur),
                            description = stringResource(R.string.hyperos_control_center_blur_description),
                            icon = Icons.Default.Wallpaper,
                            isSwitch = true,
                            checked = uiState.isBlurEnabled,
                            enabled = controlsEnabled,
                            onCheckedChange = { runWithShizukuCheck { viewModel.toggleBlur(it) } }
                        )
                        LevelSelector(
                            title = stringResource(R.string.hyperos_advanced_visual_release),
                            description = stringResource(R.string.hyperos_advanced_visual_release_description),
                            currentValue = uiState.advancedVisualRelease,
                            options = listOf(
                                LevelOption(stringResource(R.string.off), 0),
                                LevelOption("HyperOS 2", 3),
                                LevelOption("HyperOS 3", 4)
                            ),
                            enabled = controlsEnabled,
                            onSelect = { level -> runWithShizukuCheck { viewModel.setAdvancedVisualRelease(level ?: 0) } }
                        )
                        SettingsItem(
                            title = stringResource(R.string.hyperos_view_smooth_corners),
                            description = stringResource(R.string.hyperos_view_smooth_corners_description),
                            icon = Icons.Default.Wallpaper,
                            isSwitch = true,
                            checked = uiState.isViewSmoothCornerEnabled,
                            enabled = controlsEnabled,
                            onCheckedChange = { runWithShizukuCheck { viewModel.toggleViewSmoothCorner(it) } }
                        )
                        SettingsItem(
                            title = stringResource(R.string.hyperos_window_smooth_corners),
                            description = stringResource(R.string.hyperos_window_smooth_corners_description),
                            icon = Icons.Default.Wallpaper,
                            isSwitch = true,
                            checked = uiState.isWindowSmoothCornerEnabled,
                            enabled = controlsEnabled,
                            onCheckedChange = { runWithShizukuCheck { viewModel.toggleWindowSmoothCorner(it) } }
                        )
                        SettingsItem(
                            title = stringResource(R.string.hyperos_mi_shadow_renderer),
                            description = stringResource(R.string.hyperos_mi_shadow_renderer_description),
                            icon = Icons.Default.Wallpaper,
                            isSwitch = true,
                            checked = uiState.isMiShadowEnabled,
                            enabled = controlsEnabled,
                            onCheckedChange = { runWithShizukuCheck { viewModel.toggleMiShadow(it) } }
                        )
                        SettingsItem(
                            title = stringResource(R.string.hyperos_default_blur_status),
                            description = stringResource(R.string.hyperos_default_blur_status_description),
                            icon = Icons.Default.Wallpaper,
                            isSwitch = true,
                            checked = uiState.isDefaultBlurStatusEnabled,
                            enabled = controlsEnabled,
                            onCheckedChange = { runWithShizukuCheck { viewModel.toggleDefaultBlurStatus(it) } }
                        )
                        SettingsItem(
                            title = stringResource(R.string.hyperos_blur_noise),
                            description = stringResource(R.string.hyperos_blur_noise_description),
                            icon = Icons.Default.Wallpaper,
                            isSwitch = true,
                            checked = uiState.isBlurNoiseEnabled,
                            enabled = controlsEnabled,
                            onCheckedChange = { runWithShizukuCheck { viewModel.toggleBlurNoise(it) } }
                        )
                        SettingsItem(
                            title = stringResource(R.string.hyperos_enhanced_device_level_list),
                            description = stringResource(R.string.hyperos_enhanced_device_level_list_description),
                            icon = Icons.Default.Wallpaper,
                            isSwitch = true,
                            checked = uiState.isEnhancedDeviceLevelListEnabled,
                            enabled = controlsEnabled,
                            onCheckedChange = { runWithShizukuCheck { viewModel.toggleEnhancedDeviceLevelList(it) } }
                        )
                        SettingsItem(
                            title = stringResource(R.string.hyperos_linkage_state),
                            description = stringResource(R.string.hyperos_linkage_state_description),
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
            text = { Text(stringResource(R.string.hyperos_preset_max_perf)) },
            enabled = controlsEnabled,
            onClick = {
                onApplyPreset(HyperOSPreset.PERFORMANCE)
                onDismiss()
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.hyperos_preset_balanced)) },
            enabled = controlsEnabled,
            onClick = {
                onApplyPreset(HyperOSPreset.BALANCED)
                onDismiss()
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.hyperos_preset_gaming)) },
            enabled = controlsEnabled,
            onClick = {
                onApplyPreset(HyperOSPreset.GAMING)
                onDismiss()
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.hyperos_preset_battery)) },
            enabled = controlsEnabled,
            onClick = {
                onApplyPreset(HyperOSPreset.BATTERY)
                onDismiss()
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.hyperos_preset_stock_revert)) },
            enabled = controlsEnabled,
            onClick = {
                onApplyPreset(HyperOSPreset.STOCK)
                onDismiss()
            }
        )
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text(stringResource(R.string.hyperos_presets)) },
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
    val presetDeletedMessage = stringResource(R.string.preset_deleted)
    val presetDeleteErrorMessage = stringResource(R.string.preset_delete_error)
    val presetUpdatedMessage = stringResource(R.string.hyperos_preset_updated)
    val presetUpdateErrorMessage = stringResource(R.string.hyperos_preset_update_error)

    val refreshPresets = {
        presets = HyperOSSnapshotManager.loadPresets(context)
    }

    Scaffold(
        topBar = {
            ScreenTopBar(
                onNavigateBack = onNavigateBack,
                title = { Text(stringResource(R.string.hyperos_presets)) }
            )
        },
        floatingActionButton = {
            if (presets.isNotEmpty()) {
                FloatingActionButton(onClick = { showCreateDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.create_preset))
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
                                Toast.makeText(context, presetDeletedMessage, Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, presetDeleteErrorMessage, Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        HyperOSPresetEditDialog(
            title = stringResource(R.string.create_preset),
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
            title = stringResource(R.string.hyperos_edit_preset),
            initialName = preset.name,
            initialDescription = preset.description,
            onDismiss = { editingPreset = null },
            onConfirm = { name, description ->
                if (HyperOSSnapshotManager.updatePreset(context, preset, name, description)) {
                    refreshPresets()
                    editingPreset = null
                    Toast.makeText(context, presetUpdatedMessage, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, presetUpdateErrorMessage, Toast.LENGTH_SHORT).show()
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
                    contentDescription = stringResource(R.string.hyperos_presets),
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.hyperos_no_presets),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stringResource(R.string.hyperos_no_presets_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onCreateClick, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.create_preset))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.create_preset))
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
                            contentDescription = stringResource(R.string.hyperos_captured_settings),
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.hyperos_settings_count, 18),
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
                            contentDescription = stringResource(R.string.more_options),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.edit)) },
                            onClick = {
                                showMenu = false
                                onEdit()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit))
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.delete)) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.delete),
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
                    contentDescription = stringResource(R.string.apply_preset),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.apply_preset))
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
                    text = stringResource(R.string.hyperos_preset_edit_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = it.isBlank()
                    },
                    label = { Text(stringResource(R.string.preset_name)) },
                    placeholder = { Text(stringResource(R.string.hyperos_preset_name_placeholder)) },
                    isError = nameError,
                    supportingText = if (nameError) {
                        { Text(stringResource(R.string.preset_name_missing_error)) }
                    } else {
                        null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.optional_description)) },
                    placeholder = { Text(stringResource(R.string.hyperos_preset_description_placeholder)) },
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
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
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
        val selectedLabel = selectedOption?.label ?: currentValue?.toString() ?: stringResource(R.string.default_option)

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
                Text(stringResource(R.string.hyperos_selected_value, selectedLabel))
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    val itemLabel = if (option.value == currentValue) {
                        stringResource(R.string.hyperos_current_option, option.label)
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
