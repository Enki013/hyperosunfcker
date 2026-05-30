package com.hyperosunfcker.feature.hyperos.preset

import com.hyperosunfcker.feature.hyperos.state.HyperOSStateEngine

data class HyperOSSnapshot(
    val cpuLevel: Int,
    val gpuLevel: Int,
    val isBlurEnabled: Boolean,
    val advancedVisualRelease: Int,
    val isViewSmoothCornerEnabled: Boolean,
    val isWindowSmoothCornerEnabled: Boolean,
    val isMiShadowEnabled: Boolean,
    val isDefaultBlurStatusEnabled: Boolean,
    val isBlurNoiseEnabled: Boolean,
    val isEnhancedDeviceLevelListEnabled: Boolean,
    val isLinkageStateEnabled: Boolean,
    val isStackedRecentsEnabled: Boolean,
    val isPowerKeeperRestricted: Boolean,
    val phantomProcessLimit: Int,
    val isDozeWhitelistOptimized: Boolean,
    val isGmsStandbyRestricted: Boolean,
    val is120HzForced: Boolean,
    val isTelemetryFrozen: Boolean
) {
    companion object {
        fun captureCurrentState(): HyperOSSnapshot {
            return HyperOSSnapshot(
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
    }

    fun applyState(): Boolean {
        val phantomLimitApplied =
            if (this.phantomProcessLimit == 32 || this.phantomProcessLimit == 0) {
                HyperOSStateEngine.setPhantomProcessLimit(null)
            } else {
                HyperOSStateEngine.setPhantomProcessLimit(this.phantomProcessLimit)
            }

        return listOf(
            HyperOSStateEngine.setCpuLevel(this.cpuLevel),
            HyperOSStateEngine.setGpuLevel(this.gpuLevel),
            HyperOSStateEngine.setBlurEnabled(this.isBlurEnabled),
            HyperOSStateEngine.setAdvancedVisualRelease(this.advancedVisualRelease),
            HyperOSStateEngine.setViewSmoothCornerEnabled(this.isViewSmoothCornerEnabled),
            HyperOSStateEngine.setWindowSmoothCornerEnabled(this.isWindowSmoothCornerEnabled),
            HyperOSStateEngine.setMiShadowEnabled(this.isMiShadowEnabled),
            HyperOSStateEngine.setDefaultBlurStatusEnabled(this.isDefaultBlurStatusEnabled),
            HyperOSStateEngine.setBlurNoiseEnabled(this.isBlurNoiseEnabled),
            HyperOSStateEngine.setEnhancedDeviceLevelListEnabled(this.isEnhancedDeviceLevelListEnabled),
            HyperOSStateEngine.setLinkageStateEnabled(this.isLinkageStateEnabled),
            HyperOSStateEngine.setStackedRecentsEnabled(this.isStackedRecentsEnabled),
            HyperOSStateEngine.setPowerKeeperRestricted(this.isPowerKeeperRestricted),
            phantomLimitApplied,
            HyperOSStateEngine.setDozeWhitelistOptimized(this.isDozeWhitelistOptimized),
            HyperOSStateEngine.setGmsStandbyRestricted(this.isGmsStandbyRestricted),
            HyperOSStateEngine.setForce120Hz(this.is120HzForced),
            HyperOSStateEngine.setTelemetryFrozen(this.isTelemetryFrozen)
        ).all { it }
    }
}
