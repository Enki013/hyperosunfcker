package com.hyperosunfcker.feature.hyperos.preset

data class HyperOSNamedPreset(
    val name: String,
    val description: String,
    val createdDate: Long,
    val snapshot: HyperOSSnapshot,
    val version: String = "1.0"
)
