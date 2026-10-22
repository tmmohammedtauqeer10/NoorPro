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
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val zPrefs = remember { ctx.getSharedPreferences("zakat", android.content.Context.MODE_PRIVATE) }
    var currency by remember { mutableStateOf(zPrefs.getString("currency", null) ?: ZakatCurrency.defaultCode()) }
    var currencyMenu by remember { mutableStateOf(false) }
    val fmt: (Double) -> String = { ZakatCurrency.format(it, currency) }
    val sym = ZakatCurrency.symbol(currency)

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
                ExposedDropdownMenuBox(expanded = currencyMenu, onExpandedChange = { currencyMenu = it }) {
                    OutlinedTextField(
                        value = "${ZakatCurrency.label(currency)} ($sym)", onValueChange = {}, readOnly = true,
                        label = { Text("Currency") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(currencyMenu) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = currencyMenu, onDismissRequest = { currencyMenu = false }) {
                        ZakatCurrency.codes.forEach { c ->
                            DropdownMenuItem(text = { Text("$c · ${ZakatCurrency.label(c)} (${ZakatCurrency.symbol(c)})") }, onClick = {
                                currency = c; currencyMenu = false; zPrefs.edit().putString("currency", c).apply()
                            })
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Prices are not fetched automatically (works offline). Enter today's gold and silver price per gram in $currency from your local jeweller or market.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = goldPriceInput,
                    onValueChange = { goldPriceInput = it },
                    label = { Text("Gold price per gram ($sym, $currency)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = silverPriceInput,
                    onValueChange = { silverPriceInput = it },
                    label = { Text("Silver price per gram ($sym, $currency)") },
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
                    label = { Text("Cash savings ($currency)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = businessAssets,
                    onValueChange = { businessAssets = it },
                    label = { Text("Business inventory ($currency)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = otherAssets,
                    onValueChange = { otherAssets = it },
                    label = { Text("Other assets ($currency)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Total eligible assets: ${fmt(totalAssets)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        Text(
                            if (nisabThreshold > 0.0) {
                                "Nisab threshold: ${fmt(nisabThreshold)} (lower of 85 g gold / 595 g silver)"
                            } else {
                                "Enter a gold or silver price to calculate Nisab."
                            },
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Zakat due (2.5%): ${fmt(zakatDue)}",
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
                        Column(Modifier.weight(1f)) {
                            val dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(record.date))
                            Text(dateStr, fontWeight = FontWeight.Bold)
                            Text("Assets: ${ZakatCurrency.plain(record.totalAssets)}", fontSize = 14.sp)
                            Text("Zakat: ${ZakatCurrency.plain(record.zakatDue)}", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
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

/** Currency list + locale-aware formatting for the Zakat calculator (user-entered prices, no conversion). */
object ZakatCurrency {
    val codes = listOf("INR", "USD", "EUR", "GBP", "SAR", "AED", "PKR", "BDT", "MYR", "IDR", "TRY", "EGP", "NGN", "CAD", "AUD", "QAR", "KWD", "BHD", "OMR", "JOD", "MAD", "ZAR", "SGD", "LKR", "KES")
    fun defaultCode(): String = runCatching { Currency.getInstance(Locale.getDefault()).currencyCode }.getOrNull()?.takeIf { it in codes } ?: "INR"
    fun symbol(code: String): String = runCatching { Currency.getInstance(code).getSymbol(Locale.getDefault()) }.getOrDefault(code)
    fun label(code: String): String = runCatching { Currency.getInstance(code).getDisplayName(Locale.getDefault()) }.getOrDefault(code)
    fun format(v: Double, code: String): String = runCatching {
        val c = Currency.getInstance(code)
        val nf = java.text.NumberFormat.getCurrencyInstance(if (code == "INR") Locale("en", "IN") else Locale.getDefault())
        nf.currency = c; nf.maximumFractionDigits = c.defaultFractionDigits.coerceAtLeast(0); nf.minimumFractionDigits = nf.maximumFractionDigits
        nf.format(v)
    }.getOrDefault("$code ${"%.2f".format(v)}")
    fun plain(v: Double): String = java.text.NumberFormat.getNumberInstance(Locale.getDefault()).apply { maximumFractionDigits = 2; minimumFractionDigits = 2 }.format(v)
}
