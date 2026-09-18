package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BookItem
import com.example.data.LibraryCategory
import com.example.ui.viewmodel.DeenScreen
import com.example.ui.viewmodel.DeenViewModel

val DeepBlue = Color(0xFF0B132B)
val GoldAccent = Color(0xFFD4AF37)
val SlateCard = Color(0xFF152243)
val GreenRead = Color(0xFF2E7D32)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookShelfScreen(viewModel: DeenViewModel) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Islamic Books", color = GoldAccent, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(DeenScreen.DASHBOARD) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = GoldAccent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepBlue)
            )
        },
        containerColor = DeepBlue
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            OnlineBooksSection(viewModel)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HadithLibraryScreen(viewModel: DeenViewModel) {
    val colors = MaterialTheme.colorScheme
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hadith", color = colors.onBackground, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(DeenScreen.DASHBOARD) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background)
            )
        },
        containerColor = colors.background
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            HadithBooksSection(viewModel)
        }
    }
}

@Composable
fun OnlineBooksSection(viewModel: DeenViewModel) {
    val categories by viewModel.libraryCategories.collectAsState()
    val isLoading by viewModel.isLibraryLoading.collectAsState()
    val error by viewModel.libraryError.collectAsState()
    Column(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GoldAccent)
            }
        } else if (error != null) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Error loading catalog: $error", color = Color.Red, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.loadLibraryCatalog() },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = Color.Black)
                ) {
                    Text("Retry")
                }
            }
        } else if (categories.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("No books found in the catalog.", color = Color.LightGray, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.loadLibraryCatalog() },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = Color.Black)
                ) {
                    Text("Load Catalog")
                }
            }
        } else {
        LazyColumn(
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            items(categories) { category ->
                Column {
                    Text(
                        text = category.categoryTitle,
                        color = GoldAccent,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(category.books) { book ->
                            OnlineBookCard(book = book) {
                                viewModel.openBook(book)
                            }
                        }
                    }
                }
            }
        }
        }
    }
}

@Composable
fun OnlineBookCard(book: com.example.data.BookItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(150.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .background(Color.DarkGray)
            ) {
                // Remote catalog covers can return a successful blank placeholder that Coil
                // caches. A deterministic generated cover stays useful across every reload.
                GeneratedBookCover(book)
            }
            
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = book.title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = book.author,
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    maxLines = 1
                )
                if (book.pages != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${book.pages} pages",
                        color = GoldAccent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onClick,
                    modifier = Modifier.fillMaxWidth().height(36.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("Read Now", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun GeneratedBookCover(book: BookItem) {
    val accent = when {
        "Quran" in book.title || "Tafsir" in book.title || "Tajweed" in book.title -> Color(0xFF2E7D5B)
        "Hadith" in book.title || "Riyadh" in book.title -> Color(0xFFA65D32)
        "Muhammad" in book.title || "Nectar" in book.title || "Companions" in book.title -> Color(0xFF9B7A2F)
        "Finance" in book.title || "Musharakah" in book.title -> Color(0xFF326B55)
        "Muslimah" in book.title -> Color(0xFF8B526B)
        else -> Color(0xFF425E87)
    }
    val shortTitle = book.title
        .substringBefore("—")
        .substringBefore("(")
        .trim()
    val category = when {
        "Quran" in book.title || "Tafsir" in book.title || "Tajweed" in book.title -> "QURAN STUDIES"
        "Hadith" in book.title || "Riyadh" in book.title -> "HADITH"
        "Muhammad" in book.title || "Nectar" in book.title || "Companions" in book.title -> "SEERAH"
        "Finance" in book.title || "Musharakah" in book.title -> "ISLAMIC FINANCE"
        "Muslimah" in book.title -> "FAMILY & FAITH"
        else -> "ISLAMIC LIBRARY"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(accent, DeepBlue))),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val ornament = GoldAccent.copy(alpha = 0.18f)
            val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height * 0.42f)
            drawCircle(
                color = ornament,
                radius = size.width * 0.34f,
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
            )
            drawCircle(
                color = ornament,
                radius = size.width * 0.25f,
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
            )
            val diamond = size.width * 0.19f
            rotate(45f, center) {
                drawRect(
                    color = ornament,
                    topLeft = androidx.compose.ui.geometry.Offset(center.x - diamond, center.y - diamond),
                    size = androidx.compose.ui.geometry.Size(diamond * 2, diamond * 2),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, GoldAccent.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                .background(Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.MenuBook,
                    contentDescription = null,
                    tint = GoldAccent,
                    modifier = Modifier.size(38.dp)
                )
                Text(
                    category,
                    color = GoldAccent.copy(alpha = 0.85f),
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    shortTitle,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    lineHeight = 17.sp,
                    maxLines = 4,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    book.author,
                    color = GoldAccent,
                    fontSize = 9.sp,
                    maxLines = 2,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun HadithBooksSection(viewModel: DeenViewModel) {
    val colors = MaterialTheme.colorScheme
    val books = listOf(
        com.example.data.HadithBook("bukhari", "Sahih Bukhari"),
        com.example.data.HadithBook("muslim", "Sahih Muslim"),
        com.example.data.HadithBook("nasai", "Sunan An-Nasai"),
        com.example.data.HadithBook("abudawud", "Sunan Abu Dawud"),
        com.example.data.HadithBook("tirmidhi", "Jami At-Tirmidhi"),
        com.example.data.HadithBook("ibnmajah", "Sunan Ibn Majah"),
        com.example.data.HadithBook("malik", "Muwatta Malik"),
        com.example.data.HadithBook("nawawi40", "An-Nawawi's 40 Hadith"),
        com.example.data.HadithBook("riyadussalihin", "Riyad As-Salihin"),
        com.example.data.HadithBook("adab", "Al-Adab Al-Mufrad"),
        com.example.data.HadithBook("bulugh", "Bulugh al-Maram"),
        com.example.data.HadithBook("darimi", "Sunan Ad-Darimi"),
        com.example.data.HadithBook("ahmad", "Musnad Ahmad"),
        com.example.data.HadithBook("hisn", "Hisn al-Muslim")
    )
    
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Hadith Collections",
                color = colors.onBackground,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Choose a collection to browse chapters and narrations.",
                color = colors.onSurfaceVariant,
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        items(books) { book ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.selectHadithBook(book.id) },
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .background(colors.primaryContainer, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.MenuBook, contentDescription = "Book", tint = colors.onPrimaryContainer)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(text = book.name, color = colors.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Available with multiple language translations", color = colors.onSurfaceVariant, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// Remove the old BookItemCard and BookDetailsContent
