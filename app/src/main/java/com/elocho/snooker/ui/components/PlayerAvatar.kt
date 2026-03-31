package com.elocho.snooker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.elocho.snooker.ui.theme.BurgundyDark
import com.elocho.snooker.ui.theme.Gold
import com.elocho.snooker.ui.theme.LightGray

@Composable
fun PlayerAvatar(
    imageUri: String?,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    borderColor: Color = Gold,
    borderWidth: Dp = 2.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .border(borderWidth, borderColor, CircleShape)
            .background(BurgundyDark, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (imageUri != null) {
            AsyncImage(
                model = imageUri,
                contentDescription = "Player avatar",
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Default avatar",
                modifier = Modifier.size(size * 0.5f),
                tint = LightGray.copy(alpha = 0.7f)
            )
        }
    }
}
