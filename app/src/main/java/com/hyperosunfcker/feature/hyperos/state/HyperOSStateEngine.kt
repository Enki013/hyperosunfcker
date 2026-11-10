package com.hyperosunfcker.feature.hyperos.state

import com.hyperosunfcker.feature.hyperos.commands.HyperOSCommandUtils
import com.hyperosunfcker.util.LogUtils

object HyperOSStateEngine {
    private const val TAG = "HyperOSStateEngine"
    private const val MQSAS_LOG_PATH = "/storage/emulated/0/log.txt"
    private const val USER_ID = 0
    private val TELEMETRY_PACKAGES = listOf("com.miui.msa.global", "com.miui.daemon")
    private val DOZE_PACKAGES = listOf("com.facebook.services", "com.facebook.appmanager")
    private val GMS_STANDBY_PACKAGES = listOf("com.google.android.gms", "com.google.android.gsf")
    private val LAUNCHER_PACKAGES = listOf("com.miui.home", "com.mi.android.globallauncher")
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

    /**
     * Writes a persist.sys.* property and confirms it with getprop.
     *
     * The IMQSNative service call exits with 0 on HyperOS 3.1 even though it no longer
     * writes the property, so the exit code alone cannot be trusted.
     */
    private fun setPersistentProperty(key: String, value: String): Boolean {
        if (propertyMatches(readPersistentProperty(key), value)) {
            return true
        }

        val serviceCommand =
            """service call miui.mqsas.IMQSNative 21 i32 1 s16 "setprop" i32 1 s16 "$key $value" s16 "$MQSAS_LOG_PATH" i32 600"""
        val serviceResult = HyperOSCommandUtils.execute(serviceCommand)
        if (serviceResult.stdout.isNotBlank()) {
            LogUtils.i(TAG, "IMQSNative reply for $key: ${serviceResult.stdout}")
        }
        if (propertyMatches(readPersistentProperty(key), value)) {
            return true
        }

        HyperOSCommandUtils.execute("setprop $key $value")
        val currentValue = readPersistentProperty(key)
        if (propertyMatches(currentValue, value)) {
            return true
        }
        if (currentValue == null) {
            // getprop itself failed (for example Shizuku went away), so keep that command's
            // error instead of blaming the HyperOS build.
            return false
        }

        HyperOSCommandUtils.recordFailure(
            "This HyperOS build did not accept $key=$value (still ${currentValue.ifBlank { "unset" }}). " +
                "Xiaomi blocked writing these properties without root in HyperOS 3.1; they work on HyperOS 3.0 and earlier."
        )
        return false
    }

    private fun readPersistentProperty(key: String): String? {
        return readShell("getprop $key")
    }

    // An unset property reads as "", which the app already treats as 0 / false.
    private fun propertyMatches(current: String?, expected: String): Boolean {
        if (current == null) return false
        if (current.equals(expected, ignoreCase = true)) return true
        return current.isBlank() && (expected == "0" || expected.equals("false", ignoreCase = true))
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
        // The setting is always stored, but newer POCO Launcher builds (HyperOS 3.1)
        // ignore it. Log the launcher versions so bug reports show which build is in use.
        LAUNCHER_PACKAGES.forEach { packageName ->
            val version = readShell("dumpsys package $packageName | grep -m1 versionName= || true")
                ?.trim()
            if (!version.isNullOrEmpty()) {
                LogUtils.i(TAG, "Launcher $packageName $version")
            }
        }
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

    /**
     * Package names from `pm list packages`. [flag] is "" for installed packages,
     * "-d" for disabled ones and "-u" to also include packages uninstalled for the user.
     */
    private fun listPackages(flag: String = ""): Set<String>? {
        val command = listOf("pm list packages", flag, "--user $USER_ID")
            .filter { it.isNotEmpty() }
            .joinToString(" ")
        return readShell(command)
            ?.lineSequence()
            ?.map { it.trim().removePrefix("package:") }
            ?.filter { it.isNotEmpty() }
            ?.toSet()
    }

    // A telemetry package counts as frozen when it is disabled or uninstalled for the user.
    fun isTelemetryFrozen(): Boolean {
        val installed = listPackages() ?: return false
        val disabled = listPackages("-d") ?: return false
        return TELEMETRY_PACKAGES.all { it !in installed || it in disabled }
    }

    fun setTelemetryFrozen(frozen: Boolean): Boolean {
        val installed = listPackages() ?: return false
        val disabled = listPackages("-d") ?: return false
        val known = if (frozen) emptySet() else listPackages("-u") ?: return false

        // Handle every package even if an earlier one fails.
        return TELEMETRY_PACKAGES.map { packageName ->
            if (frozen) {
                freezePackage(packageName, installed, disabled)
            } else {
                unfreezePackage(packageName, installed, disabled, known)
            }
        }.all { it }
    }

    private fun freezePackage(
        packageName: String,
        installed: Set<String>,
        disabled: Set<String>
    ): Boolean {
        if (packageName !in installed || packageName in disabled) {
            return true
        }

        val disableResult = HyperOSCommandUtils.execute("pm disable-user --user $USER_ID $packageName")
        if (disableResult.success) {
            return true
        }

        // HyperOS refuses to disable some system packages ("Cannot disable system packages")
        // but still lets them be uninstalled for the current user, which install-existing undoes.
        if (!disableResult.stderr.contains("Cannot disable")) {
            return false
        }
        LogUtils.i(TAG, "HyperOS blocked disabling $packageName; uninstalling it for user $USER_ID instead")
        return runShell("pm uninstall -k --user $USER_ID $packageName") ||
            runShell("pm uninstall --user $USER_ID $packageName")
    }

    private fun unfreezePackage(
        packageName: String,
        installed: Set<String>,
        disabled: Set<String>,
        known: Set<String>
    ): Boolean {
        if (packageName !in known) {
            // Not on this device at all, so there is nothing to restore.
            return true
        }
        if (packageName !in installed &&
            !runShell("pm install-existing --user $USER_ID $packageName")
        ) {
            return false
        }
        if (packageName in disabled) {
            return runShell("pm enable --user $USER_ID $packageName")
        }
        return true
    }
}
