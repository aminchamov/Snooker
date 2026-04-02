package com.elocho.snooker.ui.match

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elocho.snooker.ui.components.AppLogo
import com.elocho.snooker.ui.components.DiagonalStripeBackground
import com.elocho.snooker.ui.components.ElOchoButton
import com.elocho.snooker.ui.components.PlayerAvatar
import com.elocho.snooker.ui.theme.Burgundy
import com.elocho.snooker.ui.theme.DarkSurface
import com.elocho.snooker.ui.theme.DarkSurfaceVariant
import com.elocho.snooker.ui.theme.ErrorRed
import com.elocho.snooker.ui.theme.Gold
import com.elocho.snooker.ui.theme.GoldLight
import com.elocho.snooker.ui.theme.LightGray
import com.elocho.snooker.ui.theme.MediumGray
import com.elocho.snooker.ui.theme.PureWhite
import com.elocho.snooker.ui.theme.SnookerBlack
import com.elocho.snooker.ui.theme.SnookerBlue
import com.elocho.snooker.ui.theme.SnookerBrown
import com.elocho.snooker.ui.theme.SnookerGreen
import com.elocho.snooker.ui.theme.SnookerPink
import com.elocho.snooker.ui.theme.SnookerRed
import com.elocho.snooker.ui.theme.SnookerYellow
import kotlin.math.min

