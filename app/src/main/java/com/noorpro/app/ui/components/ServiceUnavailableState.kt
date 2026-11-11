package com.noorpro.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noorpro.app.ui.theme.LibreCaslon

/** Friendly empty/error state when an online backend is down (billing, network, CF). */
@Composable
fun ServiceUnavailableState(
    title: String = "Service unavailable",
    body: String = "This online feature could not be reached right now. Please try again in a little while.",
    actionLabel: String = "Try again",
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, color = stitchPrimary(), fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = LibreCaslon, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(body, color = stitchMutedText(), fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 20.sp)
        if (onRetry != null) {
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRetry, shape = RoundedCornerShape(14.dp)) { Text(actionLabel) }
        }
    }
}
