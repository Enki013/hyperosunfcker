package com.hyperosunfcker.feature.hyperos.state

import com.hyperosunfcker.feature.hyperos.commands.HyperOSCommandUtils

object HyperOSStateEngine {
    private const val MQSAS_LOG_PATH = "/storage/emulated/0/log.txt"
    private const val USER_ID = 0
    private val DOZE_PACKAGES = listOf("com.facebook.services", "com.facebook.appmanager")
    private val GMS_STANDBY_PACKAGES = listOf("com.google.android.gms", "com.google.android.gsf")
    private val POWER_KEEPER_OPS =
        listOf("WRITE_SETTINGS", "GET_USAGE_STATS", "RUN_IN_BACKGROUND")

    private fun readShell(command: String): String? {
        return HyperOSCommandUtils.execute(command).takeIf { it.success }?.stdout
    }

    private fun runShell(command: String): Boolean {
        return HyperOSCommandUtils.execute(command).success
    }

    private fun runShellWithFallback(primaryCommand: String, fallbackCommand: String): Boolean {
        val primaryResult = HyperOSCommandUtils.execute(primaryCommand)
        if (primaryResult.success) {
            return true
        }

        return runShell(fallbackCommand)
    }

    private fun setPersistentProperty(key: String, value: String): Boolean {
        val serviceCommand =
            """service call miui.mqsas.IMQSNative 21 i32 1 s16 "setprop" i32 1 s16 "$key $value" s16 "$MQSAS_LOG_PATH" i32 600"""
        val serviceResult = HyperOSCommandUtils.execute(serviceCommand)
        if (serviceResult.success) {
            return true
        }

        return runShell("setprop $key $value")
    }

    private fun putSetting(namespace: String, key: String, value: String): Boolean {
        val command = "settings put $namespace $key $value"
        if (!runShell(command)) {
            return false
        }

        val currentValue = readSetting(namespace, key)
        if (currentValue == value) {
            return true
        }

        HyperOSCommandUtils.recordFailure(
            "Command did not persist setting. Expected $namespace/$key=$value, got ${currentValue ?: "null"}"
        )
        return false
    }

    private fun deleteSetting(namespace: String, key: String): Boolean {
        val command = "settings delete $namespace $key"
        if (!runShell(command)) {
            return false
        }

        val currentValue = readSetting(namespace, key)
        if (currentValue == null) {
            return true
        }

        HyperOSCommandUtils.recordFailure(
            "Command did not delete setting. $namespace/$key is still $currentValue"
        )
        return false
    }

    private fun readSetting(namespace: String, key: String): String? {
        return readShell("settings get $namespace $key")?.takeUnless { it == "null" }
    }

    private fun readPersistentInt(key: String, defaultValue: Int = 0): Int {
        return readShell("getprop $key")?.toIntOrNull() ?: defaultValue
    }

    private fun readPersistentBoolean(key: String): Boolean {
        return readShell("getprop $key").equals("true", ignoreCase = true)
    }

    private fun readSystemSettingFloat(key: String): Float? {
        return readShell("settings get system $key")?.toFloatOrNull()
    }

    // --- Visual Performance ---

    fun getCpuLevel(): Int {
        return readPersistentInt("persist.sys.computility.cpulevel")
    }

    fun setCpuLevel(level: Int): Boolean {
        return setPersistentProperty("persist.sys.computility.cpulevel", level.toString())
    }

    fun getGpuLevel(): Int {
        return readPersistentInt("persist.sys.computility.gpulevel")
    }

    fun setGpuLevel(level: Int): Boolean {
        return setPersistentProperty("persist.sys.computility.gpulevel", level.toString())
    }

    fun isBlurEnabled(): Boolean {
        return readPersistentBoolean("persist.sys.background_blur_supported")
    }

    fun setBlurEnabled(enabled: Boolean): Boolean {
        return setPersistentProperty("persist.sys.background_blur_supported", enabled.toString())
    }

    fun getAdvancedVisualRelease(): Int {
        return readPersistentInt("persist.sys.advanced_visual_release")
    }

    fun setAdvancedVisualRelease(level: Int): Boolean {
        return setPersistentProperty("persist.sys.advanced_visual_release", level.toString())
    }

    fun isViewSmoothCornerEnabled(): Boolean {
        return readPersistentBoolean("persist.sys.support_view_smoothcorner")
    }

    fun setViewSmoothCornerEnabled(enabled: Boolean): Boolean {
        return setPersistentProperty("persist.sys.support_view_smoothcorner", enabled.toString())
    }

    fun isWindowSmoothCornerEnabled(): Boolean {
        return readPersistentBoolean("persist.sys.support_window_smoothcorner")
    }

    fun setWindowSmoothCornerEnabled(enabled: Boolean): Boolean {
        return setPersistentProperty("persist.sys.support_window_smoothcorner", enabled.toString())
    }

    fun isMiShadowEnabled(): Boolean {
        return readPersistentBoolean("persist.sys.mi_shadow_supported")
    }

    fun setMiShadowEnabled(enabled: Boolean): Boolean {
        return setPersistentProperty("persist.sys.mi_shadow_supported", enabled.toString())
    }

    fun isDefaultBlurStatusEnabled(): Boolean {
        return readPersistentBoolean("persist.sys.background_blur_status_default")
    }

    fun setDefaultBlurStatusEnabled(enabled: Boolean): Boolean {
        return setPersistentProperty("persist.sys.background_blur_status_default", enabled.toString())
    }

    fun isBlurNoiseEnabled(): Boolean {
        return readPersistentBoolean("persist.sys.add_blurnoise_supported")
    }