@Composable
fun ScoreboardScreen(
    uiState: ScoreboardUiState,
    onScoreAction: (Int, SnookerScoreAction) -> Unit,
    onCorrection: (Int) -> Unit,
    onUndo: () -> Unit,
    remoteRedKeyCode: Int?,
    remoteUndoKeyCode: Int?,
    remoteYellowKeyCode: Int?,
    remoteGreenKeyCode: Int?,
    remoteBrownKeyCode: Int?,
    remoteBlueKeyCode: Int?,
    remotePinkKeyCode: Int?,
    remoteBlackKeyCode: Int?,
    remoteErrorKeyCode: Int?,
    onRemoteScoreAction: (SnookerScoreAction) -> Unit,
    onEndGame: () -> Unit,
    isTournamentMatch: Boolean = false,
    onEndTournamentGame: (() -> Unit)? = null,
    onTvPlayerSelect: (Int) -> Unit = {},
    onDismissEndMatchConfirmation: () -> Unit = {}
) {
    val hours = uiState.elapsedSeconds / 3600
    val minutes = (uiState.elapsedSeconds % 3600) / 60
    val seconds = uiState.elapsedSeconds % 60
    val timeString = String.format("%02d:%02d:%02d", hours, minutes, seconds)
    val endGameAction = if (isTournamentMatch) (onEndTournamentGame ?: onEndGame) else onEndGame

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onPreviewKeyEvent { keyEvent ->
                val keyCode = keyEvent.nativeKeyEvent.keyCode
                val isKeyUp = keyEvent.nativeKeyEvent.action == android.view.KeyEvent.ACTION_UP

                // DPAD_LEFT / DPAD_RIGHT: select which player receives remote scoring.
                // Consume on both DOWN and UP; act only on UP to match scoring button strategy.
                if (keyCode == android.view.KeyEvent.KEYCODE_DPAD_LEFT) {
                    if (isKeyUp) onTvPlayerSelect(1)
                    return@onPreviewKeyEvent true
                }
                if (keyCode == android.view.KeyEvent.KEYCODE_DPAD_RIGHT) {
                    if (isKeyUp) onTvPlayerSelect(2)
                    return@onPreviewKeyEvent true
                }
                if (keyCode == remoteUndoKeyCode) {
                    if (isKeyUp) onUndo()
                    return@onPreviewKeyEvent true
                }

                val action = when (keyCode) {
                    remoteRedKeyCode -> SnookerScoreAction.RED
                    remoteYellowKeyCode -> SnookerScoreAction.YELLOW
                    remoteGreenKeyCode -> SnookerScoreAction.GREEN
                    remoteBrownKeyCode -> SnookerScoreAction.BROWN
                    remoteBlueKeyCode -> SnookerScoreAction.BLUE
                    remotePinkKeyCode -> SnookerScoreAction.PINK
                    remoteBlackKeyCode -> SnookerScoreAction.BLACK
                    remoteErrorKeyCode -> SnookerScoreAction.ERROR
                    else -> null
                }
                if (action != null) {
                    if (isKeyUp) {
                        onRemoteScoreAction(action)
                    }
                    true
                } else {
                    false
                }
            }
    ) {
        DiagonalStripeBackground()

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isPortrait = maxWidth < 840.dp && maxWidth <= maxHeight
            val horizontalPadding = if (maxWidth >= 1200.dp) 28.dp else 16.dp

            if (isPortrait) {
                PortraitScoreboard(
                    uiState = uiState,
                    timeString = timeString,
                    onScoreAction = onScoreAction,
                    onCorrection = onCorrection,
                    onUndo = onUndo,
                    onEndGame = endGameAction,
                    horizontalPadding = horizontalPadding,
                    tvSelectedPlayer = uiState.tvSelectedPlayer
                )
            } else {
                LandscapeScoreboard(
                    uiState = uiState,
                    timeString = timeString,
                    onScoreAction = onScoreAction,
                    onCorrection = onCorrection,
                    onUndo = onUndo,
                    onEndGame = endGameAction,
                    horizontalPadding = horizontalPadding,
                    tvSelectedPlayer = uiState.tvSelectedPlayer
                )
            }
        }

        if (uiState.showEndMatchConfirmation) {
            AlertDialog(
                onDismissRequest = onDismissEndMatchConfirmation,
                title = {
                    Text(
                        text = "End Match?",
                        color = Gold,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = "The black ball has been potted. End the match now?",
                        color = PureWhite
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onDismissEndMatchConfirmation()
                            endGameAction()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Burgundy)
                    ) {
                        Text("Yes", color = PureWhite, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismissEndMatchConfirmation) {
                        Text("No", color = Gold, fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = DarkSurface,
                titleContentColor = Gold,
                textContentColor = PureWhite,
                shape = RoundedCornerShape(18.dp)
            )
        }
    }
}

@Composable
private fun LandscapeScoreboard(
    uiState: ScoreboardUiState,
    timeString: String,
    onScoreAction: (Int, SnookerScoreAction) -> Unit,
    onCorrection: (Int) -> Unit,
    onUndo: () -> Unit,
    onEndGame: () -> Unit,
    horizontalPadding: androidx.compose.ui.unit.Dp,
    tvSelectedPlayer: Int = 1
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = horizontalPadding, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlayerHeaderCard(
                playerName = uiState.player1?.name ?: "Player 1",
                imageUri = uiState.player1?.imageUri,
                score = uiState.player1Score,
                isActive = uiState.activePlayerNumber == 1,
                isTvSelected = tvSelectedPlayer == 1,
                modifier = Modifier.weight(1f)
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AppLogo(height = 34.dp)
                Spacer(modifier = Modifier.height(4.dp))
                TimerCard(
                    timeString = timeString,
                    queueCount = uiState.queuedPlayersCount,
                    setScoreText = if (uiState.activeTournamentId != null) "${uiState.tournamentPlayer1SetsWon}-${uiState.tournamentPlayer2SetsWon}" else null,
                    setsToWin = if (uiState.activeTournamentId != null) uiState.tournamentSetsToWin else null
                )
                Spacer(modifier = Modifier.height(4.dp))
                PointsLeftCard(pointsLeft = uiState.pointsLeftToScore147)
                Spacer(modifier = Modifier.height(4.dp))
                LiveBreakStatsCard(uiState = uiState)
            }
            PlayerHeaderCard(
                playerName = uiState.player2?.name ?: "Player 2",
                imageUri = uiState.player2?.imageUri,
                score = uiState.player2Score,
                isActive = uiState.activePlayerNumber == 2,
                isTvSelected = tvSelectedPlayer == 2,
                modifier = Modifier.weight(1f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ScoreControlPanel(
                    title = uiState.player1?.name ?: "Player 1",
                    score = uiState.player1Score,
                    onScoreAction = { action -> onScoreAction(1, action) },
                    modifier = Modifier
                        .weight(1f)
                        .height(150.dp),
                    compact = true
                )
                ScoreControlPanel(
                    title = uiState.player2?.name ?: "Player 2",
                    score = uiState.player2Score,
                    onScoreAction = { action -> onScoreAction(2, action) },
                    modifier = Modifier
                        .weight(1f)
                        .height(150.dp),
                    compact = true
                )
            }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.TopCenter
            ) {
                val tableWidth = min(maxWidth.value * 0.9f, maxHeight.value * 2f).dp
                SnookerTableVisual(
                    state = uiState.tableState,
                    modifier = Modifier
                        .width(tableWidth)
                        .aspectRatio(2f)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledIconButton(
                onClick = onUndo,
                modifier = Modifier.size(54.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = DarkSurfaceVariant)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Undo,
                    contentDescription = "Undo",
                    tint = PureWhite
                )
            }

            ElOchoButton(
                text = "END GAME",
                onClick = onEndGame,
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp),
                containerColor = Burgundy
            )
        }
    }
}

