package com.elocho.snooker.ui.settings

import android.net.Uri
import android.view.KeyEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elocho.snooker.data.model.RemoteMappingAction
import com.elocho.snooker.ui.components.AppLogo
import com.elocho.snooker.ui.components.DiagonalStripeBackground
import com.elocho.snooker.ui.theme.Burgundy
import com.elocho.snooker.ui.theme.DarkSurface
import com.elocho.snooker.ui.theme.DarkSurfaceVariant
import com.elocho.snooker.ui.theme.ErrorRed
import com.elocho.snooker.ui.theme.Gold
import com.elocho.snooker.ui.theme.LightGray
import com.elocho.snooker.ui.theme.PureWhite

private enum class BackupAction {
    EXPORT_PLAYERS,
    EXPORT_TOURNAMENTS,
    EXPORT_ALL,
    IMPORT_PLAYERS,
    IMPORT_TOURNAMENTS,
    IMPORT_ALL
}

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onNavigateBack: () -> Unit,
    onBeginAssignment: (RemoteMappingAction) -> Unit,
    onClearAssignment: (RemoteMappingAction) -> Unit,
    onResetRemoteMappings: () -> Unit,
    onRemoteKeyEvent: (keyCode: Int, isKeyUp: Boolean) -> Boolean,
    onExportPlayers: (Uri) -> Unit,
    onExportTournaments: (Uri) -> Unit,
    onExportAll: (Uri) -> Unit,
    onImportPlayers: (Uri) -> Unit,
    onImportTournaments: (Uri) -> Unit,
    onImportAll: (Uri) -> Unit
) {
    var pendingAction by remember { mutableStateOf<BackupAction?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val action = pendingAction
        pendingAction = null
        if (uri == null || action == null) return@rememberLauncherForActivityResult
        when (action) {
            BackupAction.EXPORT_PLAYERS -> onExportPlayers(uri)
            BackupAction.EXPORT_TOURNAMENTS -> onExportTournaments(uri)
            BackupAction.EXPORT_ALL -> onExportAll(uri)
            else -> Unit
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        val action = pendingAction
        pendingAction = null
        if (uri == null || action == null) return@rememberLauncherForActivityResult
        when (action) {
            BackupAction.IMPORT_PLAYERS -> onImportPlayers(uri)
            BackupAction.IMPORT_TOURNAMENTS -> onImportTournaments(uri)
            BackupAction.IMPORT_ALL -> onImportAll(uri)
            else -> Unit
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .onPreviewKeyEvent { event ->
                val keyCode = event.nativeKeyEvent.keyCode
                onRemoteKeyEvent(keyCode, event.nativeKeyEvent.action == KeyEvent.ACTION_UP)
            }
    ) {
        val horizontalPadding = if (maxWidth >= 1000.dp) 26.dp else 16.dp

        DiagonalStripeBackground()

        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = horizontalPadding, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PureWhite)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "SETTINGS",
                        style = MaterialTheme.typography.headlineSmall,
                        color = PureWhite,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = "Remote mappings and app configuration",
                        style = MaterialTheme.typography.bodySmall,
                        color = LightGray
                    )
                }
                AppLogo(height = 28.dp)
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = horizontalPadding),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface.copy(alpha = 0.9f))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Brand Colors", color = Gold, fontWeight = FontWeight.Bold)
                            Text("Primary: ${uiState.primaryColorHex}", color = PureWhite)
                            Text("Accent: ${uiState.accentColorHex}", color = PureWhite)
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface.copy(alpha = 0.9f))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Backup & Restore",
                                color = Gold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "Export and import players/tournaments as JSON backups.",
                                color = LightGray,
                                style = MaterialTheme.typography.bodySmall
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        pendingAction = BackupAction.EXPORT_PLAYERS
                                        exportLauncher.launch("players_export.json")
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Burgundy)
                                ) {
                                    Text("Export Players")
                                }
                                Button(
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        pendingAction = BackupAction.EXPORT_TOURNAMENTS
                                        exportLauncher.launch("tournaments_export.json")
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Burgundy)
                                ) {
                                    Text("Export Tournaments")
                                }
                            }

                            Button(
                                onClick = {
                                    pendingAction = BackupAction.EXPORT_ALL
                                    exportLauncher.launch("elocho_backup.json")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Burgundy)
                            ) {
                                Text("Export All Data")
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        pendingAction = BackupAction.IMPORT_PLAYERS
                                        importLauncher.launch(arrayOf("application/json", "text/plain"))
                                    }
                                ) {
                                    Text("Import Players")
                                }
                                OutlinedButton(
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        pendingAction = BackupAction.IMPORT_TOURNAMENTS
                                        importLauncher.launch(arrayOf("application/json", "text/plain"))
                                    }
                                ) {
                                    Text("Import Tournaments")
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    pendingAction = BackupAction.IMPORT_ALL
                                    importLauncher.launch(arrayOf("application/json", "text/plain"))
                                }
                            ) {
                                Text("Import All Data")
                            }

                            if (uiState.backupMessage != null) {
                                Text(
                                    text = uiState.backupMessage,
                                    color = if (uiState.isBackupError) ErrorRed else Gold,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface.copy(alpha = 0.9f))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                FilledIconButton(onClick = {}, enabled = false) {
                                    Icon(Icons.Default.Tv, contentDescription = null)
                                }
                                Spacer(modifier = Modifier.size(8.dp))
                                Column {
                                    Text(
                                        text = "TV Remote Mapping",
                                        color = Gold,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                    Text(
                                        text = "Choose action, tap Assign, then press a remote button.",
                                        color = LightGray,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }

                            val rows = listOf(
                                Triple(RemoteMappingAction.UNDO, "Undo", uiState.remoteUndoKeyCode),
                                Triple(RemoteMappingAction.RED, "Red (+1)", uiState.remoteRedKeyCode),
                                Triple(RemoteMappingAction.YELLOW, "Yellow (+2)", uiState.remoteYellowKeyCode),
                                Triple(RemoteMappingAction.GREEN, "Green (+3)", uiState.remoteGreenKeyCode),
                                Triple(RemoteMappingAction.BROWN, "Brown (+4)", uiState.remoteBrownKeyCode),
                                Triple(RemoteMappingAction.BLUE, "Blue (+5)", uiState.remoteBlueKeyCode),
                                Triple(RemoteMappingAction.PINK, "Pink (+6)", uiState.remotePinkKeyCode),
                                Triple(RemoteMappingAction.BLACK, "Black (+7)", uiState.remoteBlackKeyCode),
                                Triple(RemoteMappingAction.ERROR, "Error/Foul (+4)", uiState.remoteErrorKeyCode)
                            )

                            rows.forEach { (action, label, keyCode) ->
                                RemoteMappingRow(
                                    label = label,
                                    keyCode = keyCode,
                                    isListening = uiState.listeningAction == action,
                                    onAssign = { onBeginAssignment(action) },
                                    onClear = { onClearAssignment(action) }
                                )
                            }

                            Button(
                                onClick = onResetRemoteMappings,
                                colors = ButtonDefaults.buttonColors(containerColor = Burgundy)
                            ) {
                                Text("Reset To Defaults")
                            }

                            if (uiState.assignmentMessage != null) {
                                Text(
                                    text = uiState.assignmentMessage,
                                    color = if (uiState.isAssignmentError) ErrorRed else Gold,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface.copy(alpha = 0.9f))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(text = "Detect Remote Button", color = Gold, fontWeight = FontWeight.Bold)
                            Text(
                                text = "Press any remote button to inspect the incoming key code.",
                                color = LightGray,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text("Last KeyCode: ${uiState.lastDetectedKeyCode ?: "-"}", color = PureWhite)
                            Text("Last Key Name: ${uiState.lastDetectedKeyName ?: "-"}", color = PureWhite)
                            Text(
                                text = "Blocked for assignment: BACK, HOME, DPAD, CENTER, ENTER, MENU",
                                color = LightGray,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(12.dp)) }
            }
        }
    }
}

@Composable
private fun RemoteMappingRow(
    label: String,
    keyCode: Int?,
    isListening: Boolean,
    onAssign: () -> Unit,
    onClear: () -> Unit
) {
    val keyLabel = if (keyCode == null) {
        "Not assigned"
    } else {
        val readable = KeyEvent.keyCodeToString(keyCode).removePrefix("KEYCODE_")
        "$readable ($keyCode)"
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant.copy(alpha = 0.65f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    color = PureWhite,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (isListening) "Press a remote button now..." else keyLabel,
                    color = if (isListening) Gold else LightGray,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Button(onClick = onAssign) {
                Text(if (isListening) "Listening" else "Assign")
            }

            OutlinedButton(onClick = onClear, enabled = keyCode != null) {
                Text("Clear")
            }
        }
    }
}