    fun setBlurNoiseEnabled(enabled: Boolean): Boolean {
        return setPersistentProperty("persist.sys.add_blurnoise_supported", enabled.toString())
    }

    fun isEnhancedDeviceLevelListEnabled(): Boolean {
        return readSetting("system", "deviceLevelList") == "v:1,c:3,g:3"
    }

    fun setEnhancedDeviceLevelListEnabled(enabled: Boolean): Boolean {
        return if (enabled) {
            putSetting("system", "deviceLevelList", "v:1,c:3,g:3")
        } else {
            deleteSetting("system", "deviceLevelList")
        }
    }

    fun isLinkageStateEnabled(): Boolean {
        return readSetting("secure", "linkage_state") == "1"
    }

    fun setLinkageStateEnabled(enabled: Boolean): Boolean {
        return if (enabled) {
            putSetting("secure", "linkage_state", "1")
        } else {
            deleteSetting("secure", "linkage_state")
        }
    }

    fun isStackedRecentsEnabled(): Boolean {
        return readSetting("global", "task_stack_view_layout_style") == "2"
    }

    fun setStackedRecentsEnabled(enabled: Boolean): Boolean {
        return if (enabled) {
            putSetting("global", "task_stack_view_layout_style", "2")
        } else {
            deleteSetting("global", "task_stack_view_layout_style")
        }
    }

    // --- Memory & RAM ---

    fun isPowerKeeperRestricted(): Boolean {
        return POWER_KEEPER_OPS.all { op ->
            readShell("appops get com.miui.powerkeeper $op")
                ?.contains("deny", ignoreCase = true) == true
        }
    }

    fun setPowerKeeperRestricted(restricted: Boolean): Boolean {
        val mode = if (restricted) "deny" else "allow"
        return POWER_KEEPER_OPS.all { op ->
            runShell("appops set com.miui.powerkeeper $op $mode")
        }
    }

    fun getPhantomProcessLimit(): Int {
        return readShell("device_config get activity_manager max_phantom_processes")
            ?.toIntOrNull() ?: 32 // Default is usually 32
    }

    fun setPhantomProcessLimit(limit: Int?): Boolean {
        return if (limit != null) {
            runShell("device_config put activity_manager max_phantom_processes $limit")
        } else {
            runShell("device_config delete activity_manager max_phantom_processes")
        }
    }

    // --- Battery Optimization ---

    fun isDozeWhitelistOptimized(): Boolean {
        val output = readShell("cmd deviceidle whitelist")
            ?: readShell("dumpsys deviceidle whitelist")
            ?: return false
        return DOZE_PACKAGES.none { packageName -> output.contains(packageName) }
    }

    fun setDozeWhitelistOptimized(optimized: Boolean): Boolean {
        val prefix = if (optimized) "-" else "+"
        return DOZE_PACKAGES.all { packageName ->
            runShellWithFallback(
                primaryCommand = "cmd deviceidle whitelist $prefix$packageName",
                fallbackCommand = "dumpsys deviceidle whitelist $prefix$packageName"
            )
        }
    }

    fun isGmsStandbyRestricted(): Boolean {
        val output = readShell("am get-standby-bucket --user $USER_ID com.google.android.gms")
            ?: readShell("am get-standby-bucket com.google.android.gms")
        return output?.contains("40") == true ||
            output?.contains("rare", ignoreCase = true) == true
    }

    fun setGmsStandbyRestricted(restricted: Boolean): Boolean {
        val mode = if (restricted) "40" else "active"
        val whitelistPrefix = if (restricted) "-" else "+"
        return GMS_STANDBY_PACKAGES.all { packageName ->
            val whitelistUpdated = runShellWithFallback(
                primaryCommand = "cmd deviceidle whitelist $whitelistPrefix$packageName",
                fallbackCommand = "dumpsys deviceidle whitelist $whitelistPrefix$packageName"
            )
            val bucketUpdated = runShellWithFallback(
                primaryCommand = "am set-standby-bucket --user $USER_ID $packageName $mode",
                fallbackCommand = "am set-standby-bucket $packageName $mode"
            )
            whitelistUpdated && bucketUpdated
        }
    }

    // --- Display Module ---

    fun is120HzForced(): Boolean {
        val peak = readSystemSettingFloat("peak_refresh_rate")
        val min = readSystemSettingFloat("min_refresh_rate")
        return peak == 120f && min == 0f
    }

    fun setForce120Hz(forced: Boolean): Boolean {
        if (forced) {
            return listOf(
                "settings put system min_refresh_rate 0",
                "settings put system peak_refresh_rate 120",
                "settings put system user_refresh_rate 1"
            ).all(::runShell)
        }

        return listOf(
            "settings put system min_refresh_rate 60",
            "settings put system peak_refresh_rate 120",
            "settings delete system user_refresh_rate"
        ).all(::runShell)
    }

    fun openHiddenPerformanceMenu(): Boolean {
        return runShell(
            "am start -n com.android.settings/com.android.settings.fuelgauge.PowerModeSettings"
        )
    }

    // --- Debloat Module ---

    fun isTelemetryFrozen(): Boolean {
        // pm list packages -d shows disabled packages
        val output = readShell("pm list packages -d --user 0")
        return output?.contains("com.miui.msa.global") == true && output.contains("com.miui.daemon")
    }

    fun setTelemetryFrozen(frozen: Boolean): Boolean {
        if (frozen) {
            return listOf("com.miui.msa.global", "com.miui.daemon").all { packageName ->
                runShell("pm disable-user --user 0 $packageName")
            }
        }

        return listOf("com.miui.msa.global", "com.miui.daemon").all { packageName ->
            runShell("pm enable $packageName")
        }
    }
}
