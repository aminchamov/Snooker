package com.elocho.snooker.ui.match

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elocho.snooker.ui.components.AppLogo
import com.elocho.snooker.ui.components.DiagonalStripeBackground
import com.elocho.snooker.ui.components.ElOchoButton
import com.elocho.snooker.ui.components.PlayerAvatar
import com.elocho.snooker.ui.theme.Burgundy
import com.elocho.snooker.ui.theme.BurgundyDark
import com.elocho.snooker.ui.theme.DarkSurface
import com.elocho.snooker.ui.theme.Gold
import com.elocho.snooker.ui.theme.GoldLight
import com.elocho.snooker.ui.theme.LightGray
import com.elocho.snooker.ui.theme.PureWhite

@Composable
fun ResultScreen(
    uiState: ScoreboardUiState,
    onDone: () -> Unit
) {
    val titleScale = remember { Animatable(0.92f) }

    LaunchedEffect(Unit) {
        titleScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isLandscape = maxWidth > maxHeight
        val horizontalPadding = if (maxWidth > 1100.dp) 32.dp else 20.dp

        DiagonalStripeBackground()

        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = horizontalPadding, vertical = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    AppLogo(height = 36.dp)
                    ResultAnnouncement(uiState = uiState, scale = titleScale.value)
                    WinnerBanner(uiState = uiState)
                }

                Column(
                    modifier = Modifier.weight(1.1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ScoreSummaryCard(uiState = uiState)
                    ElOchoButton(
                        text = "DONE",
                        onClick = onDone,
                        modifier = Modifier.fillMaxWidth(0.72f),
                        containerColor = Burgundy
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = horizontalPadding, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                AppLogo(height = 36.dp)
                ResultAnnouncement(uiState = uiState, scale = titleScale.value)
                WinnerBanner(uiState = uiState)
                ScoreSummaryCard(uiState = uiState)
                ElOchoButton(text = "DONE", onClick = onDone, containerColor = Burgundy)
            }
        }
    }
}

@Composable
private fun ResultAnnouncement(uiState: ScoreboardUiState, scale: Float) {
    Column(
        modifier = Modifier.scale(scale),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (uiState.isDraw) "DRAW" else "WINNER",
            color = if (uiState.isDraw) Gold else PureWhite,
            fontWeight = FontWeight.Black,
            fontSize = 50.sp,
            letterSpacing = 4.sp
        )
        Text(
            text = if (uiState.isDraw) "Match ended level" else "Congratulations",
            color = GoldLight,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun WinnerBanner(uiState: ScoreboardUiState) {
    if (uiState.isDraw || uiState.winnerPlayer == null) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface.copy(alpha = 0.9f))
        ) {
            Text(
                text = "Both players finished with the same score",
                color = LightGray,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)
            )
        }
        return
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = BurgundyDark.copy(alpha = 0.92f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlayerAvatar(imageUri = uiState.winnerPlayer.imageUri, size = 64.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Champion",
                    color = Gold,
                    style = MaterialTheme.typography.labelMedium,
                    letterSpacing = 1.sp
                )
                Text(
                    text = uiState.winnerPlayer.name,
                    color = PureWhite,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ScoreSummaryCard(uiState: ScoreboardUiState) {
    val hours = uiState.elapsedSeconds / 3600
    val minutes = (uiState.elapsedSeconds % 3600) / 60
    val seconds = uiState.elapsedSeconds % 60

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface.copy(alpha = 0.92f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlayerSideScore(
                    name = uiState.player1?.name ?: "Player 1",
                    imageUri = uiState.player1?.imageUri,
                    score = uiState.player1Score,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "-",
                    color = Gold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 30.sp,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                PlayerSideScore(
                    name = uiState.player2?.name ?: "Player 2",
                    imageUri = uiState.player2?.imageUri,
                    score = uiState.player2Score,
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = "Duration ${String.format("%02d:%02d:%02d", hours, minutes, seconds)}",
                color = LightGray,
                style = MaterialTheme.typography.bodySmall
            )

            BreakSummary(uiState = uiState)
        }
    }
}

@Composable
private fun PlayerSideScore(
    name: String,
    imageUri: String?,
    score: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        PlayerAvatar(imageUri = imageUri, size = 48.dp)
        Text(
            text = name,
            color = PureWhite,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = score.toString(),
            color = Gold,
            fontSize = 30.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
private fun BreakSummary(uiState: ScoreboardUiState) {
    fun playerBreakCount(playerNumber: Int): Int = uiState.breakHistory.count { it.playerNumber == playerNumber }
    fun playerBreakAvg(playerNumber: Int): Int {
        val values = uiState.breakHistory.filter { it.playerNumber == playerNumber }.map { it.points }
        return if (values.isEmpty()) 0 else values.sum() / values.size
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = BurgundyDark.copy(alpha = 0.46f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "MATCH BREAK SUMMARY",
                color = Gold,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = 0.8.sp
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                PlayerBreakColumn(
                    playerName = uiState.player1?.name ?: "Player 1",
                    highestBreak = uiState.highestBreakPlayer1,
                    breakCount = playerBreakCount(1),
                    averageBreak = playerBreakAvg(1),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(10.dp))
                PlayerBreakColumn(
                    playerName = uiState.player2?.name ?: "Player 2",
                    highestBreak = uiState.highestBreakPlayer2,
                    breakCount = playerBreakCount(2),
                    averageBreak = playerBreakAvg(2),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun PlayerBreakColumn(
    playerName: String,
    highestBreak: Int,
    breakCount: Int,
    averageBreak: Int,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = playerName,
            color = PureWhite,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(text = "Highest $highestBreak", color = GoldLight, fontSize = 12.sp)
        Text(text = "Breaks $breakCount", color = LightGray, fontSize = 12.sp)
        Text(text = "Avg $averageBreak", color = LightGray, fontSize = 12.sp)
    }
}
