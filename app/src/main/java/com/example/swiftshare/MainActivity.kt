package com.example.swiftshare

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.swiftshare.domain.FileItem
import com.example.swiftshare.domain.Role
import com.example.swiftshare.ui.screens.BenchmarkScreen
import com.example.swiftshare.ui.screens.CompletionScreen
import com.example.swiftshare.ui.screens.DiscoveryScreen
import com.example.swiftshare.ui.screens.FileSelectionScreen
import com.example.swiftshare.ui.screens.HistoryScreen
import com.example.swiftshare.ui.screens.HomeScreen
import com.example.swiftshare.ui.screens.IncomingRequestScreen
import com.example.swiftshare.ui.screens.Screen
import com.example.swiftshare.ui.screens.SettingsScreen
import com.example.swiftshare.ui.screens.TransferScreen
import com.example.swiftshare.ui.theme.SwiftShareTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: TransferViewModel by viewModels()

    // System file picker: multiple documents of any type. No storage permission is
    // needed because access is granted per-URI by the picker (scoped-storage friendly).
    private val pickFiles =
        registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            uris.forEach { addSelectedFile(it) }
        }

    private val requestPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            viewModel.manager.refreshCapabilities()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SwiftShareTheme {
                val state by viewModel.manager.state.collectAsStateWithLifecycle()
                val capabilities by viewModel.manager.capabilities.collectAsStateWithLifecycle()
                val scope = rememberCoroutineScope()

                val sendSelected = {
                    scope.launch {
                        viewModel.manager.sendSelectedFiles(viewModel.selectedFiles.toList())
                    }
                    Unit
                }
                val goHome = {
                    viewModel.manager.cancelTransfer()
                    viewModel.screen = Screen.Home
                    Unit
                }

                BackHandler(enabled = viewModel.screen != Screen.Home) {
                    when (viewModel.screen) {
                        Screen.Discovery,
                        Screen.BenchmarkDiscovery,
                        Screen.IncomingRequest,
                        Screen.Transfer,
                        Screen.Completion -> goHome()

                        else -> viewModel.screen = Screen.Home
                    }
                }

                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                ) { padding ->
                    SwiftShareApp(
                        viewModel = viewModel,
                        state = state,
                        capabilities = capabilities,
                        selectedFiles = viewModel.selectedFiles,
                        screen = viewModel.screen,
                        onScreenChange = { viewModel.screen = it },
                        discoveryRole = viewModel.discoveryRole,
                        onDiscoveryRoleChange = { viewModel.discoveryRole = it },
                        benchmarkResult = viewModel.benchmarkResult,
                        onBenchmarkResultChange = { viewModel.benchmarkResult = it },
                        benchmarkRunning = viewModel.benchmarkRunning,
                        onBenchmarkRunningChange = { viewModel.benchmarkRunning = it },
                        onPickFiles = { pickFiles.launch(arrayOf("*/*")) },
                        onRequestPermissions = { permissions ->
                            requestPermissions.launch(permissions.toTypedArray())
                        },
                        onSelectedFilesChange = { updated ->
                            viewModel.selectedFiles.clear()
                            viewModel.selectedFiles.addAll(updated)
                        },
                        onSendSelected = sendSelected,
                        onGoHome = goHome,
                        contentPadding = padding
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.manager.refreshCapabilities()
    }

    private fun addSelectedFile(uri: Uri) {
        val name = queryName(uri) ?: uri.lastPathSegment ?: "file"
        val size = querySize(uri)
        val mime = contentResolver.getType(uri)
        if (viewModel.selectedFiles.none { it.uri == uri.toString() }) {
            viewModel.selectedFiles.add(FileItem(uri.toString(), name, size, mime))
        }
    }

    private fun queryName(uri: Uri): String? {
        return contentResolver.query(uri, null, null, null, null).use { cursor ->
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (index >= 0) cursor.getString(index) else null
            } else {
                null
            }
        }
    }

    private fun querySize(uri: Uri): Long {
        return contentResolver.query(uri, null, null, null, null).use { cursor ->
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                if (index >= 0) cursor.getLong(index).coerceAtLeast(0L) else 0L
            } else {
                0L
            }
        }
    }
}

