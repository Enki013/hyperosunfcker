package com.hyperosunfcker.feature.hyperos.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hyperosunfcker.feature.hyperos.commands.HyperOSCommandUtils
import com.hyperosunfcker.feature.hyperos.preset.HyperOSNamedPreset
import com.hyperosunfcker.feature.hyperos.preset.HyperOSPreset
import com.hyperosunfcker.feature.hyperos.preset.HyperOSPresetEngine
import com.hyperosunfcker.feature.hyperos.preset.HyperOSSnapshotManager
import com.hyperosunfcker.feature.hyperos.preset.HyperOSSnapshot
import com.hyperosunfcker.feature.hyperos.state.HyperOSStateEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HyperOSViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HyperOSUiState())
    val uiState: StateFlow<HyperOSUiState> = _uiState.asStateFlow()

    fun loadState(showToast: Boolean = false) {
        if (_uiState.value.isLoadingState || _uiState.value.isRunning) {
            return
        }

        _uiState.update {
            it.copy(
                isLoadingState = true,
                lastActionSucceeded = null,
                lastInfoMessage = null,
                lastActionErrorMessage = null
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val loadedState = readDeviceState()
                _uiState.update { currentState ->
                    loadedState.copy(
                        isRunning = currentState.isRunning,
                        isLoadingState = false,
                        isDeviceStateLoaded = true,
                        lastActionSucceeded = true,
                        lastInfoMessage = if (showToast) "Device state refreshed" else null,
                        lastActionErrorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { currentState ->
                    currentState.copy(
                        isLoadingState = false,
                        isDeviceStateLoaded = false,
                        lastActionSucceeded = false,
                        lastInfoMessage = null,
                        lastActionErrorMessage = e.message
                            ?: HyperOSCommandUtils.lastFailureMessage
                            ?: "Could not read current device state"
                    )
                }
            }
        }
    }

    private fun readDeviceState(): HyperOSUiState {
        return HyperOSUiState(
            cpuLevel = HyperOSStateEngine.getCpuLevel(),
            gpuLevel = HyperOSStateEngine.getGpuLevel(),
            isBlurEnabled = HyperOSStateEngine.isBlurEnabled(),
            advancedVisualRelease = HyperOSStateEngine.getAdvancedVisualRelease(),
            isViewSmoothCornerEnabled = HyperOSStateEngine.isViewSmoothCornerEnabled(),
            isWindowSmoothCornerEnabled = HyperOSStateEngine.isWindowSmoothCornerEnabled(),
            isMiShadowEnabled = HyperOSStateEngine.isMiShadowEnabled(),
            isDefaultBlurStatusEnabled = HyperOSStateEngine.isDefaultBlurStatusEnabled(),
            isBlurNoiseEnabled = HyperOSStateEngine.isBlurNoiseEnabled(),
            isEnhancedDeviceLevelListEnabled = HyperOSStateEngine.isEnhancedDeviceLevelListEnabled(),
            isLinkageStateEnabled = HyperOSStateEngine.isLinkageStateEnabled(),
            isStackedRecentsEnabled = HyperOSStateEngine.isStackedRecentsEnabled(),
            isPowerKeeperRestricted = HyperOSStateEngine.isPowerKeeperRestricted(),
            phantomProcessLimit = HyperOSStateEngine.getPhantomProcessLimit(),
            isDozeWhitelistOptimized = HyperOSStateEngine.isDozeWhitelistOptimized(),
            isGmsStandbyRestricted = HyperOSStateEngine.isGmsStandbyRestricted(),
            is120HzForced = HyperOSStateEngine.is120HzForced(),
            isTelemetryFrozen = HyperOSStateEngine.isTelemetryFrozen()
        )
    }

    private fun runShellAction(
        onSuccess: (HyperOSUiState) -> HyperOSUiState,
        action: () -> Boolean
    ) {
        val previousState = _uiState.value
        if (previousState.isRunning) {
            return
        }

        _uiState.value = onSuccess(previousState).copy(
            isRunning = true,
            lastActionSucceeded = null,
            lastInfoMessage = null,
            lastActionErrorMessage = null
        )

        viewModelScope.launch(Dispatchers.IO) {
            val success = action()
            val errorMessage = if (success) null else {
                HyperOSCommandUtils.lastFailureMessage ?: "Command failed for an unknown reason"
            }
            _uiState.update { currentState ->
                val finalState = if (success) currentState else previousState
                finalState.copy(
                    isRunning = false,
                    lastActionSucceeded = success,
                    lastActionErrorMessage = errorMessage
                )
            }
        }
    }

    fun setCpuLevel(level: Int) {
        runShellAction(
            onSuccess = { it.copy(cpuLevel = level) },
            action = { HyperOSStateEngine.setCpuLevel(level) }
        )
    }

    fun setGpuLevel(level: Int) {
        runShellAction(
            onSuccess = { it.copy(gpuLevel = level) },
            action = { HyperOSStateEngine.setGpuLevel(level) }
        )
    }

    fun toggleBlur(enabled: Boolean) {
        runShellAction(
            onSuccess = { it.copy(isBlurEnabled = enabled) },
            action = { HyperOSStateEngine.setBlurEnabled(enabled) }
        )
    }

    fun setAdvancedVisualRelease(level: Int) {
        runShellAction(
            onSuccess = { it.copy(advancedVisualRelease = level) },
            action = { HyperOSStateEngine.setAdvancedVisualRelease(level) }
        )
    }

    fun toggleViewSmoothCorner(enabled: Boolean) {
        runShellAction(
            onSuccess = { it.copy(isViewSmoothCornerEnabled = enabled) },
            action = { HyperOSStateEngine.setViewSmoothCornerEnabled(enabled) }
        )
    }

    fun toggleWindowSmoothCorner(enabled: Boolean) {
        runShellAction(
            onSuccess = { it.copy(isWindowSmoothCornerEnabled = enabled) },
            action = { HyperOSStateEngine.setWindowSmoothCornerEnabled(enabled) }
        )
    }

    fun toggleMiShadow(enabled: Boolean) {
        runShellAction(
            onSuccess = { it.copy(isMiShadowEnabled = enabled) },
            action = { HyperOSStateEngine.setMiShadowEnabled(enabled) }
        )
    }

    fun toggleDefaultBlurStatus(enabled: Boolean) {
        runShellAction(
            onSuccess = { it.copy(isDefaultBlurStatusEnabled = enabled) },
            action = { HyperOSStateEngine.setDefaultBlurStatusEnabled(enabled) }
        )
    }

    fun toggleBlurNoise(enabled: Boolean) {
        runShellAction(
            onSuccess = { it.copy(isBlurNoiseEnabled = enabled) },
            action = { HyperOSStateEngine.setBlurNoiseEnabled(enabled) }
        )
    }

    fun toggleEnhancedDeviceLevelList(enabled: Boolean) {
        runShellAction(
            onSuccess = { it.copy(isEnhancedDeviceLevelListEnabled = enabled) },
            action = { HyperOSStateEngine.setEnhancedDeviceLevelListEnabled(enabled) }
        )
    }

    fun toggleLinkageState(enabled: Boolean) {
        runShellAction(
            onSuccess = { it.copy(isLinkageStateEnabled = enabled) },
            action = { HyperOSStateEngine.setLinkageStateEnabled(enabled) }
        )
    }

    fun toggleStackedRecents(enabled: Boolean) {
        runShellAction(
            onSuccess = { it.copy(isStackedRecentsEnabled = enabled) },
            action = { HyperOSStateEngine.setStackedRecentsEnabled(enabled) }
        )
    }

    fun togglePowerKeeper(restricted: Boolean) {
        runShellAction(
            onSuccess = { it.copy(isPowerKeeperRestricted = restricted) },
            action = { HyperOSStateEngine.setPowerKeeperRestricted(restricted) }
        )
    }

    fun setPhantomProcessLimit(limit: Int?) {
        runShellAction(
            onSuccess = { it.copy(phantomProcessLimit = limit ?: 32) },
            action = { HyperOSStateEngine.setPhantomProcessLimit(limit) }
        )
    }

    fun toggleDozeWhitelist(optimized: Boolean) {
        runShellAction(
            onSuccess = { it.copy(isDozeWhitelistOptimized = optimized) },
            action = { HyperOSStateEngine.setDozeWhitelistOptimized(optimized) }
        )
    }

    fun toggleGmsStandby(restricted: Boolean) {
        runShellAction(
            onSuccess = { it.copy(isGmsStandbyRestricted = restricted) },
            action = { HyperOSStateEngine.setGmsStandbyRestricted(restricted) }
        )
    }

    fun toggle120Hz(forced: Boolean) {
        runShellAction(
            onSuccess = { it.copy(is120HzForced = forced) },
            action = { HyperOSStateEngine.setForce120Hz(forced) }
        )
    }

    fun openHiddenPerformanceMenu() {
        runShellAction(
            onSuccess = { it },
            action = { HyperOSStateEngine.openHiddenPerformanceMenu() }
        )
    }

    fun toggleTelemetry(frozen: Boolean) {
        runShellAction(
            onSuccess = { it.copy(isTelemetryFrozen = frozen) },
            action = { HyperOSStateEngine.setTelemetryFrozen(frozen) }
        )
    }

    fun applyPreset(preset: HyperOSPreset) {
        runShellAction(
            onSuccess = { preset.toUiState(isRunning = false, lastActionSucceeded = true) },
            action = { HyperOSPresetEngine.applyPreset(preset) }
        )
    }

    fun applyNamedPreset(preset: HyperOSNamedPreset) {
        runShellAction(
            onSuccess = { preset.snapshot.toUiState(isRunning = false, lastActionSucceeded = true) },
            action = { preset.snapshot.applyState() }
        )
    }

    fun saveNamedPreset(
        context: Context,
        name: String,
        description: String,
        onComplete: (Boolean) -> Unit
    ) {
        val appContext = context.applicationContext
        _uiState.update {
            it.copy(
                lastInfoMessage = null,
                lastActionErrorMessage = null
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            val saved = HyperOSSnapshotManager.savePreset(appContext, name, description)
            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        lastInfoMessage = if (saved) "Preset saved" else null,
                        lastActionErrorMessage = if (saved) null else "Could not save preset"
                    )
                }
                onComplete(saved)
            }
        }
    }

    private fun HyperOSPreset.toUiState(
        isRunning: Boolean,
        lastActionSucceeded: Boolean?
    ): HyperOSUiState {
        return when (this) {
            HyperOSPreset.PERFORMANCE ->
                HyperOSUiState(
                    cpuLevel = 6,
                    gpuLevel = 6,
                    isPowerKeeperRestricted = true,
                    phantomProcessLimit = 512,
                    is120HzForced = true,
                    isTelemetryFrozen = true,
                    isDeviceStateLoaded = true,
                    isRunning = isRunning,
                    lastActionSucceeded = lastActionSucceeded
                )
            HyperOSPreset.BALANCED ->
                HyperOSUiState(
                    cpuLevel = 3,
                    gpuLevel = 3,
                    phantomProcessLimit = 128,
                    isTelemetryFrozen = true,
                    isDeviceStateLoaded = true,
                    isRunning = isRunning,
                    lastActionSucceeded = lastActionSucceeded
                )
            HyperOSPreset.BATTERY ->
                HyperOSUiState(
                    isDozeWhitelistOptimized = true,
                    isGmsStandbyRestricted = true,
                    isTelemetryFrozen = true,
                    isDeviceStateLoaded = true,
                    isRunning = isRunning,
                    lastActionSucceeded = lastActionSucceeded
                )
            HyperOSPreset.GAMING ->
                HyperOSUiState(
                    cpuLevel = 6,
                    gpuLevel = 6,
                    phantomProcessLimit = 1024,
                    isPowerKeeperRestricted = true,
                    is120HzForced = true,
                    isTelemetryFrozen = true,
                    isDeviceStateLoaded = true,
                    isRunning = isRunning,
                    lastActionSucceeded = lastActionSucceeded
                )
            HyperOSPreset.STOCK ->
                HyperOSUiState(
                    isDeviceStateLoaded = true,
                    isRunning = isRunning,
                    lastActionSucceeded = lastActionSucceeded
                )
        }
    }

    private fun HyperOSSnapshot.toUiState(
        isRunning: Boolean,
        lastActionSucceeded: Boolean?
    ): HyperOSUiState {
        return HyperOSUiState(
            cpuLevel = cpuLevel,
            gpuLevel = gpuLevel,
            isBlurEnabled = isBlurEnabled,
            advancedVisualRelease = advancedVisualRelease,
            isViewSmoothCornerEnabled = isViewSmoothCornerEnabled,
            isWindowSmoothCornerEnabled = isWindowSmoothCornerEnabled,
            isMiShadowEnabled = isMiShadowEnabled,
            isDefaultBlurStatusEnabled = isDefaultBlurStatusEnabled,
            isBlurNoiseEnabled = isBlurNoiseEnabled,
            isEnhancedDeviceLevelListEnabled = isEnhancedDeviceLevelListEnabled,
            isLinkageStateEnabled = isLinkageStateEnabled,
            isStackedRecentsEnabled = isStackedRecentsEnabled,
            isPowerKeeperRestricted = isPowerKeeperRestricted,
            phantomProcessLimit = phantomProcessLimit,
            isDozeWhitelistOptimized = isDozeWhitelistOptimized,
            isGmsStandbyRestricted = isGmsStandbyRestricted,
            is120HzForced = is120HzForced,
            isTelemetryFrozen = isTelemetryFrozen,
            isDeviceStateLoaded = true,
            isRunning = isRunning,
            lastActionSucceeded = lastActionSucceeded
        )
    }
}

