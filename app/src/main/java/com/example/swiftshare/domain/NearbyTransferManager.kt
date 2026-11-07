package com.example.swiftshare.domain

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.example.swiftshare.R
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.Random
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Service id used only by SwiftShare peers. */
const val SERVICE_ID = "com.example.swiftshare.v1"

private val STRATEGY: Strategy = Strategy.P2P_STAR
private const val HEADER_TIMEOUT_MS = 30_000L
private const val END_MESSAGE = "END"
private const val BUFFER_SIZE = 128 * 1024
private const val BENCHMARK_INDEX = 100_000

private suspend fun <T> Task<T>.awaitTask(): T =
    suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { value ->
            if (continuation.isActive) continuation.resume(value)
        }
        addOnFailureListener { error ->
            if (continuation.isActive) continuation.resumeWithException(error)
        }
    }

/**
 * Central transfer/discovery logic built on Google Nearby Connections (P2P_STAR).
 *
 * The connection uses Wi-Fi for bulk data; Bluetooth is only used by Nearby
 * Connections for discovery/setup. Nothing ever goes through the internet or a
 * cloud server, and the app does not request the INTERNET permission.
 *
 * Wire protocol for a batch of files (one file at a time, stop-and-wait):
 *
 *   sender -> receiver : BYTES {"i":index,"t":total,"n":name,"s":size}
 *   receiver -> sender : BYTES "OK:index"          (receiver is ready)
 *   sender -> receiver : FILE payload (streamed from its ParcelFileDescriptor)
 *   receiver           : copy to temp file, verify byte count, move to final folder
 *   receiver -> sender : BYTES "DONE:index"        (file verified on disk)
 *   sender -> receiver : BYTES "END"               (after the last file)
 *
 * Stop-and-wait keeps at most one file in flight, which removes ambiguous ordering
 * between BYTES and FILE payloads and guarantees the sender knows the receiver
 * actually wrote each file before reporting success.
 */
class NearbyTransferManager(private val context: Context) {

    private val client = Nearby.getConnectionsClient(context)
    private val contentResolver = context.contentResolver
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow<TransferState>(TransferState.Idle)
    val state: StateFlow<TransferState> = _state.asStateFlow()

    private val _capabilities = MutableStateFlow(readCapabilities())
    val capabilities: StateFlow<DeviceCapabilities> = _capabilities.asStateFlow()

    private val historyStore = TransferHistoryStore(context)
    private val discovered = LinkedHashMap<String, DiscoveredPeer>()

    private var activeRole: Role = Role.SENDER
    @Volatile private var connectedEndpointId: String? = null
    private var connectedPeerName: String = ""

    /** Read-only endpoint id for the benchmark screen. */
    val connectedEndpoint: String? get() = connectedEndpointId
    val currentRole: Role? get() = if (connectedEndpointId != null) activeRole else null
    val isConnected: Boolean get() = connectedEndpointId != null

    private var advertising = false
    private var discovering = false
    private var advertisingToken = 0
    private var discoveryToken = 0

    // ---- per-file progress bookkeeping -------------------------------------
    @Volatile private var transferStart = 0L
    @Volatile private var currentFileBytes = 0L
    @Volatile private var currentFileSize = 0L
    private var currentIndex = 0
    private var currentTotalFiles = 0
    private var currentName = ""

