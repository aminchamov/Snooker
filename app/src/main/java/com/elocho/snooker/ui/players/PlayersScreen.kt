package com.elocho.snooker.ui.players

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elocho.snooker.data.model.Player
import com.elocho.snooker.ui.components.AppLogo
import com.elocho.snooker.ui.components.DiagonalStripeBackground
import com.elocho.snooker.ui.components.ElOchoButton
import com.elocho.snooker.ui.components.ElOchoTextField
import com.elocho.snooker.ui.components.EmptyState
import com.elocho.snooker.ui.components.PlayerAvatar
import com.elocho.snooker.ui.theme.Burgundy
import com.elocho.snooker.ui.theme.DarkSurface
import com.elocho.snooker.ui.theme.DarkSurfaceVariant
import com.elocho.snooker.ui.theme.ErrorRed
import com.elocho.snooker.ui.theme.Gold
import com.elocho.snooker.ui.theme.LightGray
import com.elocho.snooker.ui.theme.PureWhite
import com.elocho.snooker.ui.theme.SuccessGreen
import com.elocho.snooker.utils.ImageUtils

private enum class PlayerSortOption(val label: String) {
    WINS("Wins"),
    LOSSES("Losses"),
    DRAWS("Draws"),
    TOURNAMENTS("Tournaments"),
    MAX_BREAK("Max Break")
}

@Composable
fun PlayersScreen(
    uiState: PlayersUiState,
    onSearchChange: (String) -> Unit,
    onAddPlayer: () -> Unit,
    onEditPlayer: (Player) -> Unit,
    onDeletePlayer: (Player) -> Unit,
    onConfirmDelete: () -> Unit,
    onCancelDelete: () -> Unit,
    onNavigateBack: () -> Unit,
    onFormNameChange: (String) -> Unit,
    onFormImageChange: (String?) -> Unit,
    onSavePlayer: () -> Unit,
    onHideForm: () -> Unit
) {
    var sortOption by remember { mutableStateOf(PlayerSortOption.WINS) }
    val context = LocalContext.current
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val savedUri = ImageUtils.copyUriToInternalStorage(context, it)
            onFormImageChange(savedUri?.toString())
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth >= 1000.dp
        val horizontalPadding = if (isWide) 28.dp else 16.dp
        val rankedPlayers = remember(uiState.players, uiState.maxBreakByPlayerId, sortOption) {
            val base = uiState.players
            val sorted = when (sortOption) {
                PlayerSortOption.WINS -> base.sortedByDescending { it.wins }
                PlayerSortOption.LOSSES -> base.sortedByDescending { it.losses }
                PlayerSortOption.DRAWS -> base.sortedByDescending { it.draws }
                PlayerSortOption.TOURNAMENTS -> base.sortedByDescending { it.tournamentsPlayed }
                PlayerSortOption.MAX_BREAK -> base
            }
            sorted.sortedWith(compareByDescending<Player> {
                when (sortOption) {
                    PlayerSortOption.WINS -> it.wins
                    PlayerSortOption.LOSSES -> it.losses
                    PlayerSortOption.DRAWS -> it.draws
                    PlayerSortOption.TOURNAMENTS -> it.tournamentsPlayed
                    PlayerSortOption.MAX_BREAK -> uiState.maxBreakByPlayerId[it.id] ?: 0
                }
            }.thenBy { it.name.lowercase() })
        }

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
                Spacer(modifier = Modifier.width(6.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "PLAYERS",
                        style = MaterialTheme.typography.headlineSmall,
                        color = PureWhite,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = "Manage profiles and historical stats",
                        style = MaterialTheme.typography.bodySmall,
                        color = LightGray
                    )
                }
                AppLogo(height = 28.dp)
                Spacer(modifier = Modifier.width(8.dp))
                FilledIconButton(
                    onClick = onAddPlayer,
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = Burgundy)
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "Add player", tint = PureWhite)
                }
            }

            ElOchoTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = horizontalPadding),
                placeholder = { Text("Search players...", color = LightGray) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Gold) },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (uiState.players.isEmpty() && !uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = horizontalPadding),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        message = if (uiState.searchQuery.isNotBlank()) {
                            "No players match your search"
                        } else {
                            "No players yet. Add your first player to begin."
                        },
                        actionButton = if (uiState.searchQuery.isBlank()) {
                            { ElOchoButton(text = "ADD PLAYER", onClick = onAddPlayer) }
                        } else {
                            null
                        }
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = horizontalPadding, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        PlayerRankingsCard(
                            players = rankedPlayers,
                            maxBreakByPlayerId = uiState.maxBreakByPlayerId,
                            selectedSort = sortOption,
                            onSortSelected = { sortOption = it }
                        )
                    }
                    items(uiState.players) { player ->
                        PlayerRowCard(
                            player = player,
                            onEdit = { onEditPlayer(player) },
                            onDelete = { onDeletePlayer(player) }
                        )
                    }
                }
            }
        }

        if (uiState.showDeleteDialog && uiState.playerToDelete != null) {
            AlertDialog(
                onDismissRequest = onCancelDelete,
                title = { Text("Delete Player", fontWeight = FontWeight.Bold, color = Gold) },
                text = {
                    Text(
                        "Delete ${uiState.playerToDelete.name}? The player will be archived and removed from future selections while history remains available.",
                        color = PureWhite
                    )
                },
                confirmButton = {
                    TextButton(onClick = onConfirmDelete) {
                        Text("DELETE", color = ErrorRed, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = onCancelDelete) {
                        Text("CANCEL", color = LightGray)
                    }
                },
                containerColor = DarkSurfaceVariant
            )
        }

        if (uiState.showForm) {
            AlertDialog(
                onDismissRequest = onHideForm,
                title = {
                    Text(
                        if (uiState.isEditing) "Edit Player" else "New Player",
                        fontWeight = FontWeight.Bold,
                        color = Gold
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            PlayerAvatar(imageUri = uiState.formImageUri, size = 84.dp)
                        }

                        TextButton(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, tint = Gold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Choose Photo", color = Gold)
                        }

                        ElOchoTextField(
                            value = uiState.formName,
                            onValueChange = onFormNameChange,
                            label = "Player Name",
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (uiState.formError != null) {
                            Text(
                                text = uiState.formError,
                                color = ErrorRed,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = onSavePlayer,
                        enabled = uiState.formName.isNotBlank()
                    ) {
                        Text("SAVE", color = Gold, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = onHideForm) {
                        Text("CANCEL", color = LightGray)
                    }
                },
                containerColor = DarkSurfaceVariant
            )
        }
    }
}

@Composable
private fun PlayerRowCard(
    player: Player,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface.copy(alpha = 0.9f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp, focusedElevation = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlayerAvatar(imageUri = player.imageUri, size = 52.dp)
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = player.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = PureWhite,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatChip(text = "W ${player.wins}", color = SuccessGreen)
                    StatChip(text = "L ${player.losses}", color = ErrorRed)
                    StatChip(text = "D ${player.draws}", color = Gold)
                    StatChip(text = "TW ${player.tournamentsWon}", color = Gold)
                }
            }

            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Gold)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ErrorRed)
            }
        }
    }
}