@Composable
private fun PortraitScoreboard(
    uiState: ScoreboardUiState,
    timeString: String,
    onScoreAction: (Int, SnookerScoreAction) -> Unit,
    onCorrection: (Int) -> Unit,
    onUndo: () -> Unit,
    onEndGame: () -> Unit,
    horizontalPadding: androidx.compose.ui.unit.Dp,
    tvSelectedPlayer: Int = 1
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = horizontalPadding, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PlayerHeaderCard(
                playerName = uiState.player1?.name ?: "Player 1",
                imageUri = uiState.player1?.imageUri,
                score = uiState.player1Score,
                isActive = uiState.activePlayerNumber == 1,
                isTvSelected = tvSelectedPlayer == 1,
                modifier = Modifier.weight(1f)
            )
            PlayerHeaderCard(
                playerName = uiState.player2?.name ?: "Player 2",
                imageUri = uiState.player2?.imageUri,
                score = uiState.player2Score,
                isActive = uiState.activePlayerNumber == 2,
                isTvSelected = tvSelectedPlayer == 2,
                modifier = Modifier.weight(1f)
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AppLogo(height = 46.dp)
            TimerCard(
                timeString = timeString,
                queueCount = uiState.queuedPlayersCount,
                setScoreText = if (uiState.activeTournamentId != null) "${uiState.tournamentPlayer1SetsWon}-${uiState.tournamentPlayer2SetsWon}" else null,
                setsToWin = if (uiState.activeTournamentId != null) uiState.tournamentSetsToWin else null
            )
            PointsLeftCard(pointsLeft = uiState.pointsLeftToScore147)
            LiveBreakStatsCard(uiState = uiState)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ScoreControlPanel(
                title = uiState.player1?.name ?: "Player 1",
                score = uiState.player1Score,
                onScoreAction = { action -> onScoreAction(1, action) },
                modifier = Modifier
                    .weight(1f)
                    .height(160.dp),
                compact = true
            )
            ScoreControlPanel(
                title = uiState.player2?.name ?: "Player 2",
                score = uiState.player2Score,
                onScoreAction = { action -> onScoreAction(2, action) },
                modifier = Modifier
                    .weight(1f)
                    .height(160.dp),
                compact = true
            )
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.TopCenter
        ) {
            val tableWidth = min(maxWidth.value * 0.94f, maxHeight.value * 2f).dp
            SnookerTableVisual(
                state = uiState.tableState,
                modifier = Modifier
                    .width(tableWidth)
                    .aspectRatio(2f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledIconButton(
                onClick = onUndo,
                modifier = Modifier.size(52.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = DarkSurfaceVariant)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Undo,
                    contentDescription = "Undo",
                    tint = PureWhite
                )
            }

            ElOchoButton(
                text = "END GAME",
                onClick = onEndGame,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                containerColor = Burgundy
            )
        }
    }
}

