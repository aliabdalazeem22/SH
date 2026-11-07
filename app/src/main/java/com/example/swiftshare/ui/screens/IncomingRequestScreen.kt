package com.example.swiftshare.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Devices
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.swiftshare.R
import com.example.swiftshare.domain.NearbyTransferManager
import com.example.swiftshare.domain.TransferState
import com.example.swiftshare.ui.theme.Colors.OnPrimary
import com.example.swiftshare.ui.theme.Colors.OnSurface
import com.example.swiftshare.ui.theme.Colors.OnSurfaceVariant
import com.example.swiftshare.ui.theme.Colors.Primary
import com.example.swiftshare.ui.theme.Type.Body
import com.example.swiftshare.ui.theme.Type.Headline

@Composable
fun IncomingRequestScreen(
    manager: NearbyTransferManager,
    state: TransferState,
    onScreenChange: (Screen) -> Unit,
    contentPadding: androidx.compose.foundation.layout.PaddingValues
) {
    val request = state as? TransferState.IncomingRequest

    LaunchedEffect(state) {
        when (state) {
            is TransferState.Connected,
            is TransferState.Transferring -> onScreenChange(Screen.Transfer)

            is TransferState.Canceled -> onScreenChange(Screen.Home)
            else -> Unit
        }
    }

    var accepting by remember(request?.endpointId) { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AppBar(
            title = stringResource(R.string.incoming_request),
            onNavigateUp = {
                request?.let { manager.rejectConnection(it.endpointId) }
                manager.cancelTransfer()
                onScreenChange(Screen.Home)
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        when {
            request != null -> {
                Icon(
                    imageVector = Icons.Default.Devices,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(72.dp)
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = stringResource(R.string.from_device, request.peerName),
                    style = Headline,
                    color = OnSurface,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.accept_connection_hint),
                    style = Body,
                    color = OnSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Spacer(modifier = Modifier.height(20.dp))
                VerificationCodeCard(code = request.code)
                Spacer(modifier = Modifier.height(24.dp))

                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                    PrimaryButton(
                        text = stringResource(R.string.accept),
                        enabled = !accepting,
                        onClick = {
                            accepting = true
                            manager.acceptConnection(request.endpointId)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = OnPrimary
                        )
                    }
                    if (accepting) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.waiting_for_sender),
                            style = Body,
                            color = OnSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        text = stringResource(R.string.decline),
                        onClick = {
                            manager.rejectConnection(request.endpointId)
                            manager.cancelTransfer()
                            onScreenChange(Screen.Home)
                        }
                    )
                }
            }

            state is TransferState.Failed -> {
                ErrorBanner(message = state.message)
                Spacer(modifier = Modifier.height(20.dp))
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                    PrimaryButton(
                        text = stringResource(R.string.home),
                        onClick = {
                            manager.cancelTransfer()
                            onScreenChange(Screen.Home)
                        }
                    )
                }
            }

            else -> {
                CircularProgressIndicator(color = Primary)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.preparing),
                    style = Body,
                    color = OnSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
