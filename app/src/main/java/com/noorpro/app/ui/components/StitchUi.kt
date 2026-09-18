package com.noorpro.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val StitchEmerald = Color(0xFF064E3B)
val StitchEmeraldDeep = Color(0xFF003527)
val StitchGold = Color(0xFFD4AF37)
val StitchCream = Color(0xFFFCFAFA)
val StitchCardWhite = Color(0xFFFFFFFF)
val StitchInk = Color(0xFF0D1C2F)
val StitchMuted = Color(0xFF6B7A74)
val StitchLine = Color(0xFFE3EAE5)
val StitchSoft = Color(0xFFEAF3EF)
val StitchDarkBackground = Color(0xFF07110F)
val StitchDarkSurface = Color(0xFF101B18)
val StitchDarkSoft = Color(0xFF17241F)
val StitchDarkInk = Color(0xFFFAF4E8)
val StitchDarkMuted = Color(0xFFC6BCAB)
val StitchDarkLine = Color(0xFF2B3C35)
val StitchDarkIconSoft = Color(0xFF19352D)

@Composable
fun isStitchLight(): Boolean = MaterialTheme.colorScheme.background.luminance() > 0.5f

@Composable
fun stitchBackground(): Color = if (isStitchLight()) StitchCream else StitchDarkBackground

@Composable
fun stitchSurface(): Color = if (isStitchLight()) StitchCardWhite else StitchDarkSurface

@Composable
fun stitchSoftSurface(): Color = if (isStitchLight()) StitchSoft else StitchDarkSoft

@Composable
fun stitchText(): Color = if (isStitchLight()) StitchInk else StitchDarkInk

@Composable
fun stitchMutedText(): Color = if (isStitchLight()) StitchMuted else StitchDarkMuted

@Composable
fun stitchPrimary(): Color = if (isStitchLight()) StitchEmerald else MaterialTheme.colorScheme.primary

@Composable
fun StitchScreen(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val light = isStitchLight()
    val backgroundColor = stitchBackground()
    val pattern = if (light) StitchEmerald.copy(alpha = 0.035f) else StitchGold.copy(alpha = 0.025f)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .drawBehind {
                val step = 96.dp.toPx()
                val radius = 28.dp.toPx()
                var x = -step / 2
                while (x < size.width + step) {
                    var y = -step / 2
                    while (y < size.height + step) {
                        drawCircle(
                            color = pattern,
                            radius = radius,
                            center = Offset(x, y),
                            style = Stroke(width = 1.dp.toPx())
                        )
                        drawLine(pattern, Offset(x - radius, y), Offset(x + radius, y), 1.dp.toPx())
                        drawLine(pattern, Offset(x, y - radius), Offset(x, y + radius), 1.dp.toPx())
                        y += step
                    }
                    x += step
                }
            }
    ) {
        content()
    }
}

@Composable
fun StitchEyebrow(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text.uppercase(),
        color = StitchGold,
        fontSize = 12.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 3.sp,
        modifier = modifier
    )
}

@Composable
fun StitchHeadline(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: Int = 36
) {
    Text(
        text = text,
        color = stitchText(),
        fontSize = fontSize.sp,
        lineHeight = (fontSize + 6).sp,
        fontWeight = FontWeight.Black,
        fontFamily = com.noorpro.app.ui.theme.LibreCaslon,
        modifier = modifier
    )
}

@Composable
fun StitchCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(24.dp),
    borderColor: Color? = null,
    content: @Composable () -> Unit
) {
    val resolvedBorder = borderColor ?: if (isStitchLight()) StitchLine else StitchDarkLine
    Card(
        modifier = modifier,
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = stitchSurface()),
        border = BorderStroke(1.dp, resolvedBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isStitchLight()) 5.dp else 2.dp)
    ) {
        content()
    }
}

@Composable
fun StitchIconBubble(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tint: Color? = null,
    background: Color? = null
) {
    val resolvedTint = tint ?: stitchPrimary()
    val resolvedBackground = background ?: if (isStitchLight()) StitchSoft else StitchDarkIconSoft
    Box(
        modifier = modifier
            .size(54.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(resolvedBackground)
            .border(1.dp, resolvedTint.copy(alpha = 0.16f), RoundedCornerShape(18.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = resolvedTint, modifier = Modifier.size(26.dp))
    }
}

@Composable
fun StitchRoundIcon(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    selected: Boolean = false
) {
    Box(
        modifier = modifier
            .size(if (selected) 58.dp else 48.dp)
            .clip(CircleShape)
            .background(if (selected) stitchPrimary() else stitchSurface())
            .border(1.dp, if (selected) stitchPrimary() else if (isStitchLight()) StitchLine else StitchDarkLine, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) Color.White else stitchPrimary(),
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun StitchFeatureTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    wide: Boolean = false,
    onClick: () -> Unit
) {
    StitchCard(
        modifier = modifier
            .height(if (wide) 132.dp else 156.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(22.dp),
            horizontalAlignment = if (wide) Alignment.Start else Alignment.Start
        ) {
            StitchIconBubble(icon = icon)
            Spacer(modifier = Modifier.weight(1f))
            Text(title, color = stitchText(), fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
            Text(subtitle, color = stitchMutedText(), fontSize = 13.sp, maxLines = 1)
        }
    }
}

@Composable
fun StitchSectionLabel(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title.uppercase(),
        color = stitchMutedText(),
        fontSize = 12.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 3.sp,
        modifier = modifier
    )
}

@Composable
fun StitchListRow(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    iconTint: Color? = null,
    titleColor: Color? = null,
    trailing: @Composable (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StitchIconBubble(icon = icon, modifier = Modifier.size(44.dp), tint = iconTint)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = titleColor ?: stitchText(),
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    color = stitchMutedText(),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (trailing != null) {
            trailing()
        } else {
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = stitchMutedText())
        }
    }
}

@Composable
fun StitchPill(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = if (selected) stitchPrimary() else stitchSurface(),
        border = BorderStroke(1.dp, if (selected) stitchPrimary() else if (isStitchLight()) StitchLine else StitchDarkLine)
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else stitchText(),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
        )
    }
}
