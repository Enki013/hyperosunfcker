package com.hyperosunfcker.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.AutoDelete
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hyperosunfcker.R
import com.hyperosunfcker.extension.addAll
import com.hyperosunfcker.extension.showFor
import com.hyperosunfcker.packageName
import com.hyperosunfcker.ui.component.AppIconImage
import com.hyperosunfcker.ui.component.AppList
import com.hyperosunfcker.ui.component.DebloatTopBar
import com.hyperosunfcker.ui.component.fab.PresetEditFAB
import com.hyperosunfcker.ui.dialog.ExplainBadgesDialog
import com.hyperosunfcker.ui.dialog.NoWarrantyDialog
import com.hyperosunfcker.ui.dialog.ShizukuRequirementDialog
import com.hyperosunfcker.ui.dialog.SuccessDialog
import com.hyperosunfcker.ui.dialog.UninstallAppsDialog
import com.hyperosunfcker.ui.navigation.Screen
import com.hyperosunfcker.feature.hyperos.ui.HyperOSPage
import com.hyperosunfcker.ui.screen.LogsPage
import com.hyperosunfcker.ui.screen.PresetsPage
import com.hyperosunfcker.ui.screen.SettingsScreen
import com.hyperosunfcker.ui.viewmodel.AppListViewModel
import com.hyperosunfcker.ui.viewmodel.PresetsViewModel
import com.hyperosunfcker.ui.viewmodel.SettingsViewModel
import com.hyperosunfcker.ui.viewmodel.SettingsViewModelFactory
import com.hyperosunfcker.util.apps.Filter
import com.hyperosunfcker.util.shizuku.ShizukuPermission
import com.hyperosunfcker.util.showBiometricPrompt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val secretTaps = 12

@Composable
fun HyperOSUnfckerApp(
    canResetAppToFactory: (String) -> Boolean,
    uninstallApp: (String, Boolean) -> Boolean,
    reinstallApp: (String) -> Boolean,
    closeApp: () -> Unit,
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val resources = LocalResources.current

    val appListViewModel = viewModel<AppListViewModel>()
    val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModelFactory())
    val presetViewModel = viewModel<PresetsViewModel>()
    var versionTapCounter by remember { mutableIntStateOf(0) }
    val coroutineScope = rememberCoroutineScope()
    val bottomNavItems = listOf(
        BottomNavItem(Screen.HyperOS.route, stringResource(R.string.optimize), Icons.Default.Tune),
        BottomNavItem(Screen.Main.route, stringResource(R.string.debloat), Icons.Default.Apps),
        BottomNavItem(Screen.Logs.route, stringResource(R.string.logs), Icons.AutoMirrored.Filled.Article),
        BottomNavItem(Screen.Settings.route, stringResource(R.string.settings), Icons.Default.Settings),
    )
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = bottomNavItems.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(Screen.HyperOS.route) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(item.icon, contentDescription = item.label)
                            },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { appPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.HyperOS.route,
            modifier = Modifier.padding(appPadding)
        ) {
            composable(Screen.Main.route) {
                val presetSaveError = stringResource(R.string.preset_save_error)
                MainContent(
                    canResetAppToFactory = canResetAppToFactory,
                    uninstallApp = uninstallApp,
                    reinstallApp = reinstallApp,
                    navigateToPage = { navController.navigate(it) },
                    closeApp = closeApp,
                    presetEditMode = presetViewModel.editingPreset != null,
                    onPresetEditFinish = {
                        // Save all the selected apps to the preset
                        presetViewModel.editingPreset?.let { preset ->
                            presetViewModel.setPresetApps(
                                preset = preset,
                                newApps = appListViewModel.selectedApps.keys,
                                onSuccess = {
                                    appListViewModel.selectedApps.clear()
                                    presetViewModel.editingPreset = null
                                    navController.navigate(Screen.Presets.route)
                                },
                                onError = {
                                    // Show error message to user
                                    Toast.makeText(
                                        context,
                                        presetSaveError,
                                        Toast.LENGTH_LONG
                                    )
                                        .show()
                                }
                            )
                        }
                    },
                    enableSelectAll = versionTapCounter >= secretTaps,
                    appListViewModel = appListViewModel,
                    settingsViewModel = settingsViewModel,
                )
            }
            composable(route = Screen.HyperOS.route) {
                HyperOSPage(
                    onNavigateBack = { navController.navigateUp() },
                    showBackButton = false
                )
            }
            composable(route = Screen.Logs.route) {
                LogsPage(
                    onNavigateBack = { navController.navigateUp() },
                    showBackButton = false
                )
            }
            composable(route = Screen.Settings.route) {
                SettingsScreen(
                    onNavigateBack = { navController.navigateUp() },
                    settingsViewModel = settingsViewModel,
                    showBackButton = false,
                    onVersionTap = {
                        versionTapCounter += 1
                        coroutineScope.launch {
                            if (versionTapCounter > 6 && versionTapCounter < secretTaps) {
                                // Show quick toast
                                val remainingTaps = secretTaps - versionTapCounter
                                val message =
                                    resources.getString(R.string.select_all_tip, remainingTaps)
                                val toast = Toast.makeText(context, message, Toast.LENGTH_SHORT)
                                toast.showFor(500)
                            } else if (versionTapCounter >= secretTaps) {
                                // Enable select all functionality with quick toast
                                val toast =
                                    Toast.makeText(
                                        context,
                                        resources.getString(R.string.select_all_enabled),
                                        Toast.LENGTH_SHORT
                                    )
                                toast.showFor(500)
                            }
                        }
                    }
                )
            }

            composable(route = Screen.Presets.route) {
                PresetsPage(
                    presetViewModel = presetViewModel,
                    onNavigateBack = { preset ->
                        preset?.let {
                            appListViewModel.selectedApps.clear()
                            // Select apps in appListViewModel according to preset
                            appListViewModel.selectedApps.addAll(it.apps)
                        }

                        navController.navigateUp()
                    },
                    appListViewModel = appListViewModel,
                )
            }
        }
    }
}

