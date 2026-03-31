package com.elocho.snooker.ui.tournament

import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.elocho.snooker.data.model.Player
import com.elocho.snooker.ui.components.AppLogo
import com.elocho.snooker.ui.components.DiagonalStripeBackground
import com.elocho.snooker.ui.components.ElOchoButton
import com.elocho.snooker.ui.components.ElOchoTextField
import com.elocho.snooker.ui.theme.Burgundy
import com.elocho.snooker.ui.theme.DarkSurface
import com.elocho.snooker.ui.theme.DarkSurfaceVariant
import com.elocho.snooker.ui.theme.ErrorRed
import com.elocho.snooker.ui.theme.Gold
import com.elocho.snooker.ui.theme.LightGray
import com.elocho.snooker.ui.theme.PureWhite
import com.elocho.snooker.ui.theme.SuccessGreen

@Composable
fun CreateTournamentScreen(
    uiState: CreateTournamentUiState,
    onNameChange: (String) -> Unit,
    onPlayerCountChange: (Int) -> Unit,
    onAssignPlayerToSlot: (Int, Long?) -> Unit,
    onCreate: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val playerById = remember(uiState.availablePlayers) { uiState.availablePlayers.associateBy { it.id } }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth >= 1100.dp
        val horizontalPadding = if (isWide) 28.dp else 16.dp

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
                        text = "CREATE TOURNAMENT",
                        style = MaterialTheme.typography.headlineSmall,
                        color = PureWhite,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Set bracket size and assign round-one players",
                        style = MaterialTheme.typography.bodySmall,
                        color = LightGray
                    )
                }
                AppLogo(height = 28.dp)
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = horizontalPadding, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface.copy(alpha = 0.9f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ElOchoTextField(
                                value = uiState.name,
                                onValueChange = onNameChange,
                                label = "Tournament Name",
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Text(
                                text = "Player Count",
                                color = Gold,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )

                            // Player count chips - TV focusable
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(listOf(2, 4, 8, 16, 32)) { count ->
                                    var isFocused by remember { mutableStateOf(false) }
                                    AssistChip(
                                        onClick = { onPlayerCountChange(count) },
                                        label = { Text(text = "$count", fontWeight = FontWeight.Bold) },
                                        modifier = Modifier
                                            .onFocusChanged { isFocused = it.isFocused }
                                            .then(
                                                if (isFocused) Modifier.border(
                                                    2.dp, Gold, RoundedCornerShape(8.dp)
                                                ) else Modifier
                                            ),
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = if (uiState.selectedPlayerCount == count) Burgundy else DarkSurface,
                                            labelColor = PureWhite
                                        )
                                    )
                                }
                            }

                            if (uiState.error != null) {
                                Text(
                                    text = uiState.error,
                                    color = ErrorRed,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "Round 1 Slots",
                        color = Gold,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                val matchCount = uiState.selectedPlayerCount / 2
                val usedPlayerIds = uiState.firstRoundSlots.filterNotNull().toSet()

                items((0 until matchCount).toList()) { matchIndex ->
                    val slot1Index = matchIndex * 2
                    val slot2Index = slot1Index + 1
                    val slot1PlayerId = uiState.firstRoundSlots.getOrNull(slot1Index)
                    val slot2PlayerId = uiState.firstRoundSlots.getOrNull(slot2Index)

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface.copy(alpha = 0.9f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Match ${matchIndex + 1}",
                                color = Gold,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )

                            TvFriendlyPlayerPicker(
                                label = "Player A",
                                selectedPlayerId = slot1PlayerId,
                                players = uiState.availablePlayers,
                                playerById = playerById,
                                usedPlayerIds = usedPlayerIds,
                                onSelect = { onAssignPlayerToSlot(slot1Index, it) }
                            )

                            TvFriendlyPlayerPicker(
                                label = "Player B",
                                selectedPlayerId = slot2PlayerId,
                                players = uiState.availablePlayers,
                                playerById = playerById,
                                usedPlayerIds = usedPlayerIds,
                                onSelect = { onAssignPlayerToSlot(slot2Index, it) }
                            )
                        }
                    }
                }

                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        ElOchoButton(
                            text = "CREATE TOURNAMENT",
                            onClick = onCreate,
                            enabled = uiState.name.isNotBlank() &&
                                uiState.firstRoundSlots.filterNotNull().size == uiState.selectedPlayerCount
                        )
                    }
                }
            }
        }
    }
}

/**
 * TV-friendly player picker that uses a dialog list instead of a dropdown menu.
 * Fully navigable with D-pad remote: focus the card, press Enter/Center to open,
 * navigate the list with Up/Down, select with Enter/Center.
 */
