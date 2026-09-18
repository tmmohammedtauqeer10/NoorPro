package com.noorpro.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noorpro.app.data.DriveBook
import com.noorpro.app.ui.theme.MatteGold
import com.noorpro.app.ui.theme.NightBackground
import com.noorpro.app.ui.theme.SlateCard
import com.noorpro.app.ui.theme.TextPrimary
import com.noorpro.app.ui.theme.TextSecondary
import com.noorpro.app.ui.viewmodel.DeenScreen
import com.noorpro.app.ui.viewmodel.DeenViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfLibraryHubScreen(viewModel: DeenViewModel) {
    var books by remember { mutableStateOf<List<DriveBook>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedBook by remember { mutableStateOf<DriveBook?>(null) }

    LaunchedEffect(Unit) {
        errorMessage = "Cloud Library is not configured for this release. Use Islamic Library for verified books."
    }

    if (selectedBook != null) {
        SmartPdfViewerScreen(
            book = selectedBook!!,
            viewModel = viewModel,
            onBack = { selectedBook = null }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Cloud Library", color = MatteGold) },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.navigateTo(DeenScreen.DASHBOARD) }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MatteGold)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = NightBackground
                    )
                )
            }
        ) { paddingInfo ->
            Box(modifier = Modifier.fillMaxSize().background(NightBackground).padding(paddingInfo)) {
                if (isLoading) {
                    CircularProgressIndicator(color = MatteGold, modifier = Modifier.align(Alignment.Center))
                } else if (errorMessage != null) {
                    Text(errorMessage ?: "Error", color = Color.Red, modifier = Modifier.align(Alignment.Center).padding(16.dp))
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        items(books) { book ->
                            BookItem(book) {
                                selectedBook = book
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BookItem(book: DriveBook, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MatteGold.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.MenuBook, contentDescription = "Book", tint = MatteGold, modifier = Modifier.size(32.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = book.title,
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = book.author,
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            }
        }
    }
}
