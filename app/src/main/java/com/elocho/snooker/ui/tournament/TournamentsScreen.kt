package com.elocho.snooker.ui.tournament

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elocho.snooker.data.model.Tournament
import com.elocho.snooker.ui.components.AppLogo
import com.elocho.snooker.ui.components.DiagonalStripeBackground
import com.elocho.snooker.ui.components.ElOchoButton
import com.elocho.snooker.ui.components.EmptyState
import com.elocho.snooker.ui.theme.Burgundy
import com.elocho.snooker.ui.theme.DarkSurface
import com.elocho.snooker.ui.theme.DarkSurfaceVariant
import com.elocho.snooker.ui.theme.ErrorRed
import com.elocho.snooker.ui.theme.Gold
import com.elocho.snooker.ui.theme.LightGray
import com.elocho.snooker.ui.theme.PureWhite
import com.elocho.snooker.ui.theme.SuccessGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TournamentsScreen(
    tournaments: List<Tournament>,
    onCreateClick: () -> Unit,
    onTournamentClick: (Long) -> Unit,
    onDeleteClick: (Long) -> Unit,
    onNavigateBack: () -> Unit
) {
    var deleteConfirm by remember { mutableStateOf<Long?>(null) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth >= 900.dp
        val horizontalPadding = if (isWide) 26.dp else 16.dp

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
                        text = "TOURNAMENTS",
                        style = MaterialTheme.typography.headlineSmall,
                        color = PureWhite,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = "Create brackets and manage progression",
                        style = MaterialTheme.typography.bodySmall,
                        color = LightGray
                    )
                }
                AppLogo(height = 28.dp)
                Spacer(modifier = Modifier.width(8.dp))
                FilledIconButton(
                    onClick = onCreateClick,
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = Burgundy)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create", tint = PureWhite)
                }
            }

            if (tournaments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = horizontalPadding),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        message = "No tournaments yet. Create your first bracket.",
                        actionButton = { ElOchoButton(text = "CREATE TOURNAMENT", onClick = onCreateClick) }
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = horizontalPadding, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(tournaments) { tournament ->
                        TournamentRowCard(
                            tournament = tournament,
                            onOpen = { onTournamentClick(tournament.id) },
                            onDelete = { deleteConfirm = tournament.id }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(6.dp)) }
                }
            }
        }

        if (deleteConfirm != null) {
            AlertDialog(
                onDismissRequest = { deleteConfirm = null },
                title = { Text("Delete Tournament", color = Gold, fontWeight = FontWeight.Bold) },
                text = { Text("This removes the tournament and all bracket data.", color = PureWhite) },
                confirmButton = {
                    TextButton(onClick = {
                        onDeleteClick(deleteConfirm!!)
                        deleteConfirm = null
                    }) {
                        Text("DELETE", color = ErrorRed, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { deleteConfirm = null }) {
                        Text("CANCEL", color = LightGray)
                    }
                },
                containerColor = DarkSurfaceVariant
            )
        }
    }
}

@Composable
private fun TournamentRowCard(
    tournament: Tournament,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        onClick = onOpen,
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
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = null,
                tint = when (tournament.status) {
                    "completed" -> Gold
                    "in_progress" -> SuccessGreen
                    else -> LightGray
                },
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = tournament.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = PureWhite,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${tournament.playerCount} players  •  ${statusLabel(tournament.status)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = LightGray
                )
                Text(
                    text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(tournament.createdAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = LightGray.copy(alpha = 0.9f)
                )
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ErrorRed.copy(alpha = 0.86f))
            }
        }
    }
}

private fun statusLabel(status: String): String = when (status) {
    "completed" -> "Completed"
    "in_progress" -> "In progress"
    else -> "Created"
}
