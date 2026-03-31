package com.elocho.snooker.ui.menu

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elocho.snooker.ui.components.AppLogo
import com.elocho.snooker.ui.components.DiagonalStripeBackground
import com.elocho.snooker.ui.theme.*

@Composable
fun MainMenuScreen(
    onQuickMatchClick: () -> Unit,
    onTournamentsClick: () -> Unit,
    onPlayersClick: () -> Unit,
    onStatisticsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSyncClick: () -> Unit,
    isSyncing: Boolean,
    syncMessage: String?,
    lastSyncedAt: Long?,
    lastSyncStatus: String?,
    lastSyncError: String?,
    onLogoutClick: () -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        DiagonalStripeBackground()

        val isLandscape = maxWidth > maxHeight
        val isWideScreen = maxWidth > 800.dp

        val menuItems = listOf(
            MenuItemData("QUICK MATCH", Icons.Default.SportsMma, BurgundyLight) { onQuickMatchClick() },
            MenuItemData("TOURNAMENTS", Icons.Default.EmojiEvents, Gold) { onTournamentsClick() },
            MenuItemData("PLAYERS", Icons.Default.People, BurgundyVeryLight) { onPlayersClick() },
            MenuItemData(if (isSyncing) "SYNCING..." else "SYNC DATA", Icons.Default.Sync, GoldLight) { onSyncClick() },
            MenuItemData("SETTINGS", Icons.Default.Settings, LightGray) { onSettingsClick() }
        )
        val syncStatusText = buildSyncStatusText(lastSyncedAt, lastSyncStatus, lastSyncError, syncMessage)

        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(0.38f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    AppLogo(height = if (isWideScreen) 60.dp else 50.dp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "SNOOKER LOUNGE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        letterSpacing = 6.sp,
                        color = Gold.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(36.dp))
                    Text(
                        text = syncStatusText,
                        fontSize = 12.sp,
                        color = LightGray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = onLogoutClick) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, null, tint = LightGray.copy(alpha = 0.7f))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("LOGOUT", color = LightGray.copy(alpha = 0.7f), letterSpacing = 2.sp, fontSize = 13.sp)
                    }
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .weight(0.62f)
                        .fillMaxHeight(),
                    contentPadding = PaddingValues(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(menuItems) { item ->
                        MenuCard(item = item, modifier = Modifier.fillMaxWidth().height(76.dp), iconSize = 26.dp, textSize = 15.sp)
                    }
                }
            }

        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AppLogo(height = 60.dp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "SNOOKER LOUNGE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        letterSpacing = 6.sp,
                        color = Gold.copy(alpha = 0.85f)
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 480.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    menuItems.forEach { item ->
                        MenuCard(item = item, modifier = Modifier.fillMaxWidth().height(72.dp))
                    }
                }

                Text(
                    text = syncStatusText,
                    color = LightGray,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )

                TextButton(
                    onClick = onLogoutClick,
                    modifier = Modifier.defaultMinSize(minHeight = 44.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = null,
                        tint = LightGray.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "LOGOUT",
                        color = LightGray.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Normal,
                        letterSpacing = 2.sp,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

private fun buildSyncStatusText(
    lastSyncedAt: Long?,
    lastSyncStatus: String?,
    lastSyncError: String?,
    transientMessage: String?
): String {
    if (!transientMessage.isNullOrBlank()) return transientMessage
    if (lastSyncStatus == "failed") return lastSyncError ?: "Last sync failed"
    if (lastSyncedAt == null) return "Not synced yet"
    val time = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
        .format(java.util.Date(lastSyncedAt))
    return "Last synced: $time"
}

private data class MenuItemData(
    val title: String,
    val icon: ImageVector,
    val accentColor: Color,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MenuCard(
    item: MenuItemData,
    modifier: Modifier = Modifier,
    iconSize: androidx.compose.ui.unit.Dp = 28.dp,
    textSize: androidx.compose.ui.unit.TextUnit = 16.sp
) {
    Card(
        onClick = item.onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurface.copy(alpha = 0.82f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp,
            focusedElevation = 12.dp,
            pressedElevation = 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                modifier = Modifier.size(iconSize),
                tint = item.accentColor
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = item.title,
                fontSize = textSize,
                fontWeight = FontWeight.SemiBold,
                color = PureWhite,
                letterSpacing = 1.5.sp
            )
        }
    }
}
