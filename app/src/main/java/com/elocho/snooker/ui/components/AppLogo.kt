package com.elocho.snooker.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.elocho.snooker.R

@Composable
fun AppLogo(
    modifier: Modifier = Modifier,
    height: Dp = 48.dp
) {
    val widthDp = (height.value * 648f / 203f).dp
    Image(
        painter = painterResource(id = R.drawable.elocho_logo),
        contentDescription = "El Ocho Logo",
        modifier = modifier
            .heightIn(max = height)
            .widthIn(max = widthDp),
        contentScale = ContentScale.Fit
    )
}
