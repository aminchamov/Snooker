package com.elocho.snooker.ui.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elocho.snooker.ui.components.AppLogo
import com.elocho.snooker.ui.components.DiagonalStripeBackground
import com.elocho.snooker.ui.theme.Burgundy
import com.elocho.snooker.ui.theme.DarkSurface
import com.elocho.snooker.ui.theme.Gold
import com.elocho.snooker.ui.theme.LightGray
import com.elocho.snooker.ui.theme.MediumGray
import com.elocho.snooker.ui.theme.PureWhite

@Composable
fun StatisticsScreen(
    uiState: StatisticsUiState,
    onNavigateBack: () -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth >= 900.dp
        val horizontalPadding = if (isWide) 24.dp else 16.dp

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
                        text = "STATISTICS",
                        style = MaterialTheme.typography.headlineSmall,
                        color = PureWhite,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = "Ranking and performance overview",
                        style = MaterialTheme.typography.bodySmall,
                        color = LightGray
                    )
                }
                AppLogo(height = 28.dp)
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Loading statistics...",
                        color = Gold,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                return@Column
            }

            if (uiState.errorMsg != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = uiState.errorMsg,
                        color = Gold,
                        modifier = Modifier.padding(20.dp),
                        textAlign = TextAlign.Center
                    )
                }
                return@Column
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = horizontalPadding),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    SummaryRow(uiState = uiState, isWide = isWide)
                }

                item {
                    if (uiState.rankedPlayers.isEmpty()) {
                        EmptyStateCard()
                    } else {
                        RankedStatsTable(stats = uiState.rankedPlayers, minWidth = if (isWide) 1020.dp else 900.dp)
                    }
                }

                item { Spacer(modifier = Modifier.height(12.dp)) }
            }
        }
    }
}

@Composable
private fun SummaryRow(uiState: StatisticsUiState, isWide: Boolean) {
    if (isWide) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SummaryCard("Players", uiState.totalPlayers.toString(), Modifier.weight(1f))
            SummaryCard("Matches", uiState.totalCompletedMatches.toString(), Modifier.weight(1f))
            SummaryCard("Tournaments", uiState.totalCompletedTournaments.toString(), Modifier.weight(1f))
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SummaryCard("Players", uiState.totalPlayers.toString(), Modifier.fillMaxWidth())
            SummaryCard("Matches", uiState.totalCompletedMatches.toString(), Modifier.fillMaxWidth())
            SummaryCard("Tournaments", uiState.totalCompletedTournaments.toString(), Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun SummaryCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface.copy(alpha = 0.9f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, color = Gold, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp)
            Text(text = title, color = LightGray, fontSize = 12.sp)
        }
    }
}

@Composable
private fun EmptyStateCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface.copy(alpha = 0.9f))
    ) {
        Text(
            text = "No player statistics yet. Complete some matches to populate rankings.",
            color = LightGray,
            modifier = Modifier.padding(18.dp)
        )
    }
}

@Composable
private fun RankedStatsTable(stats: List<RankedPlayerStat>, minWidth: Dp) {
    val horizontalScroll = rememberScrollState()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface.copy(alpha = 0.9f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = "RANKED PLAYER STATISTICS",
                color = Gold,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
            )

            Row(
                modifier = Modifier
                    .horizontalScroll(horizontalScroll)
                    .fillMaxWidth()
            ) {
                Column(modifier = Modifier.width(minWidth)) {
                    TableHeader()
                    stats.forEachIndexed { index, item ->
                        TableRow(item = item, striped = index % 2 == 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun TableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Burgundy.copy(alpha = 0.75f), RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HeaderCell("Rank", 56.dp)
        HeaderCell("Player", 220.dp, align = TextAlign.Start)
        HeaderCell("Played", 72.dp)
        HeaderCell("Wins", 56.dp)
        HeaderCell("Draws", 56.dp)
        HeaderCell("Losses", 64.dp)
        HeaderCell("Max Break", 96.dp)
        HeaderCell("Avg Pts", 92.dp)
        HeaderCell("Tourn. Won", 96.dp)
    }
}

@Composable
private fun TableRow(item: RankedPlayerStat, striped: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (striped) MediumGray.copy(alpha = 0.2f) else MediumGray.copy(alpha = 0.1f),
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ValueCell("#${item.rank}", 56.dp)
        ValueCell(item.playerName, 220.dp, align = TextAlign.Start)
        ValueCell(item.gamesPlayed.toString(), 72.dp)
        ValueCell(item.wins.toString(), 56.dp)
        ValueCell(item.draws.toString(), 56.dp)
        ValueCell(item.losses.toString(), 64.dp)
        ValueCell(if (item.maxBreak > 0) item.maxBreak.toString() else "-", 96.dp)
        ValueCell(String.format("%.1f", item.averagePointsPerMatch), 92.dp)
        ValueCell(item.tournamentsWon.toString(), 96.dp)
    }
}

@Composable
private fun HeaderCell(text: String, width: Dp, align: TextAlign = TextAlign.Center) {
    Text(
        text = text,
        modifier = Modifier.width(width),
        color = PureWhite,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        textAlign = align,
        maxLines = 1
    )
}

@Composable
private fun ValueCell(text: String, width: Dp, align: TextAlign = TextAlign.Center) {
    Text(
        text = text,
        modifier = Modifier.width(width),
        color = PureWhite,
        fontSize = 12.sp,
        textAlign = align,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}
