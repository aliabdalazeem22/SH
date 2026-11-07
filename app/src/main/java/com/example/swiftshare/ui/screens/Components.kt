package com.example.swiftshare.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.GetApp
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Send
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.swiftshare.R
import com.example.swiftshare.domain.DiscoveredPeer
import com.example.swiftshare.domain.Direction
import com.example.swiftshare.domain.FileItem
import com.example.swiftshare.domain.Role
import com.example.swiftshare.domain.Status
import com.example.swiftshare.domain.TransferRecord
import com.example.swiftshare.ui.formatBytes
import com.example.swiftshare.ui.formatEta
import com.example.swiftshare.ui.formatSpeed
import com.example.swiftshare.ui.formatTimestamp
import com.example.swiftshare.ui.progressPercent
import com.example.swiftshare.ui.theme.Colors.Divider
import com.example.swiftshare.ui.theme.Colors.Error as ErrorColor
import com.example.swiftshare.ui.theme.Colors.OnPrimary
import com.example.swiftshare.ui.theme.Colors.OnSurface
import com.example.swiftshare.ui.theme.Colors.OnSurfaceVariant
import com.example.swiftshare.ui.theme.Colors.Primary
import com.example.swiftshare.ui.theme.Colors.PrimaryContainer
import com.example.swiftshare.ui.theme.Colors.Secondary
import com.example.swiftshare.ui.theme.Colors.Success
import com.example.swiftshare.ui.theme.Colors.SurfaceElevated
import com.example.swiftshare.ui.theme.Colors.Warning
import com.example.swiftshare.ui.theme.DividerLine
import com.example.swiftshare.ui.theme.SurfaceCard
import com.example.swiftshare.ui.theme.Type.Body
import com.example.swiftshare.ui.theme.Type.BodyLarge
import com.example.swiftshare.ui.theme.Type.Caption
import com.example.swiftshare.ui.theme.Type.Headline
import com.example.swiftshare.ui.theme.Type.Label
import java.util.Locale

@Composable
fun AppBar(
    title: String,
    onNavigateUp: (() -> Unit)? = null,
    subtitle: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, start = 16.dp, end = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onNavigateUp != null) {
            IconButton(onClick = onNavigateUp) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = OnSurface
                )
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = if (onNavigateUp != null) 12.dp else 0.dp)
        ) {
            Text(text = title, style = Headline, color = OnSurface)
            subtitle?.let {
                Text(text = it, style = Caption, color = OnSurfaceVariant)
            }
        }
    }
    DividerLine()
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = if (enabled) Primary else SurfaceElevated,
            contentColor = if (enabled) OnPrimary else OnSurfaceVariant,
            disabledBackgroundColor = SurfaceElevated,
            disabledContentColor = OnSurfaceVariant
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon?.invoke()
            if (icon != null) Spacer(modifier = Modifier.width(8.dp))
            Text(text = text, style = Label, color = if (enabled) OnPrimary else OnSurfaceVariant)
        }
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = if (enabled) PrimaryContainer else SurfaceElevated,
            contentColor = OnPrimary,
            disabledBackgroundColor = SurfaceElevated,
            disabledContentColor = OnSurfaceVariant
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon?.invoke()
            if (icon != null) Spacer(modifier = Modifier.width(8.dp))
            Text(text = text, style = Label, color = if (enabled) OnPrimary else OnSurfaceVariant)
        }
    }
}

@Composable
fun OutlinedButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = Color.Transparent,
            contentColor = Primary,
            disabledBackgroundColor = Color.Transparent,
            disabledContentColor = OnSurfaceVariant
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Text(text = text, style = Label, color = if (enabled) Primary else OnSurfaceVariant)
    }
}

@Composable
fun IconTile(icon: @Composable () -> Unit, tint: Color = Primary) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceElevated),
        contentAlignment = Alignment.Center
    ) {
        icon()
    }
}

@Composable
fun PeerItem(
    peer: DiscoveredPeer,
    selected: Boolean,
    onClick: () -> Unit
) {
    SurfaceCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (selected) 2.dp else 0.dp,
                color = if (selected) Primary else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() },
        elevation = if (selected) 4.dp else 0.dp
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Default.Devices, contentDescription = null, tint = Primary)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = peer.name, style = BodyLarge, color = OnSurface, maxLines = 1)
                Text(
                    text = stringResource(R.string.tap_to_connect),
                    style = Caption,
                    color = OnSurfaceVariant
                )
            }
            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Primary)
        }
    }
}

