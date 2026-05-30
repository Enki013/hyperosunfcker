package com.hyperosunfcker.util

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class DebloatPresetData(
        val name: String,
        val description: String,
        val createdDate: Long,
        val apps: Set<String>,
        val version: String = "1.0"
) : Parcelable