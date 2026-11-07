package com.example.swiftshare.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DevicesOther
import androidx.compose.runtime.Composable
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
import com.example.swiftshare.domain.BenchmarkResult
import com.example.swiftshare.domain.NearbyTransferManager
import com.example.swiftshare.domain.Role
import com.example.swiftshare.domain.TransferState
import com.example.swiftshare.ui.formatSpeed
import com.example.swiftshare.ui.theme.Colors.OnPrimary
import com.example.swiftshare.ui.theme.Colors.OnSurface
import com.example.swiftshare.ui.theme.Colors.OnSurfaceVariant
import com.example.swiftshare.ui.theme.Colors.Primary
import com.example.swiftshare.ui.theme.Colors.Secondary
import com.example.swiftshare.ui.theme.SurfaceCard
import com.example.swiftshare.ui.theme.Type.Body
import com.example.swiftshare.ui.theme.Type.BodyLarge
import com.example.swiftshare.ui.theme.Type.Caption
import com.example.swiftshare.ui.theme.Type.Display
import com.example.swiftshare.ui.theme.Type.Headline
import com.example.swiftshare.ui.theme.Type.Label
import java.util.Locale

@Composable
fun BenchmarkScreen(
    manager: NearbyTransferManager,
    state: TransferState,
    onScreenChange: (Screen) -> Unit,
    benchmarkResult: BenchmarkResult?,
    onBenchmarkResultChange: (BenchmarkResult?) -> Unit,
    benchmarkRunning: Boolean,
    onBenchmarkRunningChange: (Boolean) -> Unit,
    contentPadding: androidx.compose.foundation.layout.PaddingValues
) {
    val sizes = listOf(10, 25, 50, 100)
    var selectedMb by remember { mutableStateOf(25) }
    var hasRun by remember { mutableStateOf(false) }
    var lastFailed by remember { mutableStateOf(false) }

    val canBenchmark = manager.isConnected && manager.currentRole == Role.SENDER

    fun leaveScreen() {
        if (benchmarkRunning) {
            manager.cancelTransfer()
            onBenchmarkRunningChange(false)
        }
        onScreenChange(Screen.Home)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
    ) {
        AppBar(
            title = stringResource(R.string.benchmark),
            onNavigateUp = { leaveScreen() },
            subtitle = stringResource(R.string.benchmark_desc)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            Text(
                text = stringResource(R.string.test_file_size),
                style = Label,
                color = OnSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                sizes.forEach { mb ->
                    val selected = selectedMb == mb
                    SurfaceCard(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(enabled = !benchmarkRunning) { selectedMb = mb },
                        elevation = if (selected) 4.dp else 0.dp
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "$mb",
                                style = BodyLarge,
                                color = if (selected) Primary else OnSurface,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "MB",
                                style = Caption,
                                color = OnSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (!canBenchmark) {
            NoticeCard(
                icon = Icons.Default.DevicesOther,
                title = stringResource(R.string.benchmark_needs_connection),
                message = stringResource(R.string.benchmark_needs_connection_hint),
                tint = Secondary,
                actionLabel = stringResource(R.string.connect_to_receiver),
                onAction = {
                    manager.cancelTransfer()
                    onScreenChange(Screen.BenchmarkDiscovery)
                }
            )
        } else {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                PrimaryButton(
                    text = stringResource(R.string.run_benchmark),
                    enabled = !benchmarkRunning && selectedMb > 0,
                    onClick = {
                        val endpoint = manager.connectedEndpoint
                        if (endpoint == null) {
                            lastFailed = true
                            hasRun = true
                            return@PrimaryButton
                        }
                        lastFailed = false
                        hasRun = true
                        onBenchmarkResultChange(null)
                        onBenchmarkRunningChange(true)
                        manager.startBenchmark(selectedMb.toDouble(), endpoint) { result ->
                            onBenchmarkResultChange(result)
                            onBenchmarkRunningChange(false)
                            lastFailed = result == null
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = OnPrimary
                    )
                }

                if (benchmarkRunning) {
                    Spacer(modifier = Modifier.height(20.dp))
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = Primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.benchmark_running_hint),
                        style = Caption,
                        color = OnSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            when {
                benchmarkRunning -> Unit

                lastFailed -> {
                    ErrorBanner(message = stringResource(R.string.benchmark_failed))
                    Spacer(modifier = Modifier.height(12.dp))
                }

                benchmarkResult != null -> {
                    BenchmarkResultCard(result = benchmarkResult)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                hasRun -> Unit
            }

            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                OutlinedButton(
                    text = stringResource(R.string.change_receiver),
                    enabled = !benchmarkRunning,
                    onClick = {
                        manager.cancelTransfer()
                        onScreenChange(Screen.BenchmarkDiscovery)
                    }
                )
            }
        }

        if (state is TransferState.Failed && !benchmarkRunning) {
            Spacer(modifier = Modifier.height(12.dp))
            ErrorBanner(message = state.message)
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun BenchmarkResultCard(result: BenchmarkResult) {
    SurfaceCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        elevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.measured_result),
                    style = Headline,
                    color = OnSurface
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.measured_badge),
                    style = Caption,
                    color = Secondary
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = formatSpeed(result.throughputMbps),
                    style = Display,
                    color = Primary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.mb_per_sec),
                    style = Body,
                    color = OnSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ResultStat(
                    label = stringResource(R.string.test_file_size),
                    value = String.format(
                        Locale.US,
                        "%.1f MB",
                        result.testFileSizeBytes / (1024.0 * 1024.0)
                    )
                )
                ResultStat(
                    label = stringResource(R.string.time_label),
                    value = String.format(Locale.US, "%.2f s", result.elapsedSeconds)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = stringResource(R.string.benchmark_disclaimer),
                style = Caption,
                color = OnSurfaceVariant
            )
        }
    }
}

@Composable
private fun ResultStat(label: String, value: String) {
    Column {
        Text(text = label, style = Caption, color = OnSurfaceVariant)
        Text(text = value, style = BodyLarge, color = OnSurface)
    }
}