    // ---- protocol bookkeeping ---------------------------------------------
    private val pendingAcks = HashMap<Int, CompletableDeferred<String>>()
    private val outgoingFilePayloadIds = HashSet<Long>()
    private val incomingPayloads = HashMap<Long, Payload>()
    private var currentReceiving: ReceivingFile? = null

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            when (payload.type) {
                Payload.Type.BYTES -> {
                    val bytes = payload.asBytes() ?: return
                    scope.launch { handleBytes(endpointId, bytes) }
                }
                Payload.Type.FILE -> {
                    incomingPayloads[payload.id] = payload
                    currentReceiving?.let { receiving ->
                        if (receiving.payloadId == null) receiving.payloadId = payload.id
                    }
                }
                else -> Unit
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            scope.launch { handleTransferUpdate(endpointId, update) }
        }
    }

    private val advertisingLifecycle = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            connectedEndpointId = endpointId
            activeRole = Role.RECEIVER
            connectedPeerName = info.endpointName?.takeIf { it.isNotBlank() } ?: "Nearby device"
            _state.value = TransferState.IncomingRequest(
                endpointId = endpointId,
                peerName = connectedPeerName,
                code = info.authenticationDigits
            )
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            if (result.status.isSuccess) {
                connectedEndpointId = endpointId
                activeRole = Role.RECEIVER
                stopAdvertisingInternal()
                _state.value = TransferState.Connected(connectedPeerName, Role.RECEIVER)
            } else {
                connectedEndpointId = null
                _state.value = TransferState.Failed(resolveError(result.status.statusCode))
            }
        }

        override fun onDisconnected(endpointId: String) {
            if (connectedEndpointId == endpointId) {
                connectedEndpointId = null
                currentReceiving = null
                if (!isTerminalState()) {
                    _state.value = TransferState.Failed(context.getString(R.string.connection_lost))
                }
            }
        }
    }

    private val connectingLifecycle = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            connectedEndpointId = endpointId
            activeRole = Role.SENDER
            connectedPeerName = info.endpointName?.takeIf { it.isNotBlank() } ?: connectedPeerName
            // The initiating side already confirmed the peer by tapping it in the
            // device list, so it accepts automatically. The receiver still has to
            // confirm on its own screen before the connection is established.
            client.acceptConnection(endpointId, payloadCallback)
            _state.value = TransferState.Connecting(
                peerName = connectedPeerName,
                role = Role.SENDER,
                code = info.authenticationDigits
            )
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            if (result.status.isSuccess) {
                connectedEndpointId = endpointId
                activeRole = Role.SENDER
                stopDiscoveryInternal()
                _state.value = TransferState.Connected(connectedPeerName, Role.SENDER)
            } else {
                connectedEndpointId = null
                _state.value = TransferState.Failed(resolveError(result.status.statusCode))
            }
        }

        override fun onDisconnected(endpointId: String) {
            if (connectedEndpointId == endpointId) {
                connectedEndpointId = null
                if (!isTerminalState()) {
                    _state.value = TransferState.Failed(context.getString(R.string.connection_lost))
                }
            }
        }
    }

    /* ------------------------------------------------------------------ */
    /* capability / radio checks                                          */
    /* ------------------------------------------------------------------ */

    /**
     * Runtime permissions required by Nearby Connections. The exact set depends
     * on the Android version; the manifest declares the matching maximum SDK
     * versions so older devices never see irrelevant requests.
     */
    fun requiredPermissions(): List<String> {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions += Manifest.permission.BLUETOOTH_SCAN
            permissions += Manifest.permission.BLUETOOTH_CONNECT
            permissions += Manifest.permission.BLUETOOTH_ADVERTISE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissions += Manifest.permission.NEARBY_WIFI_DEVICES
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10-11 requires fine location for Nearby discovery.
            permissions += Manifest.permission.ACCESS_FINE_LOCATION
        } else {
            // Android 7-9: coarse location is enough and is the permission the
            // manifest declares for this range.
            permissions += Manifest.permission.ACCESS_COARSE_LOCATION
        }
        return permissions
    }

    fun permissionsGranted(): Boolean = requiredPermissions().all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    fun permissionRationale(): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getString(R.string.permission_bluetooth_message)
        } else {
            context.getString(R.string.permission_location_message)
        }

    fun isBluetoothEnabled(): Boolean = try {
        BluetoothAdapter.getDefaultAdapter()?.isEnabled == true
    } catch (_: SecurityException) {
        false
    }

    fun isWifiEnabled(): Boolean = try {
        (context.getSystemService(Context.WIFI_SERVICE) as? WifiManager)?.isWifiEnabled == true
    } catch (_: Exception) {
        false
    }

    private fun readCapabilities(): DeviceCapabilities = DeviceCapabilities(
        permissionsGranted = permissionsGranted(),
        bluetoothEnabled = isBluetoothEnabled(),
        wifiEnabled = isWifiEnabled()
    )

    /** Re-read permissions/radio state after returning to the foreground. */
    fun refreshCapabilities() {
        _capabilities.value = readCapabilities()
    }

    fun deviceName(): String {
        val globalName = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                Settings.Global.getString(contentResolver, Settings.Global.DEVICE_NAME)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
        val bluetoothName = try {
            BluetoothAdapter.getDefaultAdapter()?.name
        } catch (_: SecurityException) {
            null
        }
        return listOf(globalName, bluetoothName, Build.MODEL)
            .firstOrNull { !it.isNullOrBlank() } ?: "Android device"
    }

    /** Folder where received files are written. Displayed in the UI/settings. */
    fun receiveDirectoryPath(): String {
        val root = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
        return File(root, "SwiftShare").absolutePath
    }

    /* ------------------------------------------------------------------ */
    /* discovery / advertising                                            */
    /* ------------------------------------------------------------------ */

    /**
     * Starts advertising on the manager's own scope so leaving the screen cannot
     * cancel the API call halfway through. The token prevents a stale start from
     * resurrecting advertising after the user left the screen.
     */
    fun beginAdvertising(localName: String) {
        val token = ++advertisingToken
        discoveryToken++
        scope.launch { startAdvertising(localName, token) }
    }

    private suspend fun startAdvertising(localName: String, token: Int) {
        if (advertising) return
        stopDiscoveryInternal()
        activeRole = Role.RECEIVER
        connectedEndpointId = null
        discovered.clear()
        refreshCapabilities()
        _state.value = TransferState.Discovering(Role.RECEIVER)
        try {
            val options = AdvertisingOptions.Builder().setStrategy(STRATEGY).build()
            client.startAdvertising(localName, SERVICE_ID, advertisingLifecycle, options).awaitTask()
            if (token != advertisingToken) {
                runCatching { client.stopAdvertising() }
                return
            }
            advertising = true
        } catch (e: Exception) {
            if (token == advertisingToken) {
                _state.value = TransferState.Failed(context.getString(R.string.nearby_unavailable))
            }
        }
    }

    /**
     * Stops advertising without touching the UI state. Called when leaving the
     * receive screen; navigation decides what the next state should be.
     */
    fun stopAdvertising() {
        advertisingToken++
        stopAdvertisingInternal()
    }

    private fun stopAdvertisingInternal() {
        if (!advertising) return
        advertising = false
        try {
            client.stopAdvertising()
        } catch (_: Exception) {
        }
    }

    /** Same lifecycle contract as [beginAdvertising], for the sending side. */
    fun beginDiscovery(localName: String) {
        val token = ++discoveryToken
        advertisingToken++
        scope.launch { startDiscovery(localName, token) }
    }

    private suspend fun startDiscovery(localName: String, token: Int) {
        if (discovering) return
        stopAdvertisingInternal()
        activeRole = Role.SENDER
        connectedEndpointId = null
        discovered.clear()
        refreshCapabilities()
        _state.value = TransferState.Discovering(Role.SENDER)
        try {
            val options = DiscoveryOptions.Builder().setStrategy(STRATEGY).build()
            client.startDiscovery(
                SERVICE_ID,
                object : EndpointDiscoveryCallback() {
                    override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
                        discovered[endpointId] = DiscoveredPeer(
                            endpointId,
                            info.endpointName?.takeIf { it.isNotBlank() } ?: "Nearby device"
                        )
                        _state.value = TransferState.FoundPeers(discovered.values.toList(), Role.SENDER)
                    }

                    override fun onEndpointLost(endpointId: String) {
                        discovered.remove(endpointId)
                        _state.value = if (discovered.isEmpty()) {
                            TransferState.NoPeers(Role.SENDER)
                        } else {
                            TransferState.FoundPeers(discovered.values.toList(), Role.SENDER)
                        }
                    }
                },
                options
            ).awaitTask()
            if (token != discoveryToken) {
                runCatching { client.stopDiscovery() }
                return
            }
            discovering = true
        } catch (e: Exception) {
            if (token == discoveryToken) {
                _state.value = TransferState.Failed(context.getString(R.string.nearby_unavailable))
            }
        }
    }

    fun stopDiscovery() {
        discoveryToken++
        stopDiscoveryInternal()
        discovered.clear()
    }

    private fun stopDiscoveryInternal() {
        if (!discovering) return
        discovering = false
        try {
            client.stopDiscovery()
        } catch (_: Exception) {
        }
    }

    suspend fun requestConnection(endpointId: String, localName: String) {
        activeRole = Role.SENDER
        connectedPeerName = discovered[endpointId]?.name ?: "Nearby device"
        _state.value = TransferState.Connecting(connectedPeerName, Role.SENDER)
        try {
            client.requestConnection(localName, endpointId, connectingLifecycle).awaitTask()
        } catch (e: Exception) {
            connectedEndpointId = null
            _state.value = TransferState.Failed(context.getString(R.string.connection_lost))
        }
    }

    fun acceptConnection(endpointId: String) {
        connectedEndpointId = endpointId
        activeRole = Role.RECEIVER
        try {
            client.acceptConnection(endpointId, payloadCallback).addOnFailureListener {
                if (!isTerminalState()) {
                    _state.value = TransferState.Failed(context.getString(R.string.connection_lost))
                }
            }
        } catch (e: Exception) {
            _state.value = TransferState.Failed(context.getString(R.string.connection_lost))
        }
    }

    fun rejectConnection(endpointId: String) {
        try {
            client.rejectConnection(endpointId)
        } catch (_: Exception) {
        }
        connectedEndpointId = null
        _state.value = TransferState.Idle
    }

    /* ------------------------------------------------------------------ */
    /* sending                                                            */
    /* ------------------------------------------------------------------ */

    /**
     * Sends the selected files to the currently connected peer, one at a time.
     * Each file is only reported as sent after the receiver confirms it was
     * written and verified on disk.
     */
    suspend fun sendSelectedFiles(items: List<FileItem>) {
        val endpoint = connectedEndpointId
        if (endpoint == null) {
            _state.value = TransferState.Failed(context.getString(R.string.connection_lost))
            return
        }
        if (items.isEmpty()) {
            _state.value = TransferState.Failed(context.getString(R.string.no_files_selected))
            return
        }

        activeRole = Role.SENDER
        currentTotalFiles = items.size
        try {
            items.forEachIndexed { index, item ->
                if (_state.value is TransferState.Canceled) return
                currentIndex = index
                val transferred = transmitOneFile(
                    endpoint = endpoint,
                    item = item,
                    index = index,
                    totalFiles = items.size,
                    updateUi = true
                )
                if (!transferred) {
                    if (_state.value !is TransferState.Canceled) {
                        _state.value = TransferState.Failed(context.getString(R.string.transfer_failed))
                    }
                    return
                }
            }

            if (_state.value is TransferState.Canceled) return

            // Tell the receiver the whole batch is finished.
            runCatching {
                client.sendPayload(endpoint, Payload.fromBytes(END_MESSAGE.toByteArray(Charsets.UTF_8))).awaitTask()
            }

            _state.value = TransferState.Completed(
                currentName.ifBlank { items.last().name },
                Role.SENDER
            )
        } catch (e: Exception) {
            if (!isTerminalState()) {
                _state.value = TransferState.Failed(
                    e.message ?: context.getString(R.string.transfer_failed)
                )
            }
        }
        refreshCapabilities()
    }

    /**
     * Streams one file through the header -> OK -> file -> DONE handshake.
     * Returns true only when the receiver confirmed the file on disk.
     *
     * The ParcelFileDescriptor is handed to Nearby's [Payload.fromFile], which owns
     * it while streaming and releases it when the payload is closed below. It is
     * only closed manually when payload creation itself fails.
     */
    @SuppressLint("Recycle")
    private suspend fun transmitOneFile(
        endpoint: String,
        item: FileItem,
        index: Int,
        totalFiles: Int,
        updateUi: Boolean
    ): Boolean {
        val safeName = FileNaming.sanitize(item.name)
        var size = item.size
        var stagedTemp: File? = null
        var openedDescriptor: ParcelFileDescriptor? = null

        val payload: Payload = try {
            val pfd = withContext(Dispatchers.IO) {
                contentResolver.openFileDescriptor(Uri.parse(item.uri), "r")
            }
            if (pfd != null) {
                openedDescriptor = pfd
                if (size <= 0) {
                    val stat = withContext(Dispatchers.IO) {
                        runCatching { pfd.statSize }.getOrDefault(-1L)
                    }
                    if (stat > 0) size = stat
                }
                Payload.fromFile(pfd).also { it.setFileName(safeName) }
            } else {
                // Fallback: copy to a cache file first, still streamed from disk.
                val temp = stageToCache(item.uri, safeName) ?: return false
                stagedTemp = temp
                if (size <= 0) size = temp.length()
                Payload.fromFile(temp).also { it.setFileName(safeName) }
            }
        } catch (e: Exception) {
            runCatching { openedDescriptor?.close() }
            runCatching { stagedTemp?.delete() }
            return false
        }

        currentIndex = index
        currentTotalFiles = totalFiles
        currentName = safeName
        currentFileSize = size.coerceAtLeast(0)
        currentFileBytes = 0L
        transferStart = System.currentTimeMillis()
        outgoingFilePayloadIds.add(payload.id)
        if (updateUi) emitProgress(Role.SENDER)

        return try {
            val okAck = CompletableDeferred<String>()
            pendingAcks[index] = okAck
            val header = buildHeader(index, totalFiles, safeName, currentFileSize)
            client.sendPayload(endpoint, Payload.fromBytes(header.toByteArray(Charsets.UTF_8))).awaitTask()

            val ok = withTimeoutOrNull(HEADER_TIMEOUT_MS) { okAck.await() }
            if (ok != "OK") return false

            val doneAck = CompletableDeferred<String>()
            pendingAcks[index] = doneAck
            client.sendPayload(endpoint, payload).awaitTask()

            // Large files may legitimately take a long time; allow at least 0.5 MB/s.
            val timeout = maxOf(300_000L, (currentFileSize / (512 * 1024.0) * 1000L).toLong())
            val done = withTimeoutOrNull(timeout) { doneAck.await() }
            if (done != "DONE") return false

            currentFileBytes = currentFileSize
            if (updateUi) emitProgress(Role.SENDER)
            val speed = currentSpeed()
            withContext(Dispatchers.IO) {
                historyStore.addRecord(
                    TransferRecord(
                        direction = Direction.SEND,
                        role = Role.SENDER,
                        fileName = safeName,
                        fileSize = currentFileSize,
                        status = Status.COMPLETED,
                        peerName = connectedPeerName,
                        averageSpeedMbps = speed,
                        durationSeconds = ((System.currentTimeMillis() - transferStart) / 1000).coerceAtLeast(0)
                    )
                )
            }
            true
        } catch (e: Exception) {
            false
        } finally {
            outgoingFilePayloadIds.remove(payload.id)
            pendingAcks.remove(index)
            runCatching { payload.close() }
            runCatching { stagedTemp?.delete() }
        }
    }

    private suspend fun stageToCache(uriString: String, safeName: String): File? =
        withContext(Dispatchers.IO) {
            try {
                val input = contentResolver.openInputStream(Uri.parse(uriString)) ?: return@withContext null
                val temp = File(context.cacheDir, "swiftshare_send_${UUID.randomUUID()}.tmp")
                BufferedInputStream(input, BUFFER_SIZE).use { source ->
                    BufferedOutputStream(FileOutputStream(temp), BUFFER_SIZE).use { target ->
                        source.copyTo(target, BUFFER_SIZE)
                    }
                }
                temp
            } catch (_: Exception) {
                null
            }
        }

    /* ------------------------------------------------------------------ */
    /* receiving                                                          */
    /* ------------------------------------------------------------------ */

    private suspend fun handleBytes(endpointId: String, bytes: ByteArray) {
        val text = String(bytes, Charsets.UTF_8).trim()
        if (text.isEmpty()) return

        if (text.startsWith("OK:")) {
            val index = text.removePrefix("OK:").toIntOrNull() ?: return
            pendingAcks[index]?.complete("OK")
            return
        }
        if (text.startsWith("DONE:")) {
            val index = text.removePrefix("DONE:").toIntOrNull() ?: return
            pendingAcks[index]?.complete("DONE")
            return
        }
        if (text.startsWith("FAIL:")) {
            val index = text.removePrefix("FAIL:").toIntOrNull() ?: return
            pendingAcks[index]?.complete("FAIL")
            return
        }
        if (text == END_MESSAGE) {
            if (activeRole == Role.RECEIVER) {
                _state.value = TransferState.Completed(
                    currentName.ifBlank { "file" },
                    Role.RECEIVER
                )
            }
            return
        }

        if (activeRole != Role.RECEIVER) return
        val header = runCatching { org.json.JSONObject(text) }.getOrNull() ?: return
        val index = header.optInt("i", -1)
        val totalFiles = header.optInt("t", 1)
        val rawName = header.optString("n", "received_file")
        val size = header.optLong("s", 0L)
        if (index < 0) return

        val safeName = FileNaming.sanitize(rawName)
        currentReceiving = ReceivingFile(index, safeName, size)
        currentIndex = index
        currentTotalFiles = totalFiles
        currentName = safeName
        currentFileSize = size.coerceAtLeast(0)
        currentFileBytes = 0L
        transferStart = System.currentTimeMillis()

        emitProgress(Role.RECEIVER)
        runCatching {
            client.sendPayload(
                endpointId,
                Payload.fromBytes("OK:$index".toByteArray(Charsets.UTF_8))
            )
        }
    }

    private suspend fun handleTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
        val payloadId = update.payloadId
        val isOutgoing = payloadId in outgoingFilePayloadIds
        val receiving = currentReceiving
        val isIncoming = receiving?.payloadId == payloadId

        when (update.status) {
            PayloadTransferUpdate.Status.IN_PROGRESS -> {
                if (isOutgoing || isIncoming) {
                    currentFileBytes = update.bytesTransferred
                    if (update.totalBytes > 0) currentFileSize = update.totalBytes
                    emitProgress(if (isOutgoing) Role.SENDER else Role.RECEIVER)
                }
            }
            PayloadTransferUpdate.Status.SUCCESS -> {
                if (isIncoming) {
                    val payload = incomingPayloads.remove(payloadId)
                    if (payload != null) completeIncomingFile(endpointId, payload)
                } else if (isOutgoing) {
                    currentFileBytes = currentFileSize
                    emitProgress(Role.SENDER)
                }
            }
            PayloadTransferUpdate.Status.FAILURE,
            PayloadTransferUpdate.Status.CANCELED -> {
                if (isOutgoing || isIncoming) {
                    if (!isTerminalState()) {
                        _state.value = TransferState.Failed(context.getString(R.string.transfer_failed))
                    }
                }
            }
        }
    }

    private suspend fun completeIncomingFile(endpointId: String, payload: Payload) {
        val receiving = currentReceiving ?: return
        val payloadFile = payload.asFile()
        if (payloadFile == null) {
            failIncoming()
            return
        }

        val temp = File(context.cacheDir, "swiftshare_recv_${UUID.randomUUID()}.tmp")
        val copied = withContext(Dispatchers.IO) {
            try {
                openPayloadStream(payloadFile).use { source ->
                    BufferedOutputStream(FileOutputStream(temp), BUFFER_SIZE).use { target ->
                        source.copyTo(target, BUFFER_SIZE)
                    }
                }
                true
            } catch (_: Exception) {
                false
            } finally {
                runCatching { payload.close() }
            }
        }

        if (!copied) {
            temp.delete()
            failIncoming()
            return
        }

        val length = temp.length()
        if (receiving.size > 0 && length != receiving.size) {
            // Size mismatch: never expose a partial file as complete.
            temp.delete()
            runCatching {
                client.sendPayload(
                    endpointId,
                    Payload.fromBytes("FAIL:${receiving.index}".toByteArray(Charsets.UTF_8))
                )
            }
            failIncoming()
            return
        }

        val finalFile = withContext(Dispatchers.IO) { moveToFinal(temp, receiving.name) }
        if (finalFile == null) {
            temp.delete()
            failIncoming()
            return
        }

        currentFileBytes = length
        currentFileSize = length
        emitProgress(Role.RECEIVER)

        val speed = currentSpeed()
        val duration = ((System.currentTimeMillis() - transferStart) / 1000).coerceAtLeast(0)
        withContext(Dispatchers.IO) {
            historyStore.addRecord(
                TransferRecord(
                    direction = Direction.RECEIVE,
                    role = Role.RECEIVER,
                    fileName = finalFile.name,
                    fileSize = length,
                    status = Status.COMPLETED,
                    peerName = connectedPeerName,
                    averageSpeedMbps = speed,
                    durationSeconds = duration
                )
            )
        }
        currentReceiving = null

        runCatching {
            client.sendPayload(
                endpointId,
                Payload.fromBytes("DONE:${receiving.index}".toByteArray(Charsets.UTF_8))
            )
        }
    }

    private fun openPayloadStream(file: Payload.File): InputStream {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val uri = file.asUri()
                ?: throw IllegalStateException("Cannot open received file")
            contentResolver.openInputStream(uri)
                ?: throw IllegalStateException("Cannot open received file")
        } else {
            val javaFile = file.asJavaFile()
                ?: throw IllegalStateException("Cannot open received file")
            FileInputStream(javaFile)
        }
    }

    private fun moveToFinal(temp: File, originalName: String): File? {
        return try {
            val root = File(
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir,
                "SwiftShare"
            )
            if (!root.exists()) root.mkdirs()

            // Refuse to write when the destination volume cannot hold the file.
            if (root.usableSpace > 0 && root.usableSpace < temp.length()) {
                return null
            }

            val destination = FileNaming.uniqueFile(root, originalName)
            BufferedInputStream(FileInputStream(temp), BUFFER_SIZE).use { source ->
                BufferedOutputStream(FileOutputStream(destination), BUFFER_SIZE).use { target ->
                    source.copyTo(target, BUFFER_SIZE)
                }
            }
            temp.delete()
            destination
        } catch (_: Exception) {
            null
        }
    }

    private fun failIncoming() {
        currentReceiving = null
        if (!isTerminalState()) {
            _state.value = TransferState.Failed(context.getString(R.string.transfer_failed))
        }
    }

    /* ------------------------------------------------------------------ */
    /* progress helpers                                                   */
    /* ------------------------------------------------------------------ */

    private fun emitProgress(role: Role) {
        val snapshot = SpeedCalculator.calculate(
            transferredBytes = currentFileBytes,
            totalBytes = currentFileSize,
            startedAt = transferStart
        )
        _state.value = TransferState.Transferring(
            role = role,
            fileName = currentName,
            index = (currentIndex + 1).coerceAtLeast(1),
            totalFiles = currentTotalFiles.coerceAtLeast(1),
            transferredBytes = currentFileBytes,
            totalBytes = currentFileSize,
            speedMbps = snapshot.speedMbps,
            etaSeconds = snapshot.etaSeconds
        )
    }

    private fun currentSpeed(): Double = SpeedCalculator.calculate(
        transferredBytes = currentFileBytes,
        totalBytes = currentFileSize,
        startedAt = transferStart
    ).speedMbps

    private fun isTerminalState(): Boolean = when (_state.value) {
        is TransferState.Completed,
        is TransferState.Failed,
        is TransferState.Canceled -> true
        else -> false
    }

    /* ------------------------------------------------------------------ */
    /* cancellation / lifecycle                                           */
    /* ------------------------------------------------------------------ */

    fun cancelTransfer() {
        // Set the terminal state first so late callbacks cannot overwrite it with
        // a generic "connection lost" error.
        _state.value = TransferState.Canceled(context.getString(R.string.transfer_canceled))
        advertisingToken++
        discoveryToken++
        stopAdvertisingInternal()
        stopDiscoveryInternal()
        connectedEndpointId = null
        currentReceiving = null
        pendingAcks.values.forEach { if (!it.isCompleted) it.complete("CANCELED") }
        pendingAcks.clear()
        outgoingFilePayloadIds.clear()
        incomingPayloads.values.forEach { runCatching { it.close() } }
        incomingPayloads.clear()
        try {
            client.stopAllEndpoints()
        } catch (_: Exception) {
        }
        refreshCapabilities()
    }

    fun shutdown() {
        advertisingToken++
        discoveryToken++
        stopAdvertisingInternal()
        stopDiscoveryInternal()
        pendingAcks.values.forEach { if (!it.isCompleted) it.complete("CANCELED") }
        pendingAcks.clear()
        inboundCleanup()
        try {
            client.stopAllEndpoints()
        } catch (_: Exception) {
        }
        scope.cancel()
    }

    private fun inboundCleanup() {
        incomingPayloads.values.forEach { runCatching { it.close() } }
        incomingPayloads.clear()
        currentReceiving = null
    }

    /* ------------------------------------------------------------------ */
    /* history                                                            */
    /* ------------------------------------------------------------------ */

    fun getHistory(): List<TransferRecord> = historyStore.getRecords()

    fun clearHistory() {
        historyStore.clear()
    }

    /* ------------------------------------------------------------------ */
    /* benchmark                                                          */
    /* ------------------------------------------------------------------ */

    /**
     * Runs the benchmark on the manager's own scope so navigating away from the
     * screen cannot cancel a measurement halfway through. [onResult] receives null
     * when no real measurement could be completed.
     */
    fun startBenchmark(
        testFileSizeMb: Double,
        endpoint: String,
        onResult: (BenchmarkResult?) -> Unit
    ) {
        scope.launch {
            val result = runBenchmark(testFileSizeMb, endpoint)
            onResult(result)
        }
    }

    /**
     * Measures real end-to-end throughput by sending a generated file through the
     * exact same verified transfer path used for normal files. Returns null when
     * there is no active connection or the transfer fails; it never invents a rate.
     *
     * The test file is written on the receiving device exactly like a normal file,
     * so the measurement includes the receiver's disk write and DONE round-trip.
     */
    suspend fun runBenchmark(testFileSizeMb: Double, endpoint: String): BenchmarkResult? {
        if (!isConnected || currentRole != Role.SENDER) return null
        val sizeBytes = (testFileSizeMb * 1024.0 * 1024.0).toLong()
        if (sizeBytes <= 0) return null

        val temp = File(context.cacheDir, "swiftshare_bench_${UUID.randomUUID()}.tmp")
        val generated = withContext(Dispatchers.IO) { generateTestFile(temp, sizeBytes) }
        if (!generated) {
            temp.delete()
            return null
        }

        val name = "swiftshare_benchmark.bin"
        val payload = try {
            Payload.fromFile(temp).also { it.setFileName(name) }
        } catch (_: Exception) {
            temp.delete()
            return null
        }

        val startedAt = System.currentTimeMillis()
        val ok = try {
            val okAck = CompletableDeferred<String>()
            pendingAcks[BENCHMARK_INDEX] = okAck
            client.sendPayload(
                endpoint,
                Payload.fromBytes(
                    buildHeader(BENCHMARK_INDEX, 1, name, sizeBytes).toByteArray(Charsets.UTF_8)
                )
            ).awaitTask()
            val ready = withTimeoutOrNull(HEADER_TIMEOUT_MS) { okAck.await() }
            if (ready != "OK") {
                false
            } else {
                val doneAck = CompletableDeferred<String>()
                pendingAcks[BENCHMARK_INDEX] = doneAck
                client.sendPayload(endpoint, payload).awaitTask()
                val timeout = maxOf(300_000L, (sizeBytes / (512 * 1024.0) * 1000L).toLong())
                withTimeoutOrNull(timeout) { doneAck.await() } == "DONE"
            }
        } catch (_: Exception) {
            false
        } finally {
            pendingAcks.remove(BENCHMARK_INDEX)
            runCatching { payload.close() }
            temp.delete()
        }

        if (!ok) return null

        // Let the receiving device finish its batch UI.
        runCatching {
            client.sendPayload(
                endpoint,
                Payload.fromBytes(END_MESSAGE.toByteArray(Charsets.UTF_8))
            )
        }

        val elapsed = (System.currentTimeMillis() - startedAt).coerceAtLeast(1)
        return BenchmarkResult(
            testFileSizeBytes = sizeBytes,
            elapsedMs = elapsed,
            throughputMbps = sizeBytes / (1024.0 * 1024.0) / (elapsed / 1000.0)
        )
    }

    private fun generateTestFile(file: File, sizeBytes: Long): Boolean = try {
        BufferedOutputStream(FileOutputStream(file), 256 * 1024).use { output ->
            val buffer = ByteArray(256 * 1024)
            val random = Random(42)
            var written = 0L
            while (written < sizeBytes) {
                val chunk = (sizeBytes - written).coerceAtMost(buffer.size.toLong()).toInt()
                random.nextBytes(buffer)
                output.write(buffer, 0, chunk)
                written += chunk
            }
        }
        file.length() == sizeBytes
    } catch (_: Exception) {
        false
    }

    /* ------------------------------------------------------------------ */
    /* helpers                                                            */
    /* ------------------------------------------------------------------ */

    private fun buildHeader(index: Int, total: Int, name: String, size: Long): String =
        "{\"i\":$index,\"t\":$total,\"n\":${jsonString(name)},\"s\":$size}"

    private fun jsonString(value: String): String {
        val escaped = value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
        return "\"$escaped\""
    }

    private fun resolveError(code: Int): String = when (code) {
        ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> context.getString(R.string.connection_rejected)
        ConnectionsStatusCodes.STATUS_ERROR -> context.getString(R.string.connection_lost)
        ConnectionsStatusCodes.STATUS_ENDPOINT_IO_ERROR -> context.getString(R.string.connection_lost)
        ConnectionsStatusCodes.STATUS_PAYLOAD_IO_ERROR -> context.getString(R.string.transfer_failed)
        else -> ConnectionsStatusCodes.getStatusCodeString(code)
    }

    private class ReceivingFile(
        val index: Int,
        val name: String,
        val size: Long
    ) {
        var payloadId: Long? = null
    }
}