@Composable
private fun PlayerHeaderCard(
    playerName: String,
    imageUri: String?,
    score: Int,
    isActive: Boolean,
    isTvSelected: Boolean = false,
    modifier: Modifier = Modifier
) {
    val cardModifier = if (isTvSelected) {
        modifier.border(width = 2.5.dp, color = Gold, shape = RoundedCornerShape(14.dp))
    } else {
        modifier
    }
    Card(
        modifier = cardModifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) Burgundy.copy(alpha = 0.86f) else DarkSurface.copy(alpha = 0.9f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlayerAvatar(imageUri = imageUri, size = 42.dp, borderColor = if (isActive) Gold else LightGray)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playerName,
                    color = PureWhite,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Score $score",
                    color = GoldLight,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun TimerCard(
    timeString: String,
    queueCount: Int,
    setScoreText: String? = null,
    setsToWin: Int? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface.copy(alpha = 0.9f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.Timer, contentDescription = null, tint = Gold, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = timeString,
                color = PureWhite,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 20.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Queue",
                tint = GoldLight,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = queueCount.toString(),
                color = GoldLight,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp
            )
            if (!setScoreText.isNullOrBlank()) {
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "S",
                    color = Gold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = setScoreText,
                    color = GoldLight,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp
                )
                if (setsToWin != null && setsToWin > 0) {
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "/$setsToWin",
                        color = LightGray,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun PointsLeftCard(
    pointsLeft: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface.copy(alpha = 0.9f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "PTS LEFT",
                color = Gold,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "$pointsLeft",
                color = PureWhite,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 18.sp
            )
        }
    }
}

@Composable
private fun ScoreControlPanel(
    title: String,
    score: Int,
    onScoreAction: (SnookerScoreAction) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            SnookerBlack.copy(alpha = 0.98f),
                            SnookerRed.copy(alpha = 0.9f)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    color = Gold,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                ScoreControl(
                    score = score,
                    onScoreAction = onScoreAction,
                    compact = compact
                )
            }
        }
    }
}

@Composable
private fun LiveBreakStatsCard(
    uiState: ScoreboardUiState,
    modifier: Modifier = Modifier
) {
    val activeBreak = when (uiState.activePlayerNumber) {
        1 -> uiState.currentBreakPlayer1
        2 -> uiState.currentBreakPlayer2
        else -> 0
    }
    val activePlayerLabel = when (uiState.activePlayerNumber) {
        1 -> "P1"
        2 -> "P2"
        else -> "--"
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface.copy(alpha = 0.9f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "LIVE BREAK",
                color = Gold,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = activeBreak.toString(),
                color = PureWhite,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = activePlayerLabel,
                color = GoldLight,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ScoreControl(
    score: Int,
    onScoreAction: (SnookerScoreAction) -> Unit,
    compact: Boolean
) {
    val actions = remember {
        listOf(
            SnookerScoreAction.RED,
            SnookerScoreAction.YELLOW,
            SnookerScoreAction.GREEN,
            SnookerScoreAction.BROWN,
            SnookerScoreAction.BLUE,
            SnookerScoreAction.PINK,
            SnookerScoreAction.BLACK,
            SnookerScoreAction.ERROR
        )
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 6.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 6.dp),
            maxItemsInEachRow = if (compact) 4 else 3
        ) {
            actions.forEach { action ->
                ScoreActionButton(
                    action = action,
                    onClick = { onScoreAction(action) },
                    compact = compact
                )
            }
        }

        Text(
            text = score.toString(),
            fontSize = if (compact) 44.sp else 54.sp,
            fontWeight = FontWeight.ExtraBold,
            color = PureWhite,
            modifier = Modifier.padding(vertical = 4.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ScoreActionButton(
    action: SnookerScoreAction,
    onClick: () -> Unit,
    compact: Boolean
) {
    val (containerColor, contentColor) = when (action) {
        SnookerScoreAction.RED -> SnookerRed to PureWhite
        SnookerScoreAction.YELLOW -> SnookerYellow to DarkSurface
        SnookerScoreAction.GREEN -> SnookerGreen to PureWhite
        SnookerScoreAction.BROWN -> SnookerBrown to PureWhite
        SnookerScoreAction.BLUE -> SnookerBlue to PureWhite
        SnookerScoreAction.PINK -> SnookerPink to DarkSurface
        SnookerScoreAction.BLACK -> SnookerBlack to PureWhite
        SnookerScoreAction.ERROR -> ErrorRed to PureWhite
    }

    val size = when {
        action == SnookerScoreAction.ERROR && compact -> 20.dp
        action == SnookerScoreAction.ERROR -> 30.dp
        compact -> 24.dp
        else -> 34.dp
    }

    Button(
        onClick = onClick,
        modifier = Modifier.size(size),
        shape = CircleShape,
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(containerColor = containerColor, contentColor = contentColor)
    ) {
        if (action == SnookerScoreAction.ERROR) {
            Icon(imageVector = Icons.Default.Close, contentDescription = "Error", tint = PureWhite,
                modifier = Modifier.size(size * 0.6f))
        }
    }
}
