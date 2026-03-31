package com.elocho.snooker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SportsBar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.elocho.snooker.ui.theme.LightGray

@Composable
fun EmptyState(
    message: String,
    modifier: Modifier = Modifier,
    actionButton: @Composable (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.SportsBar,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = LightGray.copy(alpha = 0.35f)
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = LightGray.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )
        if (actionButton != null) {
            Spacer(modifier = Modifier.height(24.dp))
            actionButton()
        }
    }
}
