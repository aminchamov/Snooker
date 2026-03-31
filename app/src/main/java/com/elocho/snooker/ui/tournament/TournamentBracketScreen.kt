package com.elocho.snooker.ui.tournament

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Remove
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elocho.snooker.data.model.Player
import com.elocho.snooker.data.model.TournamentMatch
import com.elocho.snooker.ui.components.AppLogo
import com.elocho.snooker.ui.components.DiagonalStripeBackground
import com.elocho.snooker.ui.components.PlayerAvatar
import com.elocho.snooker.ui.theme.BurgundyDark
import com.elocho.snooker.ui.theme.DarkSurface
import com.elocho.snooker.ui.theme.DarkSurfaceVariant
import com.elocho.snooker.ui.theme.Gold
import com.elocho.snooker.ui.theme.GoldDark
import com.elocho.snooker.ui.theme.LightGray
import com.elocho.snooker.ui.theme.PureWhite
import com.elocho.snooker.ui.theme.SuccessGreen

@Composable
fun TournamentBracketScreen(
    uiState: BracketUiState,
    onMatchClick: (TournamentMatch) -> Unit,
    onNavigateBack: () -> Unit,
    onResolveDrawWinner: (Long) -> Unit,
    onDismissDrawDialog: () -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val horizontalPadding = if (maxWidth > 1200.dp) 30.dp else 16.dp

        DiagonalStripeBackground()

        Column(modifier = Modifier.fillMaxSize()) {
            // Header
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
                        text = uiState.tournament?.name ?: "Tournament",
                        style = MaterialTheme.typography.headlineSmall,
                        color = PureWhite,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (uiState.isFinished) {
                        val champion = uiState.players[uiState.tournament?.championPlayerId]
                        Text(
                            text = "Champion: ${champion?.name ?: "Unknown"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Gold,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                AppLogo(height = 28.dp)
            }

            // Bracket content
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = horizontalPadding, vertical = 8.dp)
            ) {
                if (uiState.totalRounds > 0) {
                    var zoom by rememberSaveable { mutableFloatStateOf(1f) }
                    val minZoom = 0.5f
                    val maxZoom = 2.5f
                    val horizontalScroll = rememberScrollState()
                    val verticalScroll = rememberScrollState()

                    // Responsive sizing
                    val matchCardHeight = when {
                        maxWidth < 600.dp -> 56.dp
                        maxWidth < 900.dp -> 64.dp
                        else -> 72.dp
                    }
                    val matchCardWidth = when {
                        maxWidth < 600.dp -> 140.dp
                        maxWidth < 900.dp -> 170.dp
                        else -> 200.dp
                    }
                    val connectorWidth = when {
                        maxWidth < 600.dp -> 20.dp
                        maxWidth < 900.dp -> 28.dp
                        else -> 36.dp
                    }
                    val verticalGap = when {
                        maxWidth < 600.dp -> 6.dp
                        maxWidth < 900.dp -> 10.dp
                        else -> 14.dp
                    }

                    Column(modifier = Modifier.fillMaxSize()) {
                        // Zoom controls
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledIconButton(
                                onClick = { zoom = (zoom - 0.1f).coerceIn(minZoom, maxZoom) },
                                colors = IconButtonDefaults.filledIconButtonColors(containerColor = DarkSurfaceVariant)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Zoom out", tint = PureWhite)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${(zoom * 100).toInt()}%",
                                color = LightGray,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            FilledIconButton(
                                onClick = { zoom = (zoom + 0.1f).coerceIn(minZoom, maxZoom) },
                                colors = IconButtonDefaults.filledIconButtonColors(containerColor = DarkSurfaceVariant)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Zoom in", tint = PureWhite)
                            }
                        }

                        // Scrollable bracket area
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(16.dp))
                                .background(DarkSurface.copy(alpha = 0.35f))
                                .horizontalScroll(horizontalScroll)
                                .verticalScroll(verticalScroll),
                            contentAlignment = Alignment.Center
                        ) {
                            AfconStyleBracket(
                                uiState = uiState,
                                onMatchClick = onMatchClick,
                                matchCardHeight = scaled(matchCardHeight, zoom),
                                matchCardWidth = scaled(matchCardWidth, zoom),
                                connectorWidth = scaled(connectorWidth, zoom),
                                verticalGap = scaled(verticalGap, zoom),
                                zoom = zoom
                            )
                        }
                    }
                }
            }
        }

        // Draw dialog
        if (uiState.showDrawDialog) {
            AlertDialog(
                onDismissRequest = onDismissDrawDialog,
                title = { Text("Draw - Select Winner", fontWeight = FontWeight.Bold, color = Gold) },
                text = { Text("The match ended level. Choose who advances.", color = PureWhite) },
                confirmButton = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (uiState.drawPlayer1 != null) {
                            TextButton(
                                onClick = { onResolveDrawWinner(uiState.drawPlayer1.id) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(uiState.drawPlayer1.name, color = Gold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                        if (uiState.drawPlayer2 != null) {
                            TextButton(
                                onClick = { onResolveDrawWinner(uiState.drawPlayer2.id) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(uiState.drawPlayer2.name, color = Gold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }
                },
                containerColor = DarkSurfaceVariant
            )
        }
    }
}

private fun scaled(value: Dp, zoom: Float): Dp = (value.value * zoom).dp

// ─── AFCON-style bracket layout ─────────────────────────────────────────

@Composable
private fun AfconStyleBracket(
    uiState: BracketUiState,
    onMatchClick: (TournamentMatch) -> Unit,
    matchCardHeight: Dp,
    matchCardWidth: Dp,
    connectorWidth: Dp,
    verticalGap: Dp,
    zoom: Float
) {
    val totalRounds = uiState.totalRounds
    if (totalRounds == 0) return

    // Unit height = match card height + gap (the height of one round-1 slot)
    val unitHeight = matchCardHeight + verticalGap

    // For 1-round tournament (2 players), just show the final
    if (totalRounds == 1) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(scaled(24.dp, zoom))
        ) {
            TrophyAndTitle(zoom = zoom)
            if (uiState.isFinished) {
                ChampionDisplay(player = uiState.players[uiState.tournament?.championPlayerId], zoom = zoom)
                Spacer(modifier = Modifier.height(scaled(16.dp, zoom)))
            }
            val finalMatches = uiState.matches
                .filter { it.roundNumber == totalRounds }
                .sortedBy { it.bracketPosition }
            RoundLabel(roundNumber = totalRounds, totalRounds = totalRounds, zoom = zoom)
            finalMatches.forEach { match ->
                MatchCard(
                    match = match,
                    player1 = uiState.players[match.player1Id],
                    player2 = uiState.players[match.player2Id],
                    winner = uiState.players[match.winnerPlayerId],
                    matchNumber = getMatchNumber(match, uiState.tournament?.playerCount ?: 2),
                    width = matchCardWidth,
                    height = matchCardHeight,
                    zoom = zoom,
                    onClick = { if (match.state == "ready") onMatchClick(match) }
                )
            }
        }
        return
    }

    // Build left/right round data
    val leftRounds = mutableListOf<List<TournamentMatch>>()
    val rightRounds = mutableListOf<List<TournamentMatch>>()

    for (round in 1 until totalRounds) {
        val roundMatches = uiState.matches
            .filter { it.roundNumber == round }
            .sortedBy { it.bracketPosition }
        val half = maxOf(1, roundMatches.size / 2)
        leftRounds.add(roundMatches.take(half))
        rightRounds.add(roundMatches.drop(half))
    }

    val finalMatches = uiState.matches
        .filter { it.roundNumber == totalRounds }
        .sortedBy { it.bracketPosition }

    Row(
        modifier = Modifier.padding(vertical = scaled(16.dp, zoom)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ── Left bracket ──
        leftRounds.forEachIndexed { index, matches ->
            val roundNumber = index + 1
            val slotHeight = unitHeight * (1 shl (index))  // 2^index * unitHeight

            // Round column
            BracketRoundColumn(
                roundNumber = roundNumber,
                totalRounds = totalRounds,
                matches = matches,
                players = uiState.players,
                onMatchClick = onMatchClick,
                playerCount = uiState.tournament?.playerCount ?: 2,
                matchCardWidth = matchCardWidth,
                matchCardHeight = matchCardHeight,
                slotHeight = if (index == 0) unitHeight else slotHeight,
                zoom = zoom,
                isLeftSide = true
            )

            // Connector to next round
            val sourceSlotHeight = if (index == 0) unitHeight else slotHeight
            BracketConnector(
                sourceMatchCount = matches.size,
                sourceSlotHeight = sourceSlotHeight,
                matchCardHeight = matchCardHeight,
                connectorWidth = connectorWidth,
                isLeftSide = true,
                zoom = zoom
            )
        }

        // ── Center: Final + Trophy ──
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = scaled(8.dp, zoom))
        ) {
            TrophyAndTitle(zoom = zoom)

            if (uiState.isFinished) {
                ChampionDisplay(player = uiState.players[uiState.tournament?.championPlayerId], zoom = zoom)
                Spacer(modifier = Modifier.height(scaled(12.dp, zoom)))
            }

            RoundLabel(roundNumber = totalRounds, totalRounds = totalRounds, zoom = zoom)
            Spacer(modifier = Modifier.height(scaled(4.dp, zoom)))

            finalMatches.forEach { match ->
                MatchCard(
                    match = match,
                    player1 = uiState.players[match.player1Id],
                    player2 = uiState.players[match.player2Id],
                    winner = uiState.players[match.winnerPlayerId],
                    matchNumber = getMatchNumber(match, uiState.tournament?.playerCount ?: 2),
                    width = matchCardWidth * 1.15f,
                    height = matchCardHeight * 1.1f,
                    zoom = zoom,
                    isFinal = true,
                    onClick = { if (match.state == "ready") onMatchClick(match) }
                )
            }
        }

        // ── Right bracket (mirrored) ──
        rightRounds.reversed().forEachIndexed { reversedIndex, matches ->
            val index = rightRounds.size - 1 - reversedIndex
            val roundNumber = index + 1
            val slotHeight = unitHeight * (1 shl (index))

            // Connector from previous round (drawn before the column for right side)
            val sourceSlotHeight = if (index == 0) unitHeight else slotHeight
            BracketConnector(
                sourceMatchCount = matches.size,
                sourceSlotHeight = sourceSlotHeight,
                matchCardHeight = matchCardHeight,
                connectorWidth = connectorWidth,
                isLeftSide = false,
                zoom = zoom
            )

            // Round column
            BracketRoundColumn(
                roundNumber = roundNumber,
                totalRounds = totalRounds,
                matches = matches,
                players = uiState.players,
                onMatchClick = onMatchClick,
                playerCount = uiState.tournament?.playerCount ?: 2,
                matchCardWidth = matchCardWidth,
                matchCardHeight = matchCardHeight,
                slotHeight = if (index == 0) unitHeight else slotHeight,
                zoom = zoom,
                isLeftSide = false
            )
        }
    }
}

// ─── Round column ───────────────────────────────────────────────────────

@Composable
private fun BracketRoundColumn(
    roundNumber: Int,
    totalRounds: Int,
    matches: List<TournamentMatch>,
    players: Map<Long, Player>,
    onMatchClick: (TournamentMatch) -> Unit,
    playerCount: Int,
    matchCardWidth: Dp,
    matchCardHeight: Dp,
    slotHeight: Dp,
    zoom: Float,
    @Suppress("UNUSED_PARAMETER") isLeftSide: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        RoundLabel(roundNumber = roundNumber, totalRounds = totalRounds, zoom = zoom)
        Spacer(modifier = Modifier.height(scaled(4.dp, zoom)))

        matches.forEach { match ->
            Box(
                modifier = Modifier
                    .height(slotHeight)
                    .width(matchCardWidth),
                contentAlignment = Alignment.Center
            ) {
                MatchCard(
                    match = match,
                    player1 = players[match.player1Id],
                    player2 = players[match.player2Id],
                    winner = players[match.winnerPlayerId],
                    matchNumber = getMatchNumber(match, playerCount),
                    width = matchCardWidth,
                    height = matchCardHeight,
                    zoom = zoom,
                    onClick = { if (match.state == "ready") onMatchClick(match) }
                )
            }
        }
    }
}

