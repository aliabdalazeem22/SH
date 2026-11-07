package com.example.swiftshare.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.swiftshare.R
import com.example.swiftshare.domain.NearbyTransferManager
import com.example.swiftshare.ui.theme.Colors.OnSurfaceVariant
import com.example.swiftshare.ui.theme.Type.Caption

@Composable
fun HistoryScreen(
    manager: NearbyTransferManager,
    onScreenChange: (Screen) -> Unit,
    contentPadding: androidx.compose.foundation.layout.PaddingValues
) {
    var records by remember { mutableStateOf(manager.getHistory()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        AppBar(
            title = stringResource(R.string.history),
            onNavigateUp = { onScreenChange(Screen.Home) },
            subtitle = stringResource(R.string.history_note)
        )

        if (records.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = OnSurfaceVariant
                        )
                    },
                    title = stringResource(R.string.history_empty),
                    message = stringResource(R.string.history_empty_hint)
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                item { Spacer(modifier = Modifier.height(8.dp)) }
                items(records, key = { it.id }) { record ->
                    HistoryItem(record = record)
                }
            }
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp)) {
                OutlinedButton(
                    text = stringResource(R.string.clear_history),
                    onClick = {
                        manager.clearHistory()
                        records = manager.getHistory()
                    }
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.history_storage_note),
                    style = Caption,
                    color = OnSurfaceVariant
                )
            }
        }
    }
}
