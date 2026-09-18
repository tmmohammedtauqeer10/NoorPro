package com.noorpro.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.noorpro.app.ui.viewmodel.DeenScreen
import com.noorpro.app.ui.viewmodel.DeenViewModel
import com.noorpro.app.ui.viewmodel.ZakatViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZakatCalculatorScreen(
    viewModel: DeenViewModel,
    zakatViewModel: ZakatViewModel = viewModel()
) {
    var goldGrams by remember { mutableStateOf("") }
    var silverGrams by remember { mutableStateOf("") }
    var cash by remember { mutableStateOf("") }
    var businessAssets by remember { mutableStateOf("") }
    var otherAssets by remember { mutableStateOf("") }
    var goldPriceInput by remember { mutableStateOf("") }
    var silverPriceInput by remember { mutableStateOf("") }

    val history by zakatViewModel.history.collectAsState()

    val goldPricePerGram = goldPriceInput.toDoubleOrNull() ?: 0.0
    val silverPricePerGram = silverPriceInput.toDoubleOrNull() ?: 0.0

    // Nisab thresholds (Gold: 85g, Silver: 595g)
    val goldNisabThreshold = 85.0 * goldPricePerGram
    val silverNisabThreshold = 595.0 * silverPricePerGram
    
    val availableThresholds = listOf(goldNisabThreshold, silverNisabThreshold).filter { it > 0.0 }
    val nisabThreshold = availableThresholds.minOrNull() ?: 0.0

    val goldVal = (goldGrams.toDoubleOrNull() ?: 0.0) * goldPricePerGram
    val silverVal = (silverGrams.toDoubleOrNull() ?: 0.0) * silverPricePerGram
    val cashVal = cash.toDoubleOrNull() ?: 0.0
    val businessVal = businessAssets.toDoubleOrNull() ?: 0.0
    val assetsVal = otherAssets.toDoubleOrNull() ?: 0.0

    val totalAssets = goldVal + silverVal + cashVal + businessVal + assetsVal
    val zakatDue = if (nisabThreshold > 0.0 && totalAssets >= nisabThreshold) totalAssets * 0.025 else 0.0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Zakat Calculator", color = MaterialTheme.colorScheme.onBackground) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(DeenScreen.DASHBOARD) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        zakatViewModel.saveZakatHistory(
                            goldValue = goldVal,
                            silverValue = silverVal,
                            cashValue = cashVal,
                            businessValue = businessVal,
                            assetsValue = assetsVal,
                            totalAssets = totalAssets,
                            zakatDue = zakatDue
                        )
                    }) {
                        Icon(Icons.Default.Save, contentDescription = "Save History", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    "Enter current market prices before calculating. Prices are not fetched automatically.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = goldPriceInput,
                    onValueChange = { goldPriceInput = it },
                    label = { Text("Gold price per gram (USD)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = silverPriceInput,
                    onValueChange = { silverPriceInput = it },
                    label = { Text("Silver price per gram (USD)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = goldGrams,
                    onValueChange = { goldGrams = it },
                    label = { Text("Gold (in grams)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = silverGrams,
                    onValueChange = { silverGrams = it },
                    label = { Text("Silver (in grams)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = cash,
                    onValueChange = { cash = it },
                    label = { Text("Cash Savings (USD)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = businessAssets,
                    onValueChange = { businessAssets = it },
                    label = { Text("Business Inventory (USD)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = otherAssets,
                    onValueChange = { otherAssets = it },
                    label = { Text("Other Assets (USD)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Total Eligible Assets: $${String.format("%.2f", totalAssets)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        Text(
                            if (nisabThreshold > 0.0) {
                                "Nisab Threshold: $${String.format("%.2f", nisabThreshold)}"
                            } else {
                                "Enter a gold or silver price to calculate Nisab."
                            },
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Zakat Due (2.5%): $${String.format("%.2f", zakatDue)}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (totalAssets < nisabThreshold && totalAssets > 0) {
                            Text("Your wealth is below the Nisab threshold. Zakat is not mandatory.", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                Text("Calculation History", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(history) { record ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            val dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(record.date))
                            Text(dateStr, fontWeight = FontWeight.Bold)
                            Text("Assets: $${String.format("%.2f", record.totalAssets)}", fontSize = 14.sp)
                            Text("Zakat: $${String.format("%.2f", record.zakatDue)}", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { zakatViewModel.deleteHistory(record) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}
