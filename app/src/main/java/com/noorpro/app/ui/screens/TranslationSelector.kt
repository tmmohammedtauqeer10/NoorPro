package com.noorpro.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noorpro.app.data.TranslationManager
import com.noorpro.app.ui.theme.GlassBorder
import com.noorpro.app.ui.theme.MatteGold
import com.noorpro.app.ui.theme.NightBackground
import com.noorpro.app.ui.theme.TextPrimary
import com.noorpro.app.ui.theme.TextSecondary
import com.noorpro.app.ui.viewmodel.DeenViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslationSelectionBottomSheet(
    viewModel: DeenViewModel,
    translationManager: TranslationManager,
    onDismiss: () -> Unit
) {
    val selectedTranslation by translationManager.selectedTranslation.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    var downloadingKey by remember { mutableStateOf<String?>(null) }
    var downloadProgress by remember { mutableStateOf(0f) }
    var downloadError by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = NightBackground,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MatteGold.copy(alpha = 0.5f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Translate,
                    contentDescription = "Translations",
                    tint = MatteGold,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Select Book/Author",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (downloadError != null) {
                Text(
                    text = "Error: $downloadError",
                    color = Color.Red,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Red.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val groupedTranslations = translationManager.availableTranslations.groupBy { it.language }

                groupedTranslations.forEach { (language, options) ->
                    item {
                        Text(
                            text = language,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MatteGold
                            ),
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }
                    items(options) { option ->
                        val isSelected = selectedTranslation == option.key
                        val isDownloaded = translationManager.isDownloaded(option.key)
                        val isCurrentlyDownloading = downloadingKey == option.key

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) MatteGold.copy(alpha = 0.05f) else Color.Transparent)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) MatteGold.copy(alpha = 0.3f) else GlassBorder.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable(enabled = isDownloaded && !isCurrentlyDownloading) {
                                    translationManager.selectTranslation(option.key)
                                    viewModel.reloadSurahList()
                                    onDismiss()
                                }
                                .padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = option.name,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) MatteGold else TextPrimary
                                        )
                                    )
                                    Text(
                                        text = option.translator,
                                        style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary),
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = option.description,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = TextSecondary.copy(alpha = 0.8f),
                                            lineHeight = 16.sp
                                        ),
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                if (option.key == "default" || isDownloaded) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Selected",
                                            tint = MatteGold,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    } else {
                                        Button(
                                            onClick = {
                                                translationManager.selectTranslation(option.key)
                                                viewModel.reloadSurahList()
                                                onDismiss()
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color.Transparent,
                                                contentColor = TextPrimary
                                            ),
                                            border = BorderStroke(1.dp, GlassBorder),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                            modifier = Modifier.height(36.dp)
                                        ) {
                                            Text("Use", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                } else {
                                    if (isCurrentlyDownloading) {
                                        CircularProgressIndicator(
                                            color = MatteGold,
                                            modifier = Modifier.size(24.dp),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        IconButton(
                                            onClick = {
                                                coroutineScope.launch {
                                                    downloadingKey = option.key
                                                    downloadProgress = 0f
                                                    downloadError = null
                                                    translationManager.downloadTranslation(
                                                        key = option.key,
                                                        onProgress = { downloadProgress = it },
                                                        onSuccess = {
                                                            downloadingKey = null
                                                            translationManager.selectTranslation(option.key)
                                                            viewModel.reloadSurahList()
                                                            onDismiss()
                                                        },
                                                        onError = {
                                                            downloadingKey = null
                                                            downloadError = it
                                                        }
                                                    )
                                                }
                                            },
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(GlassBorder)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CloudDownload,
                                                contentDescription = "Download ${option.name}",
                                                tint = MatteGold,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            if (isCurrentlyDownloading) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    LinearProgressIndicator(
                                        progress = { downloadProgress },
                                        color = MatteGold,
                                        trackColor = GlassBorder,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "${(downloadProgress * 100).toInt()}%",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MatteGold
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