@Composable
private fun StatChip(text: String, color: androidx.compose.ui.graphics.Color) {
    AssistChip(
        onClick = {},
        enabled = false,
        label = {
            Text(
                text = text,
                color = color,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            disabledContainerColor = Burgundy.copy(alpha = 0.24f),
            disabledLabelColor = color
        )
    )
}

@Composable
private fun PlayerRankingsCard(
    players: List<Player>,
    maxBreakByPlayerId: Map<Long, Int>,
    selectedSort: PlayerSortOption,
    onSortSelected: (PlayerSortOption) -> Unit
) {
    val horizontalScroll = rememberScrollState()
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface.copy(alpha = 0.9f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "PLAYER RANKINGS",
                color = Gold,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PlayerSortOption.entries.forEach { option ->
                    AssistChip(
                        onClick = { onSortSelected(option) },
                        label = { Text(option.label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (selectedSort == option) Burgundy else DarkSurface,
                            labelColor = PureWhite
                        )
                    )
                }
            }

            Row(modifier = Modifier.horizontalScroll(horizontalScroll)) {
                Column(modifier = Modifier.width(760.dp)) {
                    RankingsHeader()
                    players.forEachIndexed { index, player ->
                        RankingsRow(
                            rank = index + 1,
                            player = player,
                            maxBreak = maxBreakByPlayerId[player.id] ?: 0,
                            striped = index % 2 == 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RankingsHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RankingsCell("Rank", 54.dp, true)
        RankingsCell("Player", 210.dp, true, TextAlign.Start)
        RankingsCell("Wins", 70.dp, true)
        RankingsCell("Losses", 74.dp, true)
        RankingsCell("Draws", 70.dp, true)
        RankingsCell("Tourn.", 88.dp, true)
        RankingsCell("Max Break", 96.dp, true)
    }
}

@Composable
private fun RankingsRow(
    rank: Int,
    player: Player,
    maxBreak: Int,
    striped: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val textColor = if (striped) PureWhite else PureWhite.copy(alpha = 0.95f)
        RankingsCell("#$rank", 54.dp, false, color = textColor)
        RankingsCell(player.name, 210.dp, false, TextAlign.Start, textColor)
        RankingsCell(player.wins.toString(), 70.dp, false, color = textColor)
        RankingsCell(player.losses.toString(), 74.dp, false, color = textColor)
        RankingsCell(player.draws.toString(), 70.dp, false, color = textColor)
        RankingsCell(player.tournamentsPlayed.toString(), 88.dp, false, color = textColor)
        RankingsCell(if (maxBreak > 0) maxBreak.toString() else "-", 96.dp, false, color = textColor)
    }
}

@Composable
private fun RankingsCell(
    text: String,
    width: Dp,
    isHeader: Boolean,
    align: TextAlign = TextAlign.Center,
    color: androidx.compose.ui.graphics.Color = PureWhite
) {
    Text(
        text = text,
        modifier = Modifier.width(width),
        color = color,
        fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
        fontSize = if (isHeader) 12.sp else 11.sp,
        textAlign = align,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}
