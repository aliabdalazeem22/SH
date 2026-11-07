package com.example.swiftshare

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.example.swiftshare.domain.BenchmarkResult
import com.example.swiftshare.domain.FileItem
import com.example.swiftshare.domain.NearbyTransferManager
import com.example.swiftshare.domain.Role
import com.example.swiftshare.ui.screens.Screen

/**
 * Survives configuration changes so an in-flight Nearby connection is not torn
 * down by a rotation. The transfer manager is closed only when the ViewModel is
 * really cleared (activity finishing), not on every recreation.
 */
class TransferViewModel(application: Application) : AndroidViewModel(application) {

    val manager = NearbyTransferManager(application)

    val selectedFiles = mutableStateListOf<FileItem>()

    var screen by mutableStateOf(Screen.Home)
    var discoveryRole by mutableStateOf(Role.SENDER)
    var benchmarkResult by mutableStateOf<BenchmarkResult?>(null)
    var benchmarkRunning by mutableStateOf(false)

    override fun onCleared() {
        super.onCleared()
        manager.shutdown()
    }
}
