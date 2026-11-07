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
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.swiftshare.R
import com.example.swiftshare.domain.DeviceCapabilities
import com.example.swiftshare.domain.FileItem
import com.example.swiftshare.domain.NearbyTransferManager
import com.example.swiftshare.domain.Role
import com.example.swiftshare.ui.formatBytes
import com.example.swiftshare.ui.theme.Colors.OnPrimary
import com.example.swiftshare.ui.theme.Colors.OnSurface
import com.example.swiftshare.ui.theme.Colors.OnSurfaceVariant
import com.example.swiftshare.ui.theme.Colors.Primary
import com.example.swiftshare.ui.theme.Colors.Warning
import com.example.swiftshare.ui.theme.SurfaceCard
import com.example.swiftshare.ui.theme.Type.Body
import com.example.swiftshare.ui.theme.Type.Caption

@Composable
fun FileSelectionScreen(
    manager: NearbyTransferManager,
    capabilities: DeviceCapabilities,
    selectedFiles: List<FileItem>,
    onSelectedFilesChange: (List<FileItem>) -> Unit,
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
            title = stringResource(R.string.select_files),
            onNavigateUp = { onScreenChange(Screen.Home) },
            subtitle = if (selectedFiles.isEmpty()) {
                stringResource(R.string.no_files_selected)
            } else {
                stringResource(R.string.selected_files_count, selectedFiles.size)
            }
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

        if (selectedFiles.isEmpty()) {
            EmptyState(
                icon = {
                    Icon(
                        imageVector = Icons.Default.InsertDriveFile,
                        contentDescription = null,
                        tint = OnSurfaceVariant
                    )
                },
                title = stringResource(R.string.no_files_selected),
                message = stringResource(R.string.select_files_hint)
            )
        } else {
            SurfaceCard(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                elevation = 0.dp
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.selected_files_count, selectedFiles.size),
                        style = Body,
                        color = OnSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(
                            R.string.total_size,
                            formatBytes(selectedFiles.sumOf { it.size.coerceAtLeast(0) })
                        ),
                        style = Caption,
                        color = OnSurfaceVariant
                    )
                }
            }

            selectedFiles.forEachIndexed { index, item ->
                FileChip(
                    item = item,
                    onRemove = {
                        onSelectedFilesChange(
                            selectedFiles.toMutableList().also { it.removeAt(index) }
                        )
                    },
                    index = index
                )
            }
        }

        AddFilesRow(onClick = onPickFiles)

        Spacer(modifier = Modifier.height(20.dp))

        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
            PrimaryButton(
                text = stringResource(R.string.start_discovery),
                onClick = {
                    onDiscoveryRoleChange(Role.SENDER)
                    onScreenChange(Screen.Discovery)
                },
                enabled = selectedFiles.isNotEmpty() && capabilities.readyForNearby
            ) {
                Icon(
                    imageVector = Icons.Default.Wifi,
                    contentDescription = null,
                    tint = OnPrimary
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = when {
                    !capabilities.permissionsGranted -> stringResource(R.string.permission_denied)
                    !capabilities.bluetoothEnabled -> stringResource(R.string.bluetooth_disabled_message)
                    !capabilities.wifiEnabled -> stringResource(R.string.wifi_disabled_message)
                    selectedFiles.isEmpty() -> stringResource(R.string.select_at_least_one_file)
                    else -> stringResource(R.string.ready_to_connect)
                },
                style = Caption,
                color = OnSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
