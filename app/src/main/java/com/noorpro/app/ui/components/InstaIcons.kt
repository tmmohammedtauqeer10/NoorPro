package com.noorpro.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** 1.0.78: Instagram-style outline / filled action icons (line art adapted from Feather, MIT). */
object InstaIcons {
    private fun build(name: String, vararg paths: String, filled: Boolean, stroke: Float = 1.9f): ImageVector {
        val b = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
        paths.forEach { d ->
            val nodes = PathParser().parsePathString(d).toNodes()
            if (filled) b.addPath(nodes, fill = SolidColor(Color.Black))
            else b.addPath(nodes, stroke = SolidColor(Color.Black), strokeLineWidth = stroke, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        }
        return b.build()
    }
    private const val HEART = "M20.84,4.61a5.5,5.5 0 0,0 -7.78,0L12,5.67l-1.06,-1.06a5.5,5.5 0 0,0 -7.78,7.78l1.06,1.06L12,21.23l7.78,-7.78 1.06,-1.06a5.5,5.5 0 0,0 0,-7.78z"
    private const val COMMENT = "M20.66,16.14A9,9 0 1,0 17.6,19.6L21,21z"
    private const val SEND_A = "M22,3L9.2,10.1"
    private const val SEND_B = "M22,3L15,21.5 11,13 2.5,9.6z"
    private const val BOOKMARK = "M19,21l-7,-5 -7,5V5a2,2 0 0,1 2,-2h10a2,2 0 0,1 2,2z"

    val Heart: ImageVector by lazy { build("InstaHeart", HEART, filled = false) }
    val HeartFilled: ImageVector by lazy { build("InstaHeartFilled", HEART, filled = true) }
    val Comment: ImageVector by lazy { build("InstaComment", COMMENT, filled = false) }
    val Send: ImageVector by lazy { build("InstaSend", SEND_A, SEND_B, filled = false) }
    val Bookmark: ImageVector by lazy { build("InstaBookmark", BOOKMARK, filled = false) }
    val BookmarkFilled: ImageVector by lazy { build("InstaBookmarkFilled", BOOKMARK, filled = true) }
}

val InstaLikeRed = Color(0xFFFF3040)

/** An icon with a soft dark drop shadow so white glyphs stay readable on bright video. */
@Composable
fun ShadowedIcon(icon: ImageVector, contentDescription: String?, tint: Color, size: Dp, modifier: Modifier = Modifier) {
    Box(modifier) {
        Icon(icon, null, tint = Color.Black.copy(alpha = 0.30f), modifier = Modifier.size(size).offset(0.6.dp, 1.2.dp))
        Icon(icon, null, tint = Color.Black.copy(alpha = 0.16f), modifier = Modifier.size(size).offset(0.dp, 2.dp))
        Icon(icon, contentDescription, tint = tint, modifier = Modifier.size(size))
    }
}
