package com.hyperosunfcker.ui.menu

import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hyperosunfcker.R
import com.hyperosunfcker.ui.navigation.Screen

@Composable
fun MoreOptionsMenu(
        showMenu: Boolean,
        showBadgeInfoDialog: () -> Unit,
        navigateToPage: (route: String) -> Unit,
        onDismiss: () -> Unit,
) {

    DropdownMenu(
            expanded = showMenu,
            onDismissRequest = onDismiss,
            modifier = Modifier.width(200.dp)
    ) {
        // Badge info dialog
        DropdownMenuItem(
                text = { Text(stringResource(R.string.badge_info)) },
                onClick = {
                    showBadgeInfoDialog()
                    onDismiss()
                }
        )

        DropdownMenuItem(
            text = { Text(stringResource(R.string.presets)) },
            onClick = {
                navigateToPage(Screen.Presets.route)
                onDismiss()
            }
        )
    }
}