// ─── Round label ────────────────────────────────────────────────────────

@Composable
private fun RoundLabel(roundNumber: Int, totalRounds: Int, zoom: Float) {
    val label = when {
        roundNumber == totalRounds -> "FINAL"
        roundNumber == totalRounds - 1 -> "SF"
        roundNumber == totalRounds - 2 -> "QF"
        else -> "R$roundNumber"
    }
    val fullLabel = when {
        roundNumber == totalRounds -> "FINAL"
        roundNumber == totalRounds - 1 -> "Semi-Final"
        roundNumber == totalRounds - 2 -> "Quarter-Final"
        roundNumber == 1 && totalRounds >= 5 -> "Round of 32"
        roundNumber == 1 && totalRounds == 4 -> "Round of 16"
        else -> "Round $roundNumber"
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = Gold,
            fontWeight = FontWeight.ExtraBold,
            fontSize = (14f * zoom).sp,
            letterSpacing = (1.5f * zoom).sp
        )
        if (label != fullLabel) {
            Text(
                text = fullLabel,
                style = MaterialTheme.typography.labelSmall,
                color = LightGray.copy(alpha = 0.7f),
                fontSize = (9f * zoom).sp
            )
        }
    }
}

// ─── Bracket connector lines ────────────────────────────────────────────

