package com.noorpro.app.ui.screens

import com.noorpro.app.utils.CrashReporter

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.noorpro.app.data.DriveBook
import com.noorpro.app.data.DrivePdfRepository
import com.noorpro.app.ui.theme.MatteGold
import com.noorpro.app.ui.viewmodel.DeenViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartPdfViewerScreen(
    book: DriveBook,
    viewModel: DeenViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var pdfFile by remember { mutableStateOf<File?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var initialPage by remember { mutableIntStateOf(0) }
    var hasDisplayedBookmarkMessage by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val darkBackground = Color(0xFF0B132B) // Requested #0B132B
    val goldText = MatteGold // Requested #D4AF37

    LaunchedEffect(book.pdf_url) {
        try {
            isLoading = true
            // Load preferred start page
            val savedPage = viewModel.userPreferencesRepo.getPdfBookmarkFlow(book.id).first()
            initialPage = savedPage

            // Download file
            val repo = DrivePdfRepository(context)
            val file = kotlinx.coroutines.withTimeout(120000) {
                repo.downloadPdf(book.pdf_url, "drive_book_${book.id}")
            }
            pdfFile = file
            isLoading = false
            
            if (savedPage > 0 && !hasDisplayedBookmarkMessage) {
                hasDisplayedBookmarkMessage = true
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        message = "Resumed reading from page $savedPage",
                        duration = SnackbarDuration.Short
                    )
                }
            }
        } catch (e: Exception) {
            CrashReporter.report(e)
            errorMessage = if (e.message?.contains("public", ignoreCase = true) == true) {
                "This book is not shared publicly by the library owner yet."
            } else {
                "Failed to load this PDF. Please check your connection and try again."
            }
            isLoading = false
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(book.title, color = goldText) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = goldText)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = darkBackground
                )
            )
        }
    ) { paddingInfo ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingInfo)
                .background(darkBackground),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = goldText)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Loading Book...", color = goldText.copy(alpha = 0.8f))
                }
            } else if (errorMessage != null) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        errorMessage ?: "This book is unavailable.",
                        color = goldText,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "The Drive file must use “Anyone with the link” viewer access. No Google sign-in is required inside Noor Pro.",
                        color = Color.White.copy(alpha = 0.72f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            isLoading = true
                            errorMessage = null
                            coroutineScope.launch {
                                try {
                                    val savedPage = viewModel.userPreferencesRepo.getPdfBookmarkFlow(book.id).first()
                                    initialPage = savedPage
                                    val repo = DrivePdfRepository(context)
                                    val file = kotlinx.coroutines.withTimeout(120000) {
                                        repo.downloadPdf(book.pdf_url, "drive_book_${book.id}")
                                    }
                                    pdfFile = file
                                    isLoading = false
                                } catch (e: Exception) {
                                    CrashReporter.report(e)
                                    errorMessage = "Failed to load PDF. Connection timed out."
                                    isLoading = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = goldText)
                    ) {
                        Text("Retry", color = darkBackground)
                    }
                    TextButton(onClick = onBack) {
                        Text("Back to Library", color = goldText)
                    }
                }
            } else if (pdfFile != null) {
                PdfViewer(
                    file = pdfFile!!,
                    initialPage = initialPage,
                    onPageChanged = { page, _ ->
                        coroutineScope.launch {
                            viewModel.userPreferencesRepo.savePdfBookmark(book.id, page)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