data class HyperOSUiState(
    val cpuLevel: Int = 0,
    val gpuLevel: Int = 0,
    val isBlurEnabled: Boolean = false,
    val advancedVisualRelease: Int = 0,
    val isViewSmoothCornerEnabled: Boolean = false,
    val isWindowSmoothCornerEnabled: Boolean = false,
    val isMiShadowEnabled: Boolean = false,
    val isDefaultBlurStatusEnabled: Boolean = false,
    val isBlurNoiseEnabled: Boolean = false,
    val isEnhancedDeviceLevelListEnabled: Boolean = false,
    val isLinkageStateEnabled: Boolean = false,
    val isStackedRecentsEnabled: Boolean = false,
    val isPowerKeeperRestricted: Boolean = false,
    val phantomProcessLimit: Int = 32,
    val isDozeWhitelistOptimized: Boolean = false,
    val isGmsStandbyRestricted: Boolean = false,
    val is120HzForced: Boolean = false,
    val isTelemetryFrozen: Boolean = false,
    val isLoadingState: Boolean = false,
    val isDeviceStateLoaded: Boolean = false,
    val isRunning: Boolean = false,
    val lastActionSucceeded: Boolean? = null,
    val lastInfoMessage: String? = null,
    val lastActionErrorMessage: String? = null
)