@Composable
private fun BracketConnector(
    sourceMatchCount: Int,
    sourceSlotHeight: Dp,
    @Suppress("UNUSED_PARAMETER") matchCardHeight: Dp,
    connectorWidth: Dp,
    isLeftSide: Boolean,
    zoom: Float
) {
    if (sourceMatchCount < 2) {
        // Single match → just a horizontal line
        val totalHeight = sourceSlotHeight
        Canvas(
            modifier = Modifier
                .width(connectorWidth)
                .height(totalHeight)
        ) {
            val lineColor = Gold.copy(alpha = 0.5f)
            val strokeW = (1.5f * zoom).coerceAtLeast(1f)
            val midY = size.height / 2f
            drawLine(
                color = lineColor,
                start = Offset(0f, midY),
                end = Offset(size.width, midY),
                strokeWidth = strokeW,
                cap = StrokeCap.Round
            )
        }
        return
    }

    // Total height matches the source round column height (excluding label)
    // Label takes some space, but we'll use the match area height
    val totalHeight = sourceSlotHeight * sourceMatchCount

    Canvas(
        modifier = Modifier
            .width(connectorWidth)
            .height(totalHeight)
    ) {
        val lineColor = Gold.copy(alpha = 0.45f)
        val strokeW = (1.5f * zoom).coerceAtLeast(1f)
        val slotPx = size.height / sourceMatchCount

        // Process pairs of source matches
        for (pairIndex in 0 until sourceMatchCount / 2) {
            val topIndex = pairIndex * 2
            val bottomIndex = topIndex + 1

            val topCenterY = topIndex * slotPx + slotPx / 2f
            val bottomCenterY = bottomIndex * slotPx + slotPx / 2f
            val midY = (topCenterY + bottomCenterY) / 2f

            if (isLeftSide) {
                // Left bracket: lines go left → right
                val joinX = size.width * 0.45f

                // Horizontal from top match to join
                drawLine(lineColor, Offset(0f, topCenterY), Offset(joinX, topCenterY), strokeW, cap = StrokeCap.Round)
                // Horizontal from bottom match to join
                drawLine(lineColor, Offset(0f, bottomCenterY), Offset(joinX, bottomCenterY), strokeW, cap = StrokeCap.Round)
                // Vertical connecting pair
                drawLine(lineColor, Offset(joinX, topCenterY), Offset(joinX, bottomCenterY), strokeW, cap = StrokeCap.Round)
                // Horizontal from midpoint to next round
                drawLine(lineColor, Offset(joinX, midY), Offset(size.width, midY), strokeW, cap = StrokeCap.Round)
            } else {
                // Right bracket: lines go right → left (mirrored)
                val joinX = size.width * 0.55f

                drawLine(lineColor, Offset(size.width, topCenterY), Offset(joinX, topCenterY), strokeW, cap = StrokeCap.Round)
                drawLine(lineColor, Offset(size.width, bottomCenterY), Offset(joinX, bottomCenterY), strokeW, cap = StrokeCap.Round)
                drawLine(lineColor, Offset(joinX, topCenterY), Offset(joinX, bottomCenterY), strokeW, cap = StrokeCap.Round)
                drawLine(lineColor, Offset(joinX, midY), Offset(0f, midY), strokeW, cap = StrokeCap.Round)
            }
        }
    }
}

