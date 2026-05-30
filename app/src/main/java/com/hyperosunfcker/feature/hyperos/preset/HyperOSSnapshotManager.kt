package com.hyperosunfcker.feature.hyperos.preset

import android.content.Context
import com.hyperosunfcker.util.LogUtils
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object HyperOSSnapshotManager {
    private const val TAG = "HyperOSSnapshotManager"
    private const val PRESETS_FILE_NAME = "hyperos_presets.json"
    private const val LEGACY_SNAPSHOT_FILE_NAME = "hyperos_snapshot.json"

    private fun getPresetsFile(context: Context): File {
        return File(context.filesDir, PRESETS_FILE_NAME)
    }

    private fun getLegacySnapshotFile(context: Context): File {
        return File(context.filesDir, LEGACY_SNAPSHOT_FILE_NAME)
    }

    fun loadPresets(context: Context): List<HyperOSNamedPreset> {
        val file = getPresetsFile(context)
        if (!file.exists()) {
            return loadLegacyPreset(context)
        }

        return try {
            val jsonArray = JSONArray(file.readText())
            buildList {
                for (index in 0 until jsonArray.length()) {
                    val jsonObject = jsonArray.getJSONObject(index)
                    add(jsonToPreset(jsonObject))
                }
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "Failed to load HyperOS presets: ${e.message}", e)
            emptyList()
        }
    }

    fun savePreset(context: Context, name: String, description: String): Boolean {
        return try {
            val preset = HyperOSNamedPreset(
                name = name,
                description = description,
                createdDate = System.currentTimeMillis(),
                snapshot = HyperOSSnapshot.captureCurrentState()
            )
            writePresets(context, loadPresets(context) + preset)
            LogUtils.i(TAG, "Saved HyperOS preset: $name")
            true
        } catch (e: Exception) {
            LogUtils.e(TAG, "Failed to save HyperOS preset: ${e.message}", e)
            false
        }
    }

    fun updatePreset(
        context: Context,
        oldPreset: HyperOSNamedPreset,
        newName: String,
        newDescription: String
    ): Boolean {
        return try {
            val updatedPresets = loadPresets(context).map { preset ->
                if (preset.name == oldPreset.name && preset.createdDate == oldPreset.createdDate) {
                    preset.copy(name = newName, description = newDescription)
                } else {
                    preset
                }
            }
            writePresets(context, updatedPresets)
            true
        } catch (e: Exception) {
            LogUtils.e(TAG, "Failed to update HyperOS preset: ${e.message}", e)
            false
        }
    }

    fun deletePreset(context: Context, presetToDelete: HyperOSNamedPreset): Boolean {
        return try {
            val updatedPresets = loadPresets(context).filterNot { preset ->
                preset.name == presetToDelete.name && preset.createdDate == presetToDelete.createdDate
            }
            writePresets(context, updatedPresets)
            true
        } catch (e: Exception) {
            LogUtils.e(TAG, "Failed to delete HyperOS preset: ${e.message}", e)
            false
        }
    }

    fun formatDate(timestamp: Long): String {
        val formatter = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        return formatter.format(Date(timestamp))
    }

    private fun writePresets(context: Context, presets: List<HyperOSNamedPreset>) {
        val jsonArray = JSONArray()
        presets.forEach { preset -> jsonArray.put(presetToJson(preset)) }
        getPresetsFile(context).writeText(jsonArray.toString(2))
    }

    private fun loadLegacyPreset(context: Context): List<HyperOSNamedPreset> {
        val legacyFile = getLegacySnapshotFile(context)
        if (!legacyFile.exists()) {
            return emptyList()
        }

        return try {
            listOf(
                HyperOSNamedPreset(
                    name = "Legacy Custom",
                    description = "Imported from the old single custom preset.",
                    createdDate = legacyFile.lastModified(),
                    snapshot = jsonToSnapshot(JSONObject(legacyFile.readText()))
                )
            )
        } catch (e: Exception) {
            LogUtils.e(TAG, "Failed to load legacy HyperOS snapshot: ${e.message}", e)
            emptyList()
        }
    }

    private fun presetToJson(preset: HyperOSNamedPreset): JSONObject {
        return JSONObject().apply {
            put("name", preset.name)
            put("description", preset.description)
            put("createdDate", preset.createdDate)
            put("version", preset.version)
            put("snapshot", snapshotToJson(preset.snapshot))
        }
    }

    private fun jsonToPreset(jsonObject: JSONObject): HyperOSNamedPreset {
        return HyperOSNamedPreset(
            name = jsonObject.getString("name"),
            description = jsonObject.optString("description"),
            createdDate = jsonObject.getLong("createdDate"),
            version = jsonObject.optString("version", "1.0"),
            snapshot = jsonToSnapshot(jsonObject.getJSONObject("snapshot"))
        )
    }

    private fun snapshotToJson(snapshot: HyperOSSnapshot): JSONObject {
        return JSONObject().apply {
            put("cpuLevel", snapshot.cpuLevel)
            put("gpuLevel", snapshot.gpuLevel)
            put("isBlurEnabled", snapshot.isBlurEnabled)
            put("advancedVisualRelease", snapshot.advancedVisualRelease)
            put("isViewSmoothCornerEnabled", snapshot.isViewSmoothCornerEnabled)
            put("isWindowSmoothCornerEnabled", snapshot.isWindowSmoothCornerEnabled)
            put("isMiShadowEnabled", snapshot.isMiShadowEnabled)
            put("isDefaultBlurStatusEnabled", snapshot.isDefaultBlurStatusEnabled)
            put("isBlurNoiseEnabled", snapshot.isBlurNoiseEnabled)
            put("isEnhancedDeviceLevelListEnabled", snapshot.isEnhancedDeviceLevelListEnabled)
            put("isLinkageStateEnabled", snapshot.isLinkageStateEnabled)
            put("isStackedRecentsEnabled", snapshot.isStackedRecentsEnabled)
            put("isPowerKeeperRestricted", snapshot.isPowerKeeperRestricted)
            put("phantomProcessLimit", snapshot.phantomProcessLimit)
            put("isDozeWhitelistOptimized", snapshot.isDozeWhitelistOptimized)
            put("isGmsStandbyRestricted", snapshot.isGmsStandbyRestricted)
            put("is120HzForced", snapshot.is120HzForced)
            put("isTelemetryFrozen", snapshot.isTelemetryFrozen)
        }
    }

    private fun jsonToSnapshot(jsonObject: JSONObject): HyperOSSnapshot {
        return HyperOSSnapshot(
            cpuLevel = jsonObject.optInt("cpuLevel", 0),
            gpuLevel = jsonObject.optInt("gpuLevel", 0),
            isBlurEnabled = jsonObject.optBoolean("isBlurEnabled", false),
            advancedVisualRelease = jsonObject.optInt("advancedVisualRelease", 0),
            isViewSmoothCornerEnabled = jsonObject.optBoolean(
                "isViewSmoothCornerEnabled",
                jsonObject.optBoolean("isSmoothCornerEnabled", false)
            ),
            isWindowSmoothCornerEnabled = jsonObject.optBoolean("isWindowSmoothCornerEnabled", false),
            isMiShadowEnabled = jsonObject.optBoolean("isMiShadowEnabled", false),
            isDefaultBlurStatusEnabled = jsonObject.optBoolean("isDefaultBlurStatusEnabled", false),
            isBlurNoiseEnabled = jsonObject.optBoolean("isBlurNoiseEnabled", false),
            isEnhancedDeviceLevelListEnabled = jsonObject.optBoolean("isEnhancedDeviceLevelListEnabled", false),
            isLinkageStateEnabled = jsonObject.optBoolean("isLinkageStateEnabled", false),
            isStackedRecentsEnabled = jsonObject.optBoolean("isStackedRecentsEnabled", false),
            isPowerKeeperRestricted = jsonObject.optBoolean("isPowerKeeperRestricted", false),
            phantomProcessLimit = jsonObject.optInt("phantomProcessLimit", 32),
            isDozeWhitelistOptimized = jsonObject.optBoolean("isDozeWhitelistOptimized", false),
            isGmsStandbyRestricted = jsonObject.optBoolean("isGmsStandbyRestricted", false),
            is120HzForced = jsonObject.optBoolean("is120HzForced", false),
            isTelemetryFrozen = jsonObject.optBoolean("isTelemetryFrozen", false)
        )
    }
}
