# HyperOSUnf*cker

HyperOSUnf*cker is an Android app that unlocks hidden system performance, display, memory, battery, and visual settings on Xiaomi/POCO/Redmi devices running **HyperOS or MIUI-based firmware**. All privileged operations are executed through [Shizuku](https://shizuku.rikka.app/) via ADB shell commands — no root required.

---

## Table of Contents

- [Requirements](#requirements)
- [Installation](#installation)
- [How It Works](#how-it-works)
- [Screens](#screens)
  - [HyperOS Optimization](#hyperos-optimization-screen)
  - [Debloat](#debloat-screen)
  - [Presets (Debloat)](#debloat-presets-screen)
  - [Logs](#logs-screen)
  - [Settings](#settings-screen)
- [HyperOS Optimization Options](#hyperos-optimization-options)
  - [Display](#display)
  - [Visual Capability Levels](#visual-capability-levels)
  - [Memory & Multitasking](#memory--multitasking)
  - [Battery & Standby](#battery--standby)
  - [Telemetry](#telemetry)
  - [Experimental Visual Tweaks](#experimental-visual-tweaks)
- [Quick Presets](#quick-presets)
- [Named Presets](#named-presets)
- [Safety](#safety)
- [Credits](#credits)
- [See Also](#see-also)

---

## Requirements

- Android device running **HyperOS** or **MIUI-based firmware** (Xiaomi, POCO, Redmi)
- **[Shizuku](https://shizuku.rikka.app/)** installed, running, and authorized for this app
- USB debugging enabled is recommended for recovery in case of unexpected behavior

> **What is Shizuku?** Shizuku is an app that grants elevated ADB shell permissions to other apps over a local service — without requiring root. You start it once via USB or wireless ADB and it stays running in the background.

---

## Installation

1. Download the latest APK from the [Releases](../../releases) page.
2. Install it on your device (enable *Install from unknown sources* if prompted).
3. Install and start **Shizuku** from [Google Play](https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api) or [GitHub](https://github.com/RikkaApps/Shizuku).
4. Open HyperOSUnf*cker and grant Shizuku permission when prompted.
5. Pull down on any screen to refresh the detected device state.

---

## How It Works

Every toggle or level change in the app issues one or more `adb shell` commands through the Shizuku API. The app reads back the resulting values to display the current state (visible in the **Logs** screen). Changes take effect immediately — no reboot is needed for most options, though a few persistent properties (`persist.sys.*`) may only fully apply after a restart.

---

## Screens

### HyperOS Optimization Screen

The main tuning page. Contains all toggles and level selectors organized into sections. Pull down to refresh and re-read the current device state. Tap the **⋮** menu in the top-right corner to apply a Quick Preset.

### Debloat Screen

Lists all installed (and previously removed) apps on the device. Lets you uninstall or reinstall system apps using Shizuku-backed `pm` commands. Apps are tagged with safety ratings sourced from the Universal Debloater Alliance list.

- **Filter** the list by safety category, install state, or search by name/package.
- **Tap an app** to see its description, safety rating, and package name.
- **Long-press or select multiple apps** to batch-uninstall or restore.

### Debloat Presets Screen

Save and manage named sets of apps to uninstall. Create a preset once and apply it again after a factory reset or on a new device.

### Logs Screen

A real-time action log showing every shell command the app executes, its exit code, stdout, and stderr. Filter by log level (Info / Warning / Error) to focus on failures. Useful for diagnosing which commands are failing on your specific device or firmware version.

### Settings Screen

App-level preferences:

| Setting | Description |
|---|---|
| **Auto-update bloat list** | Fetches the latest app descriptions and safety ratings from the configured URL on startup |
| **Confirm before uninstall** | Shows a confirmation dialog before removing an app |
| **Hide success dialog** | Suppresses the success popup after debloat actions |
| **Require biometric auth** | Locks the app behind fingerprint/face unlock on launch |
| **Allow unsafe selections** | Enables removal of apps marked as "unsafe" — use with caution |
| **Bloat list URL** | Custom URL to fetch the package list JSON from (default: Universal Debloater Alliance) |
| **Commits URL** | Custom URL for the package list change history |

---

## HyperOS Optimization Options

### Display

#### Force 120Hz Everywhere
Sets `system/peak_refresh_rate = 120` and `system/min_refresh_rate = 0`, then enables the user refresh rate override. This bypasses per-app refresh rate restrictions imposed by HyperOS and forces 120Hz globally across all apps — including those the system would normally cap at 60Hz.

> Turning this off resets `min_refresh_rate` to 60 and removes the override flag.

#### Stacked Recent Apps
Writes `global/task_stack_view_layout_style = 2`. Changes the recent apps view to a stacked card style supported by HyperOS System Launcher and POCO Launcher builds. Turning it off deletes the setting and restores the default style.

#### Open Hidden Performance Menu
Launches the hidden `com.android.settings.fuelgauge.PowerModeSettings` activity using `am start`. This opens the device's built-in performance mode selector (e.g., Power Saving / Balanced / Performance) if the activity exists on your firmware. It is a one-time action with no toggle state.

---

### Visual Capability Levels

HyperOS uses a Computility system (`persist.sys.computility.cpulevel` and `gpulevel`) as a feature capability flag for the system UI and Xiaomi services. The app uses the `miui.mqsas.IMQSNative` service call to set these properties, with a `setprop` fallback.

**How it works:** These properties aggressively alter the system's performance governor and scaling policies. Higher values (like `6`) force the CPU cores to scale faster and keep the GPU constantly awake for real-time Gaussian blur algorithms. This tricks the system into unlocking flagship-tier visual features: advanced textures, live blur, premium parallel animations, and seamless HyperIsland integration.

| Value | Effect |
|---|---|
| **0** | Stock — removes the override, system falls back to its default capability and scaling |
| **1–5** | Intermediate levels — unlocks varying degrees of UI enhancements depending on the firmware |
| **6** | Maximum — flagship capability tier; highest chance of enabling advanced UI textures and live blur |

> **⚠️ The Hardware Illusion (Risks):** While these flags push the UI to a premium aesthetic, they are a "hardware illusion" that relentlessly consumes resources. On mid-range or entry-level devices, aggressively keeping the GPU awake and scaling the CPU leads to **dramatic battery drain, severe overheating, and thermal throttling** — which ironically causes severe UI lag and stutter rather than smoothness. It may also cause conflicts and bugs with SystemUI overlays. Smartphone manufacturers hide these features on budget devices precisely to protect this fragile thermal balance. Weigh the trade-off between aesthetic blur and thermal limits carefully. To revert, select **0** to restore the native defaults.

---

### Memory & Multitasking

#### Restrict PowerKeeper (AppOps)
Uses `appops set` to deny `WRITE_SETTINGS`, `GET_USAGE_STATS`, and `RUN_IN_BACKGROUND` for `com.miui.powerkeeper`. PowerKeeper is HyperOS's aggressive background-app killer. Restricting it prevents the system from force-stopping background apps, which greatly improves multitasking and app persistence in RAM.

> Turning this off restores all three AppOps permissions to `allow`.

#### Phantom Process Limit
Modifies `device_config activity_manager max_phantom_processes`. Android's phantom process monitor kills child processes (sub-processes spawned by apps) when this limit is reached. The system default is typically 32.

| Value | Best for |
|---|---|
| **Default** | Removes the override, resets to system default (~32) |
| **128** | General multitasking improvement |
| **512** | Heavy multitasking, terminal emulators, development tools |
| **1024** | Emulators, Termux workloads, gaming with background services |

---

### Battery & Standby

#### Optimize Doze Whitelist
Removes `com.facebook.services` and `com.facebook.appmanager` from the Doze exemption list using `cmd deviceidle whitelist -<package>`. Apps on the Doze whitelist can wake up freely even when the screen is off, causing severe idle battery drain. Removing Facebook services from this list can significantly reduce overnight discharge.

> Turning this off re-adds the packages to the Doze whitelist.

#### Restrict GMS Standby
Sets `com.google.android.gms` and `com.google.android.gsf` to standby bucket `40` (rare) via `am set-standby-bucket` and removes them from the Doze whitelist. Google Play Services is a common source of persistent wakelocks that drain battery in the background. Restricting its standby bucket reduces how often it can run background jobs.

> Turning this off sets both packages back to the `active` bucket and re-adds them to the Doze whitelist.

#### VoLTE Carrier Check Code *(informational)*
Displays a reminder to dial `*#*#86583#*#*` in the Phone app to toggle carrier-enforced VoLTE checks. This is a UI-only hint — the app does not execute a command for this option.

---

### Telemetry

#### Freeze Telemetry & Ads
Disables `com.miui.msa.global` (MIUI ad service) and `com.miui.daemon` (analytics daemon) using `pm disable-user --user 0`. These background processes collect usage analytics, serve MIUI ads in the system UI, and consume RAM and CPU even when not actively used. Disabling them improves privacy and frees resources.

> Turning this off re-enables both packages with `pm enable`.

---

### Experimental Visual Tweaks

> ⚠️ **These options are hidden by default and collapsed behind an expandable warning card.** They affect low-level visual system properties and may cause glitches, missing effects, lag, or no visible change depending on your exact HyperOS build. They are **not included in any preset**. Test one option at a time.

Tap **"Open Visual Tweaks Guide"** inside the card for the full community Reddit guide these options are based on.

#### Control Center Blur (Glassy Blur)
Triggers `service call miui.mqsas.IMQSNative 21` to enable `persist.sys.background_blur_supported`. Enables advanced transparency/blur effects across the Control Center overlay and app folders where the HyperOS blur stack is present.

#### Advanced Visual Release
Sets `persist.sys.advanced_visual_release`. Targets different HyperOS visual rendering stacks.

| Value | Target |
|---|---|
| **Off (0)** | Removes the override |
| **HyperOS 2 (3)** | Activates the HyperOS 2 visual stack |
| **HyperOS 3 (4)** | Activates the HyperOS 3 visual stack |

#### View Smooth Corners
Sets `persist.sys.support_view_smoothcorner = true`. Enables improved rounded corner rendering on individual UI views (cards, chips, buttons) in the HyperOS compositor where supported.

#### Window Smooth Corners
Sets `persist.sys.support_window_smoothcorner = true`. Applies smoother window-level rounded corners to system UI windows and overlays where the firmware supports it.

#### MI Shadow Renderer
Sets `persist.sys.mi_shadow_supported = true`. Activates Xiaomi's richer shadow rendering pipeline for HyperOS interface elements.

#### Default Blur Status
Sets `persist.sys.background_blur_status_default = true`. Forces blur surfaces to initialize as enabled rather than requiring an explicit trigger on each boot.

#### Blur Noise
Sets `persist.sys.add_blurnoise_supported = true`. Adds the frosted-glass noise/grain texture layer used by some HyperOS blur effects.

#### Enhanced Device Level List
Writes `system/deviceLevelList = v:1,c:3,g:3`. This system setting signals to HyperOS visual subsystems that the device is a high-tier model, which can unlock folder blur, lock-screen animations, and launcher visual quality tiers gated behind device level checks.

#### Linkage State
Sets `secure/linkage_state = 1`. Used alongside the advanced texture stack in community guides to activate paired visual rendering features on supported HyperOS builds.

---

## Quick Presets

Access via the **⋮** menu on the HyperOS Optimization screen. Applies a fixed combination of settings in one tap.

| Preset | CPU | GPU | Phantom Limit | 120Hz | PowerKeeper | Doze | GMS Standby | Telemetry |
|---|---|---|---|---|---|---|---|---|
| **Max Perf** | 6 | 6 | 512 | ✅ Forced | 🚫 Restricted | — | — | ❄️ Frozen |
| **Balanced** | 3 | 3 | 128 | ❌ Off | ✅ Allowed | — | — | ❄️ Frozen |
| **Gaming** | 6 | 6 | 1024 | ✅ Forced | 🚫 Restricted | — | — | ❄️ Frozen |
| **Battery** | 0 | 0 | Default | ❌ Off | ✅ Allowed | ✅ Optimized | 🚫 Restricted | ❄️ Frozen |
| **Stock (Revert)** | 0 | 0 | Default | ❌ Off | ✅ Allowed | ❌ Restored | ✅ Restored | ✅ Re-enabled |

---

## Named Presets

In addition to the fixed Quick Presets, you can **save the current HyperOS tuning state** as a named preset with a custom name and description. Named presets capture all 18 HyperOS settings at the moment of saving and can be applied later in one tap from the HyperOS Presets page.

- **Create** — saves a snapshot of the current device state
- **Apply** — restores all captured settings to the device
- **Edit** — rename or update the description
- **Delete** — removes the preset

---

## Safety

- All HyperOS tuning changes are **reversible**. Toggle a switch off or apply the **Stock (Revert)** preset to restore defaults.
- Debloat actions use `pm disable-user` (soft-disable) or `pm uninstall --user` depending on your settings. Universally unsafe packages are filtered out unless you explicitly enable *Allow unsafe selections* in Settings.
- Keep USB debugging enabled so you can run `adb shell` recovery commands if needed.
- A few `persist.sys.*` properties only fully take effect after a reboot.
- The Experimental Visual Tweaks section can cause visual glitches. If the UI breaks, reboot the device — persistent props survive reboots but the old UI state typically resets.

---

## Credits

- **[Canta](https://github.com/samolego/Canta)** — Debloat workflow, Shizuku-based package management approach, and app architecture. Upstream credit belongs to Canta and its contributors.
- **[Universal Debloater Alliance](https://github.com/Universal-Debloater-Alliance/universal-android-preinstalled-lists)** — Package safety ratings and descriptions used in the debloat list.
- **[Shizuku](https://shizuku.rikka.app/)** — Privileged ADB shell operations without root.
- **Community guides** — HyperOS visual tweak flags sourced from Xiaomi/POCO community research (Reddit, Telegram).

---

## See Also

**[HyperOS 3 Optimization ADB Tool](https://github.com/Enki013/hyperos3-optimization-adb-tool)** — the PC/desktop companion to this app. An interactive ADB shell script (macOS / Linux) that applies the same optimizations — performance presets, PowerKeeper control, Doze whitelist, GMS standby, telemetry freeze, 120Hz toggle, and a Canta-style debloat menu — entirely from a terminal over USB. Useful when you prefer to run tweaks from a computer without installing anything on the device.