// ─── Match card (AFCON style) ───────────────────────────────────────────

@Composable
private fun MatchCard(
    match: TournamentMatch,
    player1: Player?,
    player2: Player?,
    winner: Player?,
    matchNumber: Int,
    width: Dp,
    height: Dp,
    zoom: Float,
    isFinal: Boolean = false,
    onClick: () -> Unit
) {
    val isClickable = match.state == "ready"
    val isBye = match.state == "bye"
    val isCompleted = match.state == "completed" || isBye

    val borderColor = when {
        isFinal && isClickable -> Gold
        isClickable -> Gold.copy(alpha = 0.9f)
        isCompleted -> SuccessGreen.copy(alpha = 0.6f)
        else -> LightGray.copy(alpha = 0.15f)
    }

    val cardBg = when {
        isFinal -> DarkSurface.copy(alpha = 0.95f)
        isClickable -> DarkSurface.copy(alpha = 0.92f)
        else -> DarkSurface.copy(alpha = 0.75f)
    }

    Column(
        modifier = Modifier.width(width),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Match number label
        Text(
            text = "M$matchNumber",
            style = MaterialTheme.typography.labelSmall,
            color = LightGray.copy(alpha = 0.5f),
            fontSize = (8f * zoom).sp,
            modifier = Modifier.padding(bottom = scaled(2.dp, zoom))
        )

        Card(
            onClick = onClick,
            enabled = isClickable,
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .border(
                    width = if (isFinal) 2.dp else 1.5.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(scaled(8.dp, zoom))
                ),
            shape = RoundedCornerShape(scaled(8.dp, zoom)),
            colors = CardDefaults.cardColors(
                containerColor = cardBg,
                disabledContainerColor = cardBg
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = if (isClickable) 6.dp else 2.dp,
                focusedElevation = 12.dp
            )
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Player 1 row
                PlayerBar(
                    player = player1,
                    isWinner = winner?.id == player1?.id && isCompleted,
                    isBye = isBye,
                    accentColor = Gold,
                    zoom = zoom,
                    modifier = Modifier.weight(1f)
                )

                // Separator
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((0.8f).dp)
                        .background(LightGray.copy(alpha = 0.15f))
                )

                // Player 2 row
                PlayerBar(
                    player = player2,
                    isWinner = winner?.id == player2?.id && isCompleted,
                    isBye = false,
                    accentColor = GoldDark,
                    zoom = zoom,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // "Tap to play" indicator
        if (isClickable) {
            Text(
                text = "TAP TO PLAY",
                style = MaterialTheme.typography.labelSmall,
                color = Gold.copy(alpha = 0.8f),
                fontWeight = FontWeight.Bold,
                fontSize = (8f * zoom).sp,
                letterSpacing = (0.8f * zoom).sp,
                modifier = Modifier.padding(top = scaled(2.dp, zoom))
            )
        }
    }
}

// ─── Player bar (single row within match card) ──────────────────────────

@Composable
private fun PlayerBar(
    player: Player?,
    isWinner: Boolean,
    isBye: Boolean,
    accentColor: Color,
    zoom: Float,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isWinner) Modifier.background(SuccessGreen.copy(alpha = 0.08f))
                else Modifier
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Colored accent bar on left
        Box(
            modifier = Modifier
                .width(scaled(4.dp, zoom))
                .fillMaxHeight()
                .background(
                    when {
                        isWinner -> SuccessGreen
                        player != null -> accentColor.copy(alpha = 0.6f)
                        else -> LightGray.copy(alpha = 0.2f)
                    }
                )
        )

        // Player name
        Text(
            text = when {
                player != null -> player.name
                isBye -> "BYE"
                else -> "TBD"
            },
            style = MaterialTheme.typography.bodySmall,
            color = when {
                isWinner -> Gold
                player != null -> PureWhite
                else -> LightGray.copy(alpha = 0.45f)
            },
            fontWeight = if (isWinner) FontWeight.ExtraBold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = (11f * zoom).sp,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = scaled(6.dp, zoom))
        )

        // Winner indicator
        if (isWinner) {
            Text(
                text = "W",
                color = SuccessGreen,
                fontWeight = FontWeight.ExtraBold,
                fontSize = (10f * zoom).sp,
                modifier = Modifier.padding(end = scaled(6.dp, zoom))
            )
        }
    }
}

