package com.example.swiftshare.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DevicesOther
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.swiftshare.R
import com.example.swiftshare.domain.DeviceCapabilities
import com.example.swiftshare.domain.NearbyTransferManager
import com.example.swiftshare.domain.Role
import com.example.swiftshare.domain.TransferState
import com.example.swiftshare.ui.theme.Colors.OnSurface
import com.example.swiftshare.ui.theme.Colors.OnSurfaceVariant
import com.example.swiftshare.ui.theme.Colors.Primary
import com.example.swiftshare.ui.theme.Colors.Warning
import com.example.swiftshare.ui.theme.SurfaceCard
import com.example.swiftshare.ui.theme.Type.Body
import com.example.swiftshare.ui.theme.Type.Caption
import com.example.swiftshare.ui.theme.Type.Headline
import com.example.swiftshare.ui.theme.Type.Label
import kotlinx.coroutines.launch

@Composable
fun DiscoveryScreen(
    manager: NearbyTransferManager,
    state: TransferState,
    capabilities: DeviceCapabilities,
    role: Role,
    onScreenChange: (Screen) -> Unit,
    onRequestPermissions: (List<String>) -> Unit,
    onConnected: () -> Unit,
    contentPadding: androidx.compose.foundation.layout.PaddingValues,
    benchmarkMode: Boolean = false
) {
    val scope = rememberCoroutineScope()
    val localName = remember { manager.deviceName() }
    var handled by remember(role, benchmarkMode) { mutableStateOf(false) }

    LaunchedEffect(role, benchmarkMode) {
        if (role == Role.SENDER) {
            manager.beginDiscovery(localName)
        } else {
            manager.beginAdvertising(localName)
        }
    }

    LaunchedEffect(state, role, benchmarkMode) {
        when {
            state is TransferState.IncomingRequest && !benchmarkMode -> {
                if (!handled) {
                    handled = true
                    onScreenChange(Screen.IncomingRequest)
                }
            }
            state is TransferState.Connected && role == Role.SENDER -> {
                if (!handled) {
                    handled = true
                    onConnected()
                }
            }
        }
    }

    fun cancelAndGoHome() {
        manager.cancelTransfer()
        onScreenChange(Screen.Home)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
    ) {
        AppBar(
            title = if (role == Role.SENDER) {
                stringResource(R.string.start_discovery)
            } else {
                stringResource(R.string.make_discoverable)
            },
            onNavigateUp = { cancelAndGoHome() },
            subtitle = when {
                benchmarkMode -> stringResource(R.string.benchmark_discovery_subtitle)
                role == Role.SENDER -> stringResource(R.string.scanning)
                else -> stringResource(R.string.advertising)
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

        if (!capabilities.bluetoothEnabled) {
            NoticeCard(
                icon = Icons.Default.Warning,
                title = stringResource(R.string.bluetooth_disabled_title),
                message = stringResource(R.string.bluetooth_disabled_message),
                tint = Warning
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (role == Role.SENDER) {
            when (state) {
                is TransferState.FoundPeers -> {
                    Text(
                        text = stringResource(R.string.nearby_devices),
                        style = Label,
                        color = OnSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )
                    state.peers.forEach { peer ->
                        PeerItem(
                            peer = peer,
                            selected = false,
                            onClick = {
                                scope.launch {
                                    manager.requestConnection(peer.endpointId, localName)
                                }
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.tap_peer_hint),
                        style = Caption,
                        color = OnSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }

                is TransferState.NoPeers -> EmptyState(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.DevicesOther,
                            contentDescription = null,
                            tint = OnSurfaceVariant
                        )
                    },
                    title = stringResource(R.string.no_devices_found),
                    message = stringResource(R.string.no_peers_hint)
                )

                is TransferState.Discovering -> Searching(
                    message = stringResource(R.string.scanning),
                    hint = stringResource(R.string.sender_hint)
                )

                is TransferState.Connecting -> {
                    ConnectingCard(
                        peerName = state.peerName,
                        code = state.code
                    )
                }

                is TransferState.Failed -> {
                    ErrorBanner(message = state.message)
                    Spacer(modifier = Modifier.height(16.dp))
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                        SecondaryButton(
                            text = stringResource(R.string.retry),
                            onClick = {
                                manager.cancelTransfer()
                                manager.beginDiscovery(localName)
                            }
                        )
                    }
                }

                else -> Searching(
                    message = stringResource(R.string.scanning),
                    hint = stringResource(R.string.sender_hint)
                )
            }
        } else {
            when (state) {
                is TransferState.Discovering -> {
                    EmptyState(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Wifi,
                                contentDescription = null,
                                tint = Primary
                            )
                        },
                        title = stringResource(R.string.advertising),
                        message = stringResource(R.string.waiting_for_sender_hint)
                    )
                    SurfaceCard(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                        elevation = 0.dp
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(R.string.device_name),
                                style = Caption,
                                color = OnSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = localName, style = Headline, color = OnSurface)
                        }
                    }
                }

                is TransferState.Failed -> {
                    ErrorBanner(message = state.message)
                    Spacer(modifier = Modifier.height(16.dp))
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                        SecondaryButton(
                            text = stringResource(R.string.retry),
                            onClick = {
                                manager.cancelTransfer()
                                manager.beginAdvertising(localName)
                            }
                        )
                    }
                }

                else -> Searching(
                    message = stringResource(R.string.advertising),
                    hint = stringResource(R.string.waiting_for_sender_hint)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                OutlinedButton(
                    text = stringResource(R.string.stop),
                    onClick = { cancelAndGoHome() }
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun Searching(message: String, hint: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = Primary
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(text = message, style = Headline, color = OnSurface, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = hint,
            style = Body,
            color = OnSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}

@Composable
private fun ConnectingCard(peerName: String, code: String?) {
    SurfaceCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
        elevation = 2.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(modifier = Modifier.size(40.dp), color = Primary)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.connecting_to, peerName),
                style = Headline,
                color = OnSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.waiting_for_accept),
                style = Body,
                color = OnSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            VerificationCodeCard(code = code)
        }
    }
}
