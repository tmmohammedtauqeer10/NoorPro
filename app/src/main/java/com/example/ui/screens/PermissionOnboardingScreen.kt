package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MatteGold
import com.example.ui.viewmodel.ThemeMode

@Composable
fun PermissionOnboardingScreen(
    selectedTheme: ThemeMode,
    onThemeSelected: (ThemeMode) -> Unit,
    onEnableAndContinue: () -> Unit,
    onSkip: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(scheme.background, scheme.surfaceVariant)))
            .padding(horizontal = 22.dp)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(scheme.primary.copy(alpha = 0.08f), size.width * 0.58f, Offset(size.width * .82f, size.height * .12f))
            drawCircle(MatteGold.copy(alpha = 0.08f), size.width * 0.42f, Offset(size.width * .08f, size.height * .78f))
        }
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(22.dp))
            NoorMark()
            Spacer(Modifier.height(16.dp))
            Text("Welcome to Noor Pro", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
            Text(
                "Your beautiful daily companion for prayer, Quran, remembrance, and growth.",
                color = scheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
            )
            Spacer(Modifier.height(12.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = scheme.surface.copy(alpha = 0.94f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, scheme.outlineVariant),
                shape = RoundedCornerShape(28.dp)
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                    Text("Choose your look", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ThemeChoice("Light", Icons.Default.LightMode, selectedTheme == ThemeMode.LIGHT, Modifier.weight(1f)) { onThemeSelected(ThemeMode.LIGHT) }
                        ThemeChoice("Dark", Icons.Default.DarkMode, selectedTheme == ThemeMode.DARK, Modifier.weight(1f)) { onThemeSelected(ThemeMode.DARK) }
                        ThemeChoice("Auto", Icons.Default.BrightnessAuto, selectedTheme == ThemeMode.SYSTEM, Modifier.weight(1f)) { onThemeSelected(ThemeMode.SYSTEM) }
                    }
                    HorizontalDivider(color = scheme.outlineVariant)
                    Text("Helpful permissions", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    PermissionRow(Icons.Default.LocationOn, "Location", "Accurate prayer times and Qibla")
                    PermissionRow(Icons.Default.NotificationsActive, "Notifications", "Prayer and daily reminder alerts")
                    Text(
                        "You stay in control. Permissions can be changed later in Settings.",
                        color = scheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = onEnableAndContinue,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(Icons.Default.AutoAwesome, null)
                Spacer(Modifier.width(10.dp))
                Text("Enable & Continue", fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = onSkip) { Text("Continue without permissions", color = scheme.onSurfaceVariant) }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun NoorMark() {
    Box(
        Modifier.size(104.dp).clip(RoundedCornerShape(30.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF0A1930), Color(0xFF174B4A))))
            .border(1.dp, MatteGold.copy(alpha = .6f), RoundedCornerShape(30.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.NightsStay, null, tint = MatteGold, modifier = Modifier.size(58.dp).offset(y = (-7).dp))
        Icon(Icons.Default.MenuBook, null, tint = Color(0xFFFFF8E7), modifier = Modifier.size(48.dp).offset(y = 24.dp))
    }
}

@Composable
private fun ThemeChoice(label: String, icon: ImageVector, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier.clip(RoundedCornerShape(16.dp))
            .background(if (selected) scheme.primaryContainer else scheme.surfaceVariant)
            .border(1.dp, if (selected) scheme.primary else scheme.outlineVariant, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick).padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = if (selected) scheme.primary else scheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PermissionRow(icon: ImageVector, title: String, description: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(42.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(21.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, fontWeight = FontWeight.Bold)
            Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
    }
}
