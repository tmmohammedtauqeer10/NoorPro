package com.noorpro.app.ui.screens

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.provider.MediaStore
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noorpro.app.data.AiFeatureService
import com.noorpro.app.data.NoorImageResult
import com.noorpro.app.ui.theme.MatteGold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Image Studio styles (must match the Worker's STYLES keys). */
internal val NOOR_IMAGE_STYLES = listOf(
    "calligraphy" to "Calligraphy art",
    "mosque" to "Mosque",
    "nature" to "Nature",
    "geometric" to "Geometric",
    "reel" to "Reel background",
    "card" to "Share card",
)

internal val NOOR_IMAGE_IDEAS = listOf(
    "Golden calligraphy-style art on deep green, Ramadan night",
    "Masjid an-Nabawi green dome at dawn",
    "Peaceful mountain lake at Fajr with soft mist",
    "Blue and gold Islamic geometric star pattern",
    "Lanterns and crescent moon for an Eid card",
)

private val StudioGreen = Color(0xFF1F7A5A)

/**
 * 1.2.0 (114) Noor AI Image Studio (free): Islamic art, calligraphy-style posters, mosque and nature backgrounds,
 * reel and share-card backgrounds. No people, faces, animals, prophets, Sahabah, Ahl al-Bayt or awliya, and nothing
 * immodest or non-Islamic: the server refuses those requests and checks each image before returning it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NoorImageStudioSheet(onDismiss: () -> Unit, language: String = AiFeatureService.answerLanguage) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var prompt by remember { mutableStateOf("") }
    var style by remember { mutableStateOf("calligraphy") }
    var busy by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<NoorImageResult?>(null) }
    val bitmap = remember(result) { (result as? NoorImageResult.Image)?.let { runCatching { BitmapFactory.decodeByteArray(it.bytes, 0, it.bytes.size) }.getOrNull() } }

    fun generate() {
        if (busy) return
        busy = true; result = null
        scope.launch {
            result = AiFeatureService.generateImage(prompt, style, language)
            busy = false
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp).padding(bottom = 28.dp).testTag("noorai_image_studio")) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, null, tint = MatteGold)
                Spacer(Modifier.width(8.dp))
                Text("Image Studio", fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = com.noorpro.app.ui.theme.LibreCaslon)
            }
            Text("Islamic art, calligraphy-style posters, mosque & nature backgrounds. Free · a few images a day.",
                fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
            Spacer(Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(NOOR_IMAGE_STYLES) { (code, label) ->
                    FilterChip(selected = style == code, onClick = { style = code }, label = { Text(label) })
                }
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = prompt, onValueChange = { prompt = it.take(300) },
                modifier = Modifier.fillMaxWidth().testTag("noorai_image_prompt"),
                placeholder = { Text("Describe your image, e.g. ${NOOR_IMAGE_IDEAS[1]}") },
                minLines = 2, maxLines = 4,
            )
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(NOOR_IMAGE_IDEAS) { idea ->
                    Text(idea, fontSize = 12.sp, modifier = Modifier.clip(RoundedCornerShape(14.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                        .clickable { prompt = idea }.padding(horizontal = 10.dp, vertical = 6.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(onClick = { generate() }, enabled = !busy && (prompt.isNotBlank() || style != "calligraphy"),
                colors = ButtonDefaults.buttonColors(containerColor = StudioGreen), modifier = Modifier.fillMaxWidth().height(50.dp)) {
                if (busy) { CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp); Spacer(Modifier.width(10.dp)); Text("Creating… (about 10 s)") }
                else Text("Generate", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(14.dp))
            when (val r = result) {
                is NoorImageResult.Image -> if (bitmap != null) {
                    Image(bitmap.asImageBitmap(), "Generated image", contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(18.dp)).testTag("noorai_image_result"))
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = {
                            scope.launch {
                                val ok = saveToGallery(context, r.bytes)
                                android.widget.Toast.makeText(context, if (ok) "Saved to Pictures/Noor Pro" else "Couldn't save. Try Share instead.", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Download, null); Spacer(Modifier.width(6.dp)); Text("Save") }
                        Button(onClick = { scope.launch { shareImage(context, r.bytes) } }, modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = StudioGreen)) { Icon(Icons.Default.Share, null); Spacer(Modifier.width(6.dp)); Text("Share") }
                    }
                }
                is NoorImageResult.Refused -> Notice(Icons.Default.Block, r.message, Color(0xFFC0563F))
                is NoorImageResult.Failed -> Notice(Icons.Default.Block, r.message, MaterialTheme.colorScheme.onSurfaceVariant)
                null -> Text("Noor AI never draws people, faces, animals, prophets, Sahabah, Ahl al-Bayt or awliya, and nothing immodest or non-Islamic.",
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun Notice(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, tint: Color) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(tint.copy(alpha = 0.10f)).padding(12.dp).testTag("noorai_image_notice"),
        verticalAlignment = Alignment.Top) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 13.sp, lineHeight = 19.sp)
    }
}

private suspend fun saveToGallery(context: Context, bytes: ByteArray): Boolean = withContext(Dispatchers.IO) {
    if (Build.VERSION.SDK_INT < 29) return@withContext false
    runCatching {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "noorpro_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Noor Pro")
        }
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return@runCatching false
        context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) } ?: return@runCatching false
        true
    }.getOrDefault(false)
}

private suspend fun shareImage(context: Context, bytes: ByteArray) {
    val file = withContext(Dispatchers.IO) { File(context.cacheDir, "noorpro_image.jpg").apply { writeBytes(bytes) } }
    runCatching {
        val uri = androidx.core.content.FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).setType("image/jpeg").putExtra(Intent.EXTRA_STREAM, uri)
            .putExtra(Intent.EXTRA_TEXT, "Made with Noor AI · Noor Pro").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(send, "Share image").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
