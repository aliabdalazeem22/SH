package com.example.swiftshare.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.swiftshare.R
import com.example.swiftshare.domain.DeviceCapabilities
import com.example.swiftshare.domain.NearbyTransferManager
import com.example.swiftshare.ui.theme.Colors.Error
import com.example.swiftshare.ui.theme.Colors.OnSurface
import com.example.swiftshare.ui.theme.Colors.OnSurfaceVariant
import com.example.swiftshare.ui.theme.Colors.Success
import com.example.swiftshare.ui.theme.SurfaceCard
import com.example.swiftshare.ui.theme.Type.Body
import com.example.swiftshare.ui.theme.Type.Caption
import com.example.swiftshare.ui.theme.Type.Headline
import com.example.swiftshare.ui.theme.Type.Label

@Composable
fun SettingsScreen(
    manager: NearbyTransferManager,
    capabilities: DeviceCapabilities,
    onScreenChange: (Screen) -> Unit,
    onRequestPermissions: (List<String>) -> Unit,
    contentPadding: androidx.compose.foundation.layout.PaddingValues
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
    ) {
        AppBar(
            title = stringResource(R.string.settings),
            onNavigateUp = { onScreenChange(Screen.Home) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        SettingCard(title = stringResource(R.string.device)) {
            Text(text = manager.deviceName(), style = Body, color = OnSurface)
        }

        SettingCard(title = stringResource(R.string.permissions)) {
            Text(
                text = if (capabilities.permissionsGranted) {
                    stringResource(R.string.permissions_granted)
                } else {
                    stringResource(R.string.permissions_missing)
                },
                style = Body,
                color = if (capabilities.permissionsGranted) Success else Error
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = manager.permissionRationale(),
                style = Caption,
                color = OnSurfaceVariant
            )
            if (!capabilities.permissionsGranted) {
                Spacer(modifier = Modifier.height(12.dp))
                PrimaryButton(
                    text = stringResource(R.string.grant_permission),
                    onClick = { onRequestPermissions(manager.requiredPermissions()) }
                )
            }
        }

        SettingCard(title = stringResource(R.string.radios)) {
            Text(
                text = stringResource(
                    R.string.radio_status,
                    stringResource(
                        if (capabilities.bluetoothEnabled) R.string.on else R.string.off
                    ),
                    stringResource(
                        if (capabilities.wifiEnabled) R.string.on else R.string.off
                    )
                ),
                style = Body,
                color = OnSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.radio_note),
                style = Caption,
                color = OnSurfaceVariant
            )
        }

        SettingCard(title = stringResource(R.string.storage)) {
            Text(
                text = manager.receiveDirectoryPath(),
                style = Body,
                color = OnSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.storage_note),
                style = Caption,
                color = OnSurfaceVariant
            )
        }

        SettingCard(title = stringResource(R.string.language)) {
            Text(text = stringResource(R.string.language_arabic), style = Body, color = OnSurface)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = stringResource(R.string.language_english), style = Body, color = OnSurface)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.language_note),
                style = Caption,
                color = OnSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
            SecondaryButton(
                text = stringResource(R.string.benchmark),
                onClick = { onScreenChange(Screen.Benchmark) }
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                text = stringResource(R.string.clear_history),
                onClick = { manager.clearHistory() }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        SettingCard(title = stringResource(R.string.about)) {
            Text(
                text = stringResource(R.string.privacy_note),
                style = Body,
                color = OnSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.speed_disclaimer),
                style = Caption,
                color = OnSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.apk_sharing_note),
                style = Caption,
                color = OnSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun SettingCard(title: String, content: @Composable () -> Unit) {
    SurfaceCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        elevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = Label, color = OnSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}