@Composable
fun SwiftShareApp(
    viewModel: TransferViewModel,
    state: com.example.swiftshare.domain.TransferState,
    capabilities: com.example.swiftshare.domain.DeviceCapabilities,
    selectedFiles: List<FileItem>,
    screen: Screen,
    onScreenChange: (Screen) -> Unit,
    discoveryRole: Role,
    onDiscoveryRoleChange: (Role) -> Unit,
    benchmarkResult: com.example.swiftshare.domain.BenchmarkResult?,
    onBenchmarkResultChange: (com.example.swiftshare.domain.BenchmarkResult?) -> Unit,
    benchmarkRunning: Boolean,
    onBenchmarkRunningChange: (Boolean) -> Unit,
    onPickFiles: () -> Unit,
    onRequestPermissions: (List<String>) -> Unit,
    onSelectedFilesChange: (List<FileItem>) -> Unit,
    onSendSelected: () -> Unit,
    onGoHome: () -> Unit,
    contentPadding: PaddingValues
) {
    val manager = viewModel.manager

    when (screen) {
        Screen.Home -> HomeScreen(
            manager = manager,
            state = state,
            capabilities = capabilities,
            selectedFiles = selectedFiles,
            onScreenChange = onScreenChange,
            onPickFiles = onPickFiles,
            onRequestPermissions = onRequestPermissions,
            onDiscoveryRoleChange = onDiscoveryRoleChange,
            contentPadding = contentPadding
        )

        Screen.FileSelection -> FileSelectionScreen(
            manager = manager,
            capabilities = capabilities,
            selectedFiles = selectedFiles,
            onSelectedFilesChange = onSelectedFilesChange,
            onScreenChange = onScreenChange,
            onPickFiles = onPickFiles,
            onRequestPermissions = onRequestPermissions,
            onDiscoveryRoleChange = onDiscoveryRoleChange,
            contentPadding = contentPadding
        )

        Screen.Discovery -> DiscoveryScreen(
            manager = manager,
            state = state,
            capabilities = capabilities,
            role = discoveryRole,
            onScreenChange = onScreenChange,
            onRequestPermissions = onRequestPermissions,
            onConnected = {
                onScreenChange(Screen.Transfer)
                onSendSelected()
            },
            contentPadding = contentPadding
        )

        Screen.BenchmarkDiscovery -> DiscoveryScreen(
            manager = manager,
            state = state,
            capabilities = capabilities,
            role = Role.SENDER,
            benchmarkMode = true,
            onScreenChange = onScreenChange,
            onRequestPermissions = onRequestPermissions,
            onConnected = { onScreenChange(Screen.Benchmark) },
            contentPadding = contentPadding
        )

        Screen.IncomingRequest -> IncomingRequestScreen(
            manager = manager,
            state = state,
            onScreenChange = onScreenChange,
            contentPadding = contentPadding
        )

        Screen.Transfer -> TransferScreen(
            manager = manager,
            state = state,
            onScreenChange = onScreenChange,
            contentPadding = contentPadding
        )

        Screen.Completion -> CompletionScreen(
            manager = manager,
            state = state,
            onScreenChange = onScreenChange,
            onGoHome = onGoHome,
            onRetry = {
                viewModel.discoveryRole = if (selectedFiles.isNotEmpty()) Role.SENDER else Role.RECEIVER
                viewModel.manager.cancelTransfer()
                viewModel.screen = Screen.Discovery
                Unit
            },
            contentPadding = contentPadding
        )

        Screen.History -> HistoryScreen(
            manager = manager,
            onScreenChange = onScreenChange,
            contentPadding = contentPadding
        )

        Screen.Settings -> SettingsScreen(
            manager = manager,
            capabilities = capabilities,
            onScreenChange = onScreenChange,
            onRequestPermissions = onRequestPermissions,
            contentPadding = contentPadding
        )

        Screen.Benchmark -> BenchmarkScreen(
            manager = manager,
            state = state,
            onScreenChange = onScreenChange,
            benchmarkResult = benchmarkResult,
            onBenchmarkResultChange = onBenchmarkResultChange,
            benchmarkRunning = benchmarkRunning,
            onBenchmarkRunningChange = onBenchmarkRunningChange,
            contentPadding = contentPadding
        )
    }
}