private data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun MainContent(
    canResetAppToFactory: (String) -> Boolean,
    uninstallApp: (String, Boolean) -> Boolean,
    reinstallApp: (String) -> Boolean,
    onPresetEditFinish: () -> Unit,
    navigateToPage: (route: String) -> Unit,
    closeApp: () -> Unit,
    enableSelectAll: Boolean,
    presetEditMode: Boolean,
    appListViewModel: AppListViewModel,
    settingsViewModel: SettingsViewModel,
) {

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Selected tab
    var selectedAppsType by remember { mutableStateOf(AppsType.INSTALLED) }

    val disableRiskDialog by settingsViewModel.disableRiskDialog.collectAsStateWithLifecycle()
    val confirmBeforeUninstall by settingsViewModel.confirmBeforeUninstall.collectAsStateWithLifecycle()

    // Current active dialog
    var currentDialog by remember { mutableStateOf<(@Composable () -> Unit)?>(null) }

    if (!disableRiskDialog) {
        NoWarrantyDialog(
            onProceed = { neverShowAgain ->
                settingsViewModel.saveDisableRiskDialog(neverShowAgain)
            },
            onCancel = { closeApp() }
        )
    }

    val pagerState = rememberPagerState(pageCount = { AppsType.entries.size })

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            selectedAppsType = AppsType.entries[page]
            appListViewModel.selectedFilter = Filter.any
        }
    }

    val appIcon = remember(context) { context.packageManager.getApplicationIcon(packageName) }

    var showExplainBadgeDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            DebloatTopBar(
                openBadgesInfoDialog = { showExplainBadgeDialog = true },
                navigateToPage = navigateToPage,
                appListViewModel = appListViewModel,
                title = stringResource(R.string.debloat),
            )

            if (showExplainBadgeDialog) {
                ExplainBadgesDialog(onDismissRequest = { showExplainBadgeDialog = false })
            }
        },
        floatingActionButton = {
            AnimatedVisibility(
                // Make the FAB hidden if no apps are selected
                visible = appListViewModel.selectedApps.isNotEmpty(),
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                if (presetEditMode) {
                    PresetEditFAB(
                        onPresetEditFinish = onPresetEditFinish,
                    )
                } else {
                    FloatingActionButton(
                        containerColor =
                        if (selectedAppsType == AppsType.UNINSTALLED) {
                            MaterialTheme.colorScheme.tertiaryContainer
                        } else {

                            MaterialTheme.colorScheme.errorContainer
                        },
                        shape = RoundedCornerShape(32.dp),
                        modifier = Modifier
                            .padding(16.dp)
                            .navigationBarsPadding(),
                        onClick = {
                            if (appListViewModel.selectedApps.contains(packageName)) {
                                Toast.makeText(
                                    context,
                                    "This app cannot remove itself.",
                                    Toast.LENGTH_SHORT
                                )
                                    .show()

                                return@FloatingActionButton
                            }

                            val uninstallApps = {
                                val uninstall = { resetToFactory: Boolean ->
                                    val process = {
                                        coroutineScope.launch {
                                            val uninstalled = uninstallOrReinstall(
                                                uninstallApp = uninstallApp,
                                                reinstallApp = reinstallApp,
                                                selectedAppsType = selectedAppsType,
                                                appListViewModel = appListViewModel,
                                                resetToFactory = resetToFactory,
                                            )

                                            if (uninstalled > 0 && !settingsViewModel.hideSuccessDialog.value) {
                                                currentDialog = {
                                                    SuccessDialog(
                                                        count = uninstalled,
                                                        isReinstall = selectedAppsType == AppsType.UNINSTALLED,
                                                        onDismissRequest = {
                                                            currentDialog = null
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    if (settingsViewModel.authEnabled.value) {
                                        showBiometricPrompt(context) { process() }
                                    } else {
                                        process()
                                    }
                                }

                                // Show confirmation dialog.
                                if (selectedAppsType == AppsType.INSTALLED && confirmBeforeUninstall) {
                                    if (appListViewModel.selectedApps.isNotEmpty()) {
                                        // TO-Do Consider refactoring dialog management.
                                        // This `currentDialog`
                                        // currentDialog(@Composable) could potentially be
                                        // replaced by a simpler state
                                        // Haven't touched it as for now due to its role in
                                        // core uninstall flow.
                                        currentDialog = {
                                            val canResetAny =
                                                appListViewModel.selectedApps.keys.any {
                                                    canResetAppToFactory(it)
                                                }

                                            UninstallAppsDialog(
                                                appCount =
                                                appListViewModel
                                                    .selectedApps
                                                    .size,
                                                canResetToFactory = canResetAny,
                                                onDismiss = { currentDialog = null },
                                                onAgree = { resetToFactory ->
                                                    currentDialog = null
                                                    uninstall(resetToFactory)
                                                }
                                            )
                                        }
                                    }
                                } else {
                                    // Trigger uninstall
                                    uninstall(false)
                                }
                            }

                            // Show dialog before uninstalling if we are on the "installed"
                            // tab
                            // However, do not show it if user has disabled the dialog in
                            // settings
                            // or if we are on the "uninstalled" tab
                            if (!ShizukuPermission.isAppAuthorized()) {
                                currentDialog = {
                                    ShizukuRequirementDialog(
                                        shizukuStatus =
                                        ShizukuPermission.checkShizukuActive(
                                            context.packageManager
                                        ),
                                        onClose = { proceed ->
                                            currentDialog = null

                                            if (proceed) {
                                                uninstallApps()
                                            }
                                        }
                                    )
                                }
                            } else {
                                uninstallApps()
                            }
                        },
                    ) {
                        when (selectedAppsType) {
                            AppsType.INSTALLED ->
                                if (appListViewModel.selectedApps.contains(packageName)) {
                                    AppIconImage(
                                        appIconImage = appIcon,
                                        contentDescription =
                                        stringResource(R.string.app_name)
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription =
                                        stringResource(R.string.uninstall)
                                    )
                                }

                            AppsType.UNINSTALLED ->
                                Icon(
                                    Icons.Default.InstallMobile,
                                    contentDescription =
                                    stringResource(R.string.reinstall)
                                )
                        }
                    }
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            TabRow(
                selectedTabIndex = selectedAppsType.ordinal,
                contentColor = MaterialTheme.colorScheme.primary,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ) {
                AppsType.entries.forEach { currentTab ->
                    Tab(
                        selected = selectedAppsType == currentTab,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(currentTab.ordinal)
                                appListViewModel.selectedFilter = Filter.any
                            }
                            selectedAppsType = currentTab
                        },
                        icon = {
                            Icon(currentTab.icon, contentDescription = currentTab.toString())
                        },
                    )
                }
            }
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) { page ->
                // Show active dialog
                currentDialog?.let { it() }

                var isRefreshing by remember { mutableStateOf(false) }

                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = {
                        coroutineScope.launch {
                            isRefreshing = true
                            appListViewModel.loadInstalled(
                                packageManager = context.packageManager,
                                context = context,
                            )
                            isRefreshing = false
                        }
                    },
                ) {
                    AppList(
                        appType = AppsType.entries[page],
                        appListModel = appListViewModel,
                        settingsViewModel = settingsViewModel,
                        enableSelectAll = enableSelectAll,
                    )
                }
            }
        }
    }
}

suspend fun uninstallOrReinstall(
    uninstallApp: (String, Boolean) -> Boolean,
    reinstallApp: (String) -> Boolean,
    selectedAppsType: AppsType,
    appListViewModel: AppListViewModel,
    resetToFactory: Boolean = false,
): Int {
    val appsToProcess = appListViewModel.selectedApps.keys.toList()
    var count = 0
    withContext(Dispatchers.IO) {
        when (selectedAppsType) {
            AppsType.INSTALLED -> {
                appsToProcess.forEach { app ->
                    val uninstalled = uninstallApp(app, resetToFactory)
                    if (uninstalled) {
                        with(Dispatchers.Main) {
                            count += 1
                            appListViewModel.changeAppStatus(app)
                            appListViewModel.selectedApps.remove(app)
                        }
                    }
                }
            }

            AppsType.UNINSTALLED -> {
                appsToProcess.forEach { app ->
                    val installed = reinstallApp(app)
                    if (installed) {
                        with(Dispatchers.Main) {
                            count += 1
                            appListViewModel.changeAppStatus(app)
                            appListViewModel.selectedApps.remove(app)
                        }
                    }
                }
            }
        }
    }

    return count
}

enum class AppsType(val icon: ImageVector) {
    INSTALLED(Icons.Default.AutoDelete),
    UNINSTALLED(Icons.Default.DeleteForever),
}