@Composable
fun TransferProgress(
    role: Role,
    fileName: String,
    index: Int,
    totalFiles: Int,
    transferredBytes: Long,
    totalBytes: Long,
    speedMbps: Double,
    etaSeconds: Long?,
    onCancel: () -> Unit
) {
    val progress = progressPercent(transferredBytes, totalBytes) / 100f
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(
                    if (role == Role.SENDER) R.string.sending else R.string.receiving
                ),
                style = BodyLarge,
                color = OnSurface
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.file_counter, index, totalFiles),
                style = Caption,
                color = OnSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = fileName,
            style = Body,
            color = OnSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(12.dp))
        LinearProgressIndicator(
            progress = progress.coerceIn(0f, 1f),
            modifier = Modifier.fillMaxWidth(),
            color = Primary,
            backgroundColor = Divider
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StatBlock(
                label = stringResource(R.string.transferred),
                value = formatBytes(transferredBytes),
                valueColor = OnSurface
            )
            StatBlock(
                label = stringResource(R.string.of),
                value = formatBytes(totalBytes),
                valueColor = OnSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StatBlock(
                label = stringResource(R.string.speed),
                value = "${formatSpeed(speedMbps)} ${stringResource(R.string.mb_per_sec)}",
                valueColor = Primary
            )
            StatBlock(
                label = stringResource(R.string.eta),
                value = formatEta(etaSeconds),
                valueColor = OnSurface
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        PrimaryButton(text = stringResource(R.string.cancel), onClick = onCancel)
    }
}

@Composable
fun StatBlock(label: String, value: String, valueColor: Color) {
    Column {
        Text(text = label, style = Caption, color = OnSurfaceVariant)
        Text(text = value, style = BodyLarge, color = valueColor)
    }
}

@Composable
fun EmptyState(icon: @Composable () -> Unit, title: String, message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        icon()
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = title, style = Headline, color = OnSurface, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = Body,
            color = OnSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ErrorBanner(message: String) {
    SurfaceCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        elevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(imageVector = Icons.Default.Error, contentDescription = null, tint = ErrorColor)
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = message, style = Body, color = OnSurface)
        }
    }
}

@Composable
fun NoticeCard(
    icon: ImageVector,
    title: String,
    message: String,
    tint: Color = Warning,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    SurfaceCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        elevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(imageVector = icon, contentDescription = null, tint = tint)
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = title, style = Headline, color = OnSurface)
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = message, style = Body, color = OnSurfaceVariant)
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(14.dp))
                PrimaryButton(text = actionLabel, onClick = onAction)
            }
        }
    }
}

@Composable
fun HistoryItem(record: TransferRecord) {
    SurfaceCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        elevation = 0.dp
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (record.direction == Direction.SEND) {
                    Icons.Default.Send
                } else {
                    Icons.Default.GetApp
                },
                contentDescription = null,
                tint = if (record.direction == Direction.SEND) Primary else Secondary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.fileName,
                    style = BodyLarge,
                    color = OnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = listOfNotNull(
                        record.peerName?.takeIf { it.isNotBlank() },
                        formatBytes(record.fileSize),
                        formatTimestamp(record.timestamp).takeIf { it.isNotEmpty() },
                        String.format(Locale.US, "%.2f MB/s", record.averageSpeedMbps)
                    ).joinToString(" • "),
                    style = Caption,
                    color = OnSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(
                    when (record.status) {
                        Status.COMPLETED -> R.string.status_completed
                        Status.FAILED -> R.string.status_failed
                        Status.CANCELED -> R.string.status_canceled
                        Status.IN_PROGRESS -> R.string.status_in_progress
                    }
                ),
                style = Caption,
                color = when (record.status) {
                    Status.COMPLETED -> Success
                    Status.FAILED -> ErrorColor
                    Status.CANCELED -> Warning
                    Status.IN_PROGRESS -> OnSurfaceVariant
                }
            )
        }
    }
}

@Composable
fun FileChip(
    item: FileItem,
    onRemove: () -> Unit,
    index: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceElevated, RoundedCornerShape(10.dp))
            .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.InsertDriveFile,
            contentDescription = null,
            tint = OnSurfaceVariant
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = Body,
                color = OnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = listOfNotNull(
                    formatBytes(item.size).takeIf { item.size > 0 },
                    item.mimeType?.takeIf { it.isNotBlank() }
                ).joinToString(" • "),
                style = Caption,
                color = OnSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(text = "${index + 1}", style = Caption, color = Primary)
        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.remove_file),
                tint = OnSurfaceVariant
            )
        }
    }
}

@Composable
fun AddFilesRow(onClick: () -> Unit) {
    SurfaceCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onClick() },
        elevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = stringResource(R.string.add_more_files), style = Label, color = Primary)
        }
    }
}

@Composable
fun VerificationCodeCard(code: String?) {
    if (code.isNullOrBlank()) return
    SurfaceCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        elevation = 0.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.verification_code),
                style = Caption,
                color = OnSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = code, style = Headline, color = Secondary)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.verification_code_hint),
                style = Caption,
                color = OnSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
