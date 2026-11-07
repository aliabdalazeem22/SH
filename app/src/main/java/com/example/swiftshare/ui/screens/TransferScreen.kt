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
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.swiftshare.R
import com.example.swiftshare.domain.NearbyTransferManager
import com.example.swiftshare.domain.Role
import com.example.swiftshare.domain.TransferState
import com.example.swiftshare.ui.theme.Colors.OnSurfaceVariant
import com.example.swiftshare.ui.theme.Colors.Primary
import com.example.swiftshare.ui.theme.Type.Body

@Composable
fun TransferScreen(
    manager: NearbyTransferManager,
    state: TransferState,
    onScreenChange: (Screen) -> Unit,
    contentPadding: androidx.compose.foundation.layout.PaddingValues
) {
    LaunchedEffect(state) {
        when (state) {
            is TransferState.Completed,
            is TransferState.Failed,
            is TransferState.Canceled -> onScreenChange(Screen.Completion)

            else -> Unit
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
    ) {
        AppBar(
            title = stringResource(
                if (state is TransferState.Transferring && state.role == Role.RECEIVER) {
                    R.string.receiving
                } else {
                    R.string.transferring
                }
            ),
            onNavigateUp = {
                manager.cancelTransfer()
                onScreenChange(Screen.Home)
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        when (state) {
            is TransferState.Transferring -> TransferProgress(
                role = state.role,
                fileName = state.fileName,
                index = state.index,
                totalFiles = state.totalFiles,
                transferredBytes = state.transferredBytes,
                totalBytes = state.totalBytes,
                speedMbps = state.speedMbps,
                etaSeconds = state.etaSeconds,
                onCancel = { manager.cancelTransfer() }
            )

            is TransferState.Connected -> Preparing(
                message = stringResource(
                    if (state.role == Role.RECEIVER) {
                        R.string.preparing_receive
                    } else {
                        R.string.preparing_send
                    }
                )
            )

            is TransferState.Connecting -> Preparing(
                message = stringResource(R.string.connecting_to, state.peerName)
            )

            else -> Preparing(message = stringResource(R.string.preparing))
        }
    }
}

@Composable
private fun Preparing(message: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(52.dp),
            color = Primary
        )
        Spacer(modifier = Modifier.height(20.dp))
        Icon(
            imageVector = Icons.Default.HourglassEmpty,
            contentDescription = null,
            tint = OnSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = message,
            style = Body,
            color = OnSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