// ─── Trophy and title (center of bracket) ───────────────────────────────

@Composable
private fun TrophyAndTitle(zoom: Float) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(bottom = scaled(12.dp, zoom))
    ) {
        Icon(
            imageVector = Icons.Default.EmojiEvents,
            contentDescription = "Trophy",
            tint = Gold,
            modifier = Modifier.size(scaled(48.dp, zoom))
        )
        Spacer(modifier = Modifier.height(scaled(4.dp, zoom)))
    }
}

// ─── Champion display ───────────────────────────────────────────────────

@Composable
private fun ChampionDisplay(player: Player?, zoom: Float) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "CHAMPION",
            fontSize = (24f * zoom).sp,
            fontWeight = FontWeight.Black,
            color = Gold,
            letterSpacing = (3f * zoom).sp
        )
        Spacer(modifier = Modifier.height(scaled(6.dp, zoom)))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(scaled(30.dp, zoom)))
                .background(BurgundyDark.copy(alpha = 0.95f))
                .border(1.5.dp, Gold.copy(alpha = 0.5f), RoundedCornerShape(scaled(30.dp, zoom)))
                .padding(end = scaled(16.dp, zoom)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlayerAvatar(imageUri = player?.imageUri, size = scaled(44.dp, zoom))
            Spacer(modifier = Modifier.width(scaled(8.dp, zoom)))
            Text(
                text = player?.name ?: "TBD",
                color = PureWhite,
                fontWeight = FontWeight.Bold,
                fontSize = (14f * zoom).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ─── Utilities ──────────────────────────────────────────────────────────

private fun getMatchNumber(match: TournamentMatch, totalPlayerCount: Int): Int {
    var number = 0
    for (r in 1 until match.roundNumber) {
        number += totalPlayerCount / (1 shl r)
    }
    number += match.bracketPosition + 1
    return number
}
