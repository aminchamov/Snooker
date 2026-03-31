package com.elocho.snooker.ui.match

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elocho.snooker.data.model.Player
import com.elocho.snooker.ui.components.AppLogo
import com.elocho.snooker.ui.components.DiagonalStripeBackground
import com.elocho.snooker.ui.components.ElOchoButton
import com.elocho.snooker.ui.components.PlayerAvatar
import com.elocho.snooker.ui.theme.Burgundy
import com.elocho.snooker.ui.theme.DarkSurface
import com.elocho.snooker.ui.theme.Gold
import com.elocho.snooker.ui.theme.LightGray
import com.elocho.snooker.ui.theme.PureWhite

@Composable
fun PlayerSelectionScreen(
    players: List<Player>,
    maxSelection: Int = 2,
    onPlayersSelected: (List<Long>) -> Unit,
    onNavigateBack: () -> Unit,
    onAddPlayer: () -> Unit
) {
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth >= 900.dp
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
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "SELECT PLAYERS",
                        style = MaterialTheme.typography.headlineSmall,
                        color = PureWhite,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = "Pick $maxSelection players to start a quick match",
                        style = MaterialTheme.typography.bodySmall,
                        color = LightGray
                    )
                }
                SelectionCountPill(selectedCount = selectedIds.size, maxSelection = maxSelection)
                Spacer(modifier = Modifier.width(8.dp))
                AppLogo(height = 28.dp)
            }

            if (players.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = horizontalPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface.copy(alpha = 0.9f))
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(horizontal = 24.dp, vertical = 26.dp)
                                .width(if (isWide) 420.dp else 320.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Groups,
                                contentDescription = null,
                                tint = Gold.copy(alpha = 0.85f),
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No players yet",
                                style = MaterialTheme.typography.titleLarge,
                                color = PureWhite,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Create at least two players before starting a match.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = LightGray
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                            ElOchoButton(text = "ADD PLAYER", onClick = onAddPlayer)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = horizontalPadding, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(players) { player ->
                        val isSelected = player.id in selectedIds
                        Card(
                            onClick = {
                                selectedIds = if (isSelected) {
                                    selectedIds - player.id
                                } else if (selectedIds.size < maxSelection) {
                                    selectedIds + player.id
                                } else {
                                    selectedIds
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) Burgundy.copy(alpha = 0.86f) else DarkSurface.copy(alpha = 0.88f)
                            ),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = if (isSelected) 10.dp else 4.dp,
                                focusedElevation = 14.dp
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                PlayerAvatar(
                                    imageUri = player.imageUri,
                                    size = if (isWide) 56.dp else 50.dp,
                                    borderColor = if (isSelected) Gold else LightGray
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = player.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = PureWhite,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "W ${player.wins}   L ${player.losses}   D ${player.draws}   TW ${player.tournamentsWon}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = LightGray
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Gold,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = horizontalPadding, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onAddPlayer,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.height(52.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Gold)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "NEW PLAYER", color = PureWhite, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.weight(1f))

                ElOchoButton(
                    text = "START MATCH",
                    onClick = { onPlayersSelected(selectedIds.toList()) },
                    enabled = selectedIds.size == maxSelection,
                    modifier = Modifier.height(52.dp)
                )
            }
        }
    }
}

@Composable
private fun SelectionCountPill(selectedCount: Int, maxSelection: Int) {
    Card(
        shape = RoundedCornerShape(999.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface.copy(alpha = 0.86f))
    ) {
        Text(
            text = "$selectedCount / $maxSelection",
            color = Gold,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}