@Composable
private fun TvFriendlyPlayerPicker(
    label: String,
    selectedPlayerId: Long?,
    players: List<Player>,
    playerById: Map<Long, Player>,
    usedPlayerIds: Set<Long>,
    onSelect: (Long?) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }
    val selectedName = selectedPlayerId?.let { playerById[it]?.name } ?: "Select Player"
    val hasSelection = selectedPlayerId != null

    // The clickable/focusable card that opens the picker dialog
    Card(
        onClick = { showDialog = true },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .then(
                if (isFocused) Modifier.border(
                    2.dp, Gold, RoundedCornerShape(12.dp)
                ) else Modifier
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurfaceVariant.copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            focusedElevation = 8.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = if (hasSelection) Gold else LightGray.copy(alpha = 0.5f),
                modifier = Modifier.padding(end = 10.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = Gold.copy(alpha = 0.7f),
                    fontSize = 10.sp
                )
                Text(
                    text = selectedName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (hasSelection) PureWhite else LightGray.copy(alpha = 0.5f),
                    fontWeight = if (hasSelection) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (hasSelection) {
                IconButton(onClick = { onSelect(null) }) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Clear",
                        tint = LightGray.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }

    // Full-screen dialog picker for TV remote navigation
    if (showDialog) {
        PlayerPickerDialog(
            label = label,
            players = players,
            selectedPlayerId = selectedPlayerId,
            usedPlayerIds = usedPlayerIds,
            onSelect = { playerId ->
                onSelect(playerId)
                showDialog = false
            },
            onDismiss = { showDialog = false }
        )
    }
}

/**
 * Dialog-based player picker with full D-pad/remote support.
 * Players are shown in a scrollable list with clear focus indicators.
 */
@Composable
private fun PlayerPickerDialog(
    label: String,
    players: List<Player>,
    selectedPlayerId: Long?,
    usedPlayerIds: Set<Long>,
    onSelect: (Long?) -> Unit,
    onDismiss: () -> Unit
) {
    // Track which item is focused for TV remote
    var focusedIndex by remember { mutableIntStateOf(-1) }
    val listState = rememberLazyListState()
    val firstFocusRequester = remember { FocusRequester() }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 500.dp)
                .onKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown) {
                        when (event.key) {
                            Key.Back, Key.Escape -> {
                                onDismiss()
                                true
                            }
                            else -> false
                        }
                    } else false
                },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Select $label",
                        style = MaterialTheme.typography.titleMedium,
                        color = Gold,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = LightGray)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Clear slot option
                var clearFocused by remember { mutableStateOf(false) }
                Card(
                    onClick = { onSelect(null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { clearFocused = it.isFocused }
                        .then(
                            if (clearFocused) Modifier.border(
                                2.dp, Gold, RoundedCornerShape(12.dp)
                            ) else Modifier
                        ),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
                ) {
                    Text(
                        text = "Clear Slot",
                        color = LightGray,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Player list
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    itemsIndexed(players) { index, player ->
                        val isSelectable = !usedPlayerIds.contains(player.id) || selectedPlayerId == player.id
                        val isCurrentlySelected = selectedPlayerId == player.id
                        var itemFocused by remember { mutableStateOf(false) }

                        val focusReq = if (index == 0) firstFocusRequester else remember { FocusRequester() }

                        Card(
                            onClick = {
                                if (isSelectable) onSelect(player.id)
                            },
                            enabled = isSelectable,
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusReq)
                                .onFocusChanged {
                                    itemFocused = it.isFocused
                                    if (it.isFocused) focusedIndex = index
                                }
                                .then(
                                    if (itemFocused) Modifier.border(
                                        2.dp, Gold, RoundedCornerShape(12.dp)
                                    ) else Modifier
                                ),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = when {
                                    isCurrentlySelected -> Burgundy.copy(alpha = 0.8f)
                                    !isSelectable -> DarkSurfaceVariant.copy(alpha = 0.4f)
                                    else -> DarkSurfaceVariant
                                },
                                disabledContainerColor = DarkSurfaceVariant.copy(alpha = 0.4f)
                            ),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = 2.dp,
                                focusedElevation = 8.dp
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = player.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = when {
                                        isCurrentlySelected -> Gold
                                        !isSelectable -> LightGray.copy(alpha = 0.35f)
                                        else -> PureWhite
                                    },
                                    fontWeight = if (isCurrentlySelected) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )

                                if (isCurrentlySelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = SuccessGreen
                                    )
                                } else if (!isSelectable) {
                                    Text(
                                        text = "In use",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = LightGray.copy(alpha = 0.4f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Request focus on first player for TV remote
    LaunchedEffect(Unit) {
        try {
            firstFocusRequester.requestFocus()
        } catch (_: Exception) {
            // Focus request may fail if not yet composed
        }
    }
}
