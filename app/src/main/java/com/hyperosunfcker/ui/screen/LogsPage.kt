package com.hyperosunfcker.ui.screen

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.hyperosunfcker.R
import com.hyperosunfcker.ui.component.IconClickButton
import com.hyperosunfcker.util.LogUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsPage(
    onNavigateBack: () -> Unit,
    showBackButton: Boolean = true
) {
    val clipboardManager = LocalClipboardManager.current
    val logs = LogUtils.getLogs()
    var selectedLevel by remember { mutableStateOf<LogUtils.LogLevel?>(null) }
    var showFilterMenu by remember { mutableStateOf(false) }
    val filteredLogs = if (selectedLevel == null) {
        logs
    } else {
        logs.filter { it.level == selectedLevel }
    }

    Scaffold(
            topBar = {
                TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        title = {
                            Text(
                                if (selectedLevel == null) {
                                    stringResource(R.string.logs)
                                } else {
                                    "${stringResource(R.string.logs)} · ${selectedLevel?.label}"
                                }
                            )
                        },
                        navigationIcon = {
                            if (showBackButton) {
                                IconClickButton(
                                    onClick = onNavigateBack,
                                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.back),
                                )
                            }
                        },
                        actions = {
                            IconClickButton(
                                onClick = { showFilterMenu = !showFilterMenu },
                                icon = Icons.Default.FilterAlt,
                                contentDescription = "Filter logs"
                            )
                            LogFilterMenu(
                                showMenu = showFilterMenu,
                                selectedLevel = selectedLevel,
                                logs = logs,
                                onSelectedLevelChange = { selectedLevel = it },
                                onDismiss = { showFilterMenu = false }
                            )
                        }
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                        onClick = {
                            val logText =
                                    filteredLogs.joinToString("\n") { log ->
                                        "[${log.getFormattedTime()}] ${log.level} ${log.tag}: ${log.message}"
                                    }
                            clipboardManager.setText(AnnotatedString(logText))
                        }
                ) {
                    Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = stringResource(R.string.copy_logs)
                    )
                }
            }
    ) { padding ->
        Column(
                modifier =
                        Modifier.fillMaxSize()
                                .padding(padding)
                                .verticalScroll(rememberScrollState())
        ) {
            filteredLogs.forEach { logEntry -> LogEntryChip(logEntry) }
        }
    }
}

@Composable
private fun LogFilterMenu(
    showMenu: Boolean,
    selectedLevel: LogUtils.LogLevel?,
    logs: List<LogUtils.LogEntry>,
    onSelectedLevelChange: (LogUtils.LogLevel?) -> Unit,
    onDismiss: () -> Unit
) {
    DropdownMenu(
        expanded = showMenu,
        onDismissRequest = onDismiss
    ) {
        DropdownMenuItem(
            text = {
                Text(
                    if (selectedLevel == null) {
                        "All (${logs.size}) · current"
                    } else {
                        "All (${logs.size})"
                    }
                )
            },
            onClick = {
                onSelectedLevelChange(null)
                onDismiss()
            }
        )
        LogUtils.LogLevel.entries.forEach { level ->
            val count = logs.count { it.level == level }
            DropdownMenuItem(
                text = {
                    Text(
                        if (selectedLevel == level) {
                            "${level.label} ($count) · current"
                        } else {
                            "${level.label} ($count)"
                        }
                    )
                },
                onClick = {
                    onSelectedLevelChange(level)
                    onDismiss()
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LogEntryChip(logEntry: LogUtils.LogEntry) {
    var expanded by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .padding(horizontal = 8.dp)
            .combinedClickable(
                onClick = { expanded = !expanded },
                onLongClick = {
                    val logText = "[${logEntry.getFormattedTime()}] ${logEntry.level} ${logEntry.tag}: ${logEntry.message}"
                    clipboardManager.setText(AnnotatedString(logText))
                    Toast.makeText(context, R.string.log_copied, Toast.LENGTH_SHORT).show()
                }
            ),
        color = logEntry.level.color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${logEntry.level} ${logEntry.tag}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = logEntry.getFormattedTime(),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            AnimatedVisibility(visible = expanded) {
                Text(
                    text = logEntry.message,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }

}
