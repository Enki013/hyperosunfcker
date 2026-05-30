package com.hyperosunfcker.feature.hyperos.preset

import com.hyperosunfcker.feature.hyperos.state.HyperOSStateEngine

enum class HyperOSPreset {
    PERFORMANCE,
    BALANCED,
    BATTERY,
    GAMING,
    STOCK
}

object HyperOSPresetEngine {
    
    fun applyPreset(preset: HyperOSPreset): Boolean {
        return when (preset) {
            HyperOSPreset.PERFORMANCE -> applyPerformancePreset()
            HyperOSPreset.BALANCED -> applyBalancedPreset()
            HyperOSPreset.BATTERY -> applyBatteryPreset()
            HyperOSPreset.GAMING -> applyGamingPreset()
            HyperOSPreset.STOCK -> applyStockPreset()
        }
    }

    private fun applyPerformancePreset(): Boolean {
        return listOf(
            HyperOSStateEngine.setCpuLevel(6),
            HyperOSStateEngine.setGpuLevel(6),
            HyperOSStateEngine.setPhantomProcessLimit(512),
            HyperOSStateEngine.setForce120Hz(true),
            HyperOSStateEngine.setPowerKeeperRestricted(true),
            HyperOSStateEngine.setTelemetryFrozen(true)
        ).all { it }
    }

    private fun applyBalancedPreset(): Boolean {
        return listOf(
            HyperOSStateEngine.setCpuLevel(3),
            HyperOSStateEngine.setGpuLevel(3),
            HyperOSStateEngine.setPhantomProcessLimit(128),
            HyperOSStateEngine.setForce120Hz(false),
            HyperOSStateEngine.setPowerKeeperRestricted(false),
            HyperOSStateEngine.setTelemetryFrozen(true)
        ).all { it }
    }

    private fun applyBatteryPreset(): Boolean {
        return listOf(
            HyperOSStateEngine.setCpuLevel(0),
            HyperOSStateEngine.setGpuLevel(0),
            HyperOSStateEngine.setDozeWhitelistOptimized(true),
            HyperOSStateEngine.setGmsStandbyRestricted(true),
            HyperOSStateEngine.setForce120Hz(false),
            HyperOSStateEngine.setPowerKeeperRestricted(false),
            HyperOSStateEngine.setTelemetryFrozen(true)
        ).all { it }
    }

    private fun applyGamingPreset(): Boolean {
        return listOf(
            HyperOSStateEngine.setCpuLevel(6),
            HyperOSStateEngine.setGpuLevel(6),
            HyperOSStateEngine.setPhantomProcessLimit(1024),
            HyperOSStateEngine.setPowerKeeperRestricted(true),
            HyperOSStateEngine.setForce120Hz(true),
            HyperOSStateEngine.setTelemetryFrozen(true)
        ).all { it }
    }

    private fun applyStockPreset(): Boolean {
        return listOf(
            HyperOSStateEngine.setCpuLevel(0),
            HyperOSStateEngine.setGpuLevel(0),
            HyperOSStateEngine.setPowerKeeperRestricted(false),
            HyperOSStateEngine.setPhantomProcessLimit(null),
            HyperOSStateEngine.setDozeWhitelistOptimized(false),
            HyperOSStateEngine.setGmsStandbyRestricted(false),
            HyperOSStateEngine.setForce120Hz(false),
            HyperOSStateEngine.setTelemetryFrozen(false)
        ).all { it }
    }
}
