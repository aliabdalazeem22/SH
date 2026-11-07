package com.example.swiftshare.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GetApp
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.swiftshare.R
import com.example.swiftshare.domain.DeviceCapabilities
import com.example.swiftshare.domain.FileItem
import com.example.swiftshare.domain.NearbyTransferManager
import com.example.swiftshare.domain.Role
import com.example.swiftshare.domain.TransferState
import com.example.swiftshare.ui.theme.Colors.OnPrimary
import com.example.swiftshare.ui.theme.Colors.OnSurface
import com.example.swiftshare.ui.theme.Colors.OnSurfaceVariant
import com.example.swiftshare.ui.theme.Colors.Primary
import com.example.swiftshare.ui.theme.Colors.Secondary
import com.example.swiftshare.ui.theme.Colors.Success
import com.example.swiftshare.ui.theme.Colors.Warning
import com.example.swiftshare.ui.theme.DividerLine
import com.example.swiftshare.ui.theme.SurfaceCard
import com.example.swiftshare.ui.theme.Type.Caption
import com.example.swiftshare.ui.theme.Type.Headline
import com.example.swiftshare.ui.theme.Type.Label

@Composable
fun HomeScreen(
    manager: NearbyTransferManager,
    state: TransferState,
    capabilities: DeviceCapabilities,
    selectedFiles: List<FileItem>,
    onScreenChange: (Screen) -> Unit,
    onPickFiles: () -> Unit,
    onRequestPermissions: (List<String>) -> Unit,
    onDiscoveryRoleChange: (Role) -> Unit,
    contentPadding: androidx.compose.foundation.layout.PaddingValues
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
    ) {
        AppBar(
            title = stringResource(R.string.home_title),
            subtitle = stringResource(R.string.home_subtitle)
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (!capabilities.permissionsGranted) {
            NoticeCard(
                icon = Icons.Default.Warning,
                title = stringResource(R.string.permission_bluetooth_title),
                message = manager.permissionRationale(),
                tint = Warning,
                actionLabel = stringResource(R.string.grant_permission),
                onAction = { onRequestPermissions(manager.requiredPermissions()) }
            )
        }

        if (!capabilities.bluetoothEnabled) {
            NoticeCard(
                icon = Icons.Default.Warning,
                title = stringResource(R.string.bluetooth_disabled_title),
                message = stringResource(R.string.bluetooth_disabled_message),
                tint = Warning
            )
        }

        if (!capabilities.wifiEnabled) {
            NoticeCard(
                icon = Icons.Default.Warning,
                title = stringResource(R.string.wifi_disabled_title),
                message = stringResource(R.string.wifi_disabled_message),
                tint = Warning
            )
        }

        if (state is TransferState.Connected || state is TransferState.Transferring) {
            SurfaceCard(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                elevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = Success
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.transfer_in_progress),
                                style = Label,
                                color = OnSurface
                            )
                            Text(
                                text = stringResource(R.string.return_to_transfer),
                                style = Caption,
                                color = OnSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    SecondaryButton(
                        text = stringResource(R.string.open),
                        onClick = { onScreenChange(Screen.Transfer) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PrimaryButton(
                text = stringResource(R.string.send),
                onClick = {
                    onDiscoveryRoleChange(Role.SENDER)
                    if (selectedFiles.isEmpty()) {
                        onScreenChange(Screen.FileSelection)
                    } else {
                        onScreenChange(Screen.Discovery)
                    }
                },
                enabled = capabilities.readyForNearby
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = null,
                    tint = OnPrimary
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = if (selectedFiles.isEmpty()) {
                    stringResource(R.string.no_files_selected)
                } else {
                    stringResource(R.string.selected_files_count, selectedFiles.size)
                },
                style = Caption,
                color = OnSurfaceVariant
            )
            if (selectedFiles.isEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                SecondaryButton(
                    text = stringResource(R.string.select_files),
                    onClick = {
                        onDiscoveryRoleChange(Role.SENDER)
                        onScreenChange(Screen.FileSelection)
                    },
                    enabled = capabilities.permissionsGranted
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            SecondaryButton(
                text = stringResource(R.string.receive),
                onClick = {
                    onDiscoveryRoleChange(Role.RECEIVER)
                    onScreenChange(Screen.Discovery)
                },
                enabled = capabilities.readyForNearby
            ) {
                Icon(
                    imageVector = Icons.Default.GetApp,
                    contentDescription = null,
                    tint = OnPrimary
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.receive_hint),
                style = Caption,
                color = OnSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(28.dp))
        DividerLine()
        Spacer(modifier = Modifier.height(20.dp))

        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val painter = painterResource(id = R.drawable.ic_benchmark)
                androidx.compose.foundation.Image(
                    painter = painter,
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(Primary)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.benchmark),
                    style = Headline,
                    color = OnSurface
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.benchmark_desc),
                style = Caption,
                color = OnSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            SecondaryButton(
                text = stringResource(R.string.open_benchmark),
                onClick = { onScreenChange(Screen.Benchmark) }
            )

            Spacer(modifier = Modifier.height(20.dp))
            DividerLine()
            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = Secondary
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.history),
                    style = Headline,
                    color = OnSurface
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            SecondaryButton(
                text = stringResource(R.string.history),
                onClick = { onScreenChange(Screen.History) }
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = OnSurfaceVariant
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.settings),
                    style = Headline,
                    color = OnSurface
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            SecondaryButton(
                text = stringResource(R.string.settings),
                onClick = { onScreenChange(Screen.Settings) }
            )

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.privacy_note),
                style = Caption,
                color = OnSurfaceVariant
            )
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
