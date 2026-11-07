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
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Error
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.swiftshare.R
import com.example.swiftshare.domain.NearbyTransferManager
import com.example.swiftshare.domain.Role
import com.example.swiftshare.domain.TransferState
import com.example.swiftshare.ui.theme.Colors.Error as ErrorColor
import com.example.swiftshare.ui.theme.Colors.OnSurface
import com.example.swiftshare.ui.theme.Colors.OnSurfaceVariant
import com.example.swiftshare.ui.theme.Colors.Success
import com.example.swiftshare.ui.theme.Colors.Warning
import com.example.swiftshare.ui.theme.Type.Body
import com.example.swiftshare.ui.theme.Type.BodyLarge
import com.example.swiftshare.ui.theme.Type.Caption
import com.example.swiftshare.ui.theme.Type.Headline

@Composable
fun CompletionScreen(
    manager: NearbyTransferManager,
    state: TransferState,
    onScreenChange: (Screen) -> Unit,
    onGoHome: () -> Unit,
    onRetry: () -> Unit,
    contentPadding: androidx.compose.foundation.layout.PaddingValues
) {
    val completed = state as? TransferState.Completed
    val failed = state as? TransferState.Failed
    val canceled = state as? TransferState.Canceled
    val role = completed?.role ?: manager.currentRole

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AppBar(
            title = stringResource(
                when {
                    completed != null -> R.string.transfer_complete
                    failed != null -> R.string.transfer_failed
                    else -> R.string.transfer_canceled
                }
            ),
            onNavigateUp = { onGoHome() }
        )

        Spacer(modifier = Modifier.height(32.dp))

        when {
            completed != null -> {
                Icon(
                    imageVector = Icons.Default.Done,
                    contentDescription = null,
                    tint = Success,
                    modifier = Modifier.size(80.dp)
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = stringResource(R.string.transfer_complete),
                    style = Headline,
                    color = OnSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = completed.fileName,
                    style = BodyLarge,
                    color = OnSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                if (role == Role.RECEIVER) {
                    Text(
                        text = stringResource(R.string.saved_to, manager.receiveDirectoryPath()),
                        style = Caption,
                        color = OnSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.received_files_note),
                        style = Caption,
                        color = OnSurfaceVariant
                    )
                } else {
                    Text(
                        text = stringResource(R.string.sent_successfully),
                        style = Caption,
                        color = OnSurfaceVariant
                    )
                }
            }

            failed != null -> {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = null,
                    tint = ErrorColor,
                    modifier = Modifier.size(80.dp)
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = stringResource(R.string.transfer_failed),
                    style = Headline,
                    color = OnSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = failed.message,
                    style = Body,
                    color = OnSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }

            canceled != null -> {
                Icon(
                    imageVector = Icons.Default.Cancel,
                    contentDescription = null,
                    tint = Warning,
                    modifier = Modifier.size(80.dp)
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = stringResource(R.string.transfer_canceled),
                    style = Headline,
                    color = OnSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = canceled.message ?: stringResource(R.string.canceled_hint),
                    style = Body,
                    color = OnSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }

            else -> {
                Text(
                    text = stringResource(R.string.preparing),
                    style = Headline,
                    color = OnSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
            when {
                failed != null -> {
                    PrimaryButton(
                        text = stringResource(R.string.retry),
                        onClick = onRetry
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                completed != null && role == Role.RECEIVER -> {
                    PrimaryButton(
                        text = stringResource(R.string.history),
                        onClick = { onScreenChange(Screen.History) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                else -> {
                    PrimaryButton(
                        text = stringResource(R.string.history),
                        onClick = { onScreenChange(Screen.History) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            OutlinedButton(
                text = stringResource(R.string.home),
                onClick = { onGoHome() }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
