package com.noorpro.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noorpro.app.ui.components.stitchLine
import com.noorpro.app.ui.components.stitchMutedText
import com.noorpro.app.ui.components.stitchText
import com.noorpro.app.ui.islamic.IkIconTile
import com.noorpro.app.ui.islamic.ikCard
import com.noorpro.app.ui.theme.LibreCaslon

/** 1.2.0 (115): Hadith collections and the daily quiz, side by side, on Home and in the Library. */
@Composable
fun LearnShortcutRow(onHadith: () -> Unit, onQuiz: () -> Unit, modifier: Modifier = Modifier, tagPrefix: String = "learn") {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        LearnTile(Icons.AutoMirrored.Outlined.LibraryBooks, "Hadith", "Bukhari, Muslim & 8 more · offline", Modifier.weight(1f).testTag("${tagPrefix}_hadith"), onHadith)
        LearnTile(Icons.Outlined.Quiz, "Daily quiz", "10 questions · sources after each", Modifier.weight(1f).testTag("${tagPrefix}_quiz"), onQuiz)
    }
}

@Composable
private fun LearnTile(icon: ImageVector, title: String, sub: String, modifier: Modifier, onClick: () -> Unit) {
    Row(modifier.clip(RoundedCornerShape(20.dp)).background(ikCard()).border(1.dp, stitchLine(), RoundedCornerShape(20.dp))
        .clickable(onClick = onClick).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        IkIconTile(icon, size = 40.dp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = stitchText(), fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = LibreCaslon, maxLines = 1)
            Text(sub, color = stitchMutedText(), fontSize = 11.sp, lineHeight = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}
