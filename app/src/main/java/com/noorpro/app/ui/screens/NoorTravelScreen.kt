package com.noorpro.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import com.noorpro.app.data.TravelPartner
import com.noorpro.app.data.TravelSearch
import com.noorpro.app.ui.components.*
import com.noorpro.app.ui.viewmodel.DeenScreen
import com.noorpro.app.ui.viewmodel.DeenViewModel
import java.util.Locale

@Composable
fun NoorTravelEntryCard(onOpen: () -> Unit) {
    Card(onClick = onOpen, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = stitchSurface())) {
        Column(Modifier.padding(20.dp)) {
            Text("NoorPro Travel", color = stitchText(), style = MaterialTheme.typography.titleLarge)
            Text("International flights • Umrah • Hajj assistance", color = stitchMutedText())
            Text("Explore travel →", color = stitchPrimary(), modifier = Modifier.padding(top = 12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoorTravelScreen(viewModel: DeenViewModel) {
    val context = LocalContext.current
    var section by rememberSaveable { mutableStateOf("Flights") }
    var origin by rememberSaveable { mutableStateOf("") }
    var destination by rememberSaveable { mutableStateOf("") }
    var departure by rememberSaveable { mutableStateOf("") }
    var returnDate by rememberSaveable { mutableStateOf("") }
    var adults by rememberSaveable { mutableStateOf("1") }
    var roundTrip by rememberSaveable { mutableStateOf(true) }
    var partners by remember { mutableStateOf<Map<String, TravelPartner>>(emptyMap()) }
    var configMessage by remember { mutableStateOf("Loading travel partners…") }
    var error by remember { mutableStateOf<String?>(null) }
    var handoff by remember { mutableStateOf<Pair<String, String>?>(null) }
    var reload by remember { mutableIntStateOf(0) }

    DisposableEffect(reload) {
        configMessage = "Loading travel partners…"
        val registration = runCatching {
            FirebaseFirestore.getInstance().collection("travelPartners").addSnapshotListener { snapshot, failure ->
                if (failure != null) {
                    partners = emptyMap()
                    configMessage = "Travel partners could not be loaded. Check your connection and try again."
                } else {
                    partners = snapshot?.documents.orEmpty().associate { doc ->
                        doc.id to TravelPartner(doc.getString("name").orEmpty(), doc.getString("urlTemplate").orEmpty(), doc.getBoolean("enabled") == true)
                    }
                    configMessage = "Booking partners are being arranged. Please check back later."
                }
            }
        }.getOrNull()
        if (registration == null) configMessage = "Travel services are currently unavailable."
        onDispose { registration?.remove() }
    }

    Scaffold(
        containerColor = stitchBackground(),
        topBar = { TopAppBar(
            title = { Text("NoorPro Travel", color = stitchText()) },
            navigationIcon = { TextButton(onClick = { if (!viewModel.goBack()) viewModel.navigateTo(DeenScreen.DASHBOARD) }) { Text("Back") } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = stitchBackground())
        ) }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).imePadding(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 190.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Flights", "Umrah", "Hajj").forEach { label ->
                        FilterChip(selected = section == label, onClick = { section = label; error = null }, label = { Text(label) })
                    }
                }
            }
            if (section == "Flights") {
                item {
                    Text("Fly to Saudi Arabia or other countries", color = stitchText(), style = MaterialTheme.typography.titleMedium)
                    Text("Search with a travel partner. Prices, availability and tickets are confirmed on their website.", color = stitchMutedText())
                }
                item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(roundTrip, { roundTrip = true }, { Text("Return") })
                    FilterChip(!roundTrip, { roundTrip = false }, { Text("One-way") })
                } }
                item { TravelField("Departure airport code (e.g. BLR)", origin) { origin = it.trim().uppercase(Locale.US).take(3); error = null } }
                item { TravelField("Destination airport code (e.g. JED)", destination) { destination = it.trim().uppercase(Locale.US).take(3); error = null } }
                item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { destination = "JED"; error = null }) { Text("Jeddah") }
                    OutlinedButton(onClick = { destination = "MED"; error = null }) { Text("Madinah") }
                } }
                item { TravelField("Departure date (YYYY-MM-DD)", departure) { departure = it; error = null } }
                if (roundTrip) item { TravelField("Return date (YYYY-MM-DD)", returnDate) { returnDate = it; error = null } }
                item { TravelField("Adult passengers (1–9)", adults) { adults = it; error = null } }
                item { Text("This first version supports adult passengers. For children, infants or group bookings, contact the partner directly.", color = stitchMutedText()) }
            } else {
                item {
                    Text(if (section == "Umrah") "Plan your Umrah journey" else "Hajj assistance", color = stitchText(), style = MaterialTheme.typography.titleLarge)
                    Text(if (section == "Umrah") "Ask the operator about flights, hotels, transport, visa assistance and package inclusions." else "Ask an authorized operator about applications, eligibility and packages. A flight ticket alone does not authorize Hajj.", color = stitchMutedText())
                }
            }
            item {
                val key = section.lowercase(Locale.US)
                val partner = partners[key]
                // Validate configuration independently of the user's form before enabling the action.
                val configured = partner?.bookingUrl(TravelSearch("BLR", "JED", "2099-01-01", "2099-01-02", "1", true)) != null
                if (!configured) {
                    Text(configMessage, color = stitchMutedText())
                    TextButton(onClick = { reload++ }) { Text("Retry") }
                } else {
                    Text("Booking and support provided by ${partner?.name}", color = stitchMutedText())
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 8.dp)) }
                Button(enabled = configured, modifier = Modifier.fillMaxWidth(), onClick = {
                    val search = if (section == "Flights") TravelSearch(origin, destination, departure, returnDate, adults, roundTrip) else null
                    val validation = search?.validationError()
                    if (validation != null) error = validation
                    else {
                        val url = partner?.bookingUrl(search)
                        if (url == null) error = "The partner link is unavailable. Please try again later."
                        else handoff = partner?.name.orEmpty() to url
                    }
                }) { Text(if (section == "Flights") "Continue to flight partner" else "Enquire with operator") }
            }
            item {
                HorizontalDivider()
                Text("Prepare for your pilgrimage", color = stitchText(), fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp))
                Text("Open the existing Hajj and Umrah guide for rituals, duas and your journey planner.", color = stitchMutedText())
                OutlinedButton(onClick = { viewModel.navigateTo(DeenScreen.HAJJ_UMRAH) }) { Text("Open Hajj & Umrah guide") }
            }
        }
    }
    handoff?.let { (name, url) ->
        AlertDialog(
            onDismissRequest = { handoff = null },
            title = { Text("Continue to $name") },
            text = { Text("You will open ${Uri.parse(url).host}. Booking, payment, tickets, changes and refunds are handled by $name. Review their final fare and terms before paying.") },
            confirmButton = { TextButton(onClick = {
                handoff = null
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                    .onFailure { error = "Unable to open the partner website. Please check that a browser is installed." }
            }) { Text("Continue") } },
            dismissButton = { TextButton(onClick = { handoff = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun TravelField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = onChange, label = { Text(label) }, singleLine = true, modifier = Modifier.fillMaxWidth())
}
