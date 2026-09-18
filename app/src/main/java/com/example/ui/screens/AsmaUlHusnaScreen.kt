package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.DeenScreen
import com.example.ui.viewmodel.DeenViewModel

data class NameOfAllah(val id: Int, val arabic: String, val transliteration: String, val meaning: String)

val asmaUlHusnaList = listOf(
    NameOfAllah(1, "الرَّحْمَنُ", "Ar-Rahman", "The Most Compassionate"),
    NameOfAllah(2, "الرَّحِيمُ", "Ar-Rahim", "The Most Merciful"),
    NameOfAllah(3, "الْمَلِكُ", "Al-Malik", "The King"),
    NameOfAllah(4, "الْقُدُّوسُ", "Al-Quddus", "The Most Holy"),
    NameOfAllah(5, "السَّلَامُ", "As-Salam", "The Source of Peace"),
    NameOfAllah(6, "الْمُؤْمِنُ", "Al-Mu'min", "The Giver of Faith"),
    NameOfAllah(7, "الْمُهَيْمِنُ", "Al-Muhaymin", "The Guardian"),
    NameOfAllah(8, "الْعَزِيزُ", "Al-Aziz", "The Almighty"),
    NameOfAllah(9, "الْجَبَّارُ", "Al-Jabbar", "The Compeller"),
    NameOfAllah(10, "الْمُتَكَبِّرُ", "Al-Mutakabbir", "The Supreme"),
    NameOfAllah(11, "الْخَالِقُ", "Al-Khaliq", "The Creator"),
    NameOfAllah(12, "الْبَارِئُ", "Al-Bari", "The Originator"),
    NameOfAllah(13, "الْمُصَوِّرُ", "Al-Musawwir", "The Fashioner"),
    NameOfAllah(14, "الْغَفَّارُ", "Al-Ghaffar", "The Constant Forgiver"),
    NameOfAllah(15, "الْقَهَّارُ", "Al-Qahhar", "The All-Subduer"),
    NameOfAllah(16, "الْوَهَّابُ", "Al-Wahhab", "The Supreme Bestower"),
    NameOfAllah(17, "الرَّزَّاقُ", "Ar-Razzaq", "The Provider"),
    NameOfAllah(18, "الْفَتَّاحُ", "Al-Fattah", "The Opener"),
    NameOfAllah(19, "اَلْعَلِيْمُ", "Al-Alim", "The All-Knowing"),
    NameOfAllah(20, "الْقَابِضُ", "Al-Qabid", "The Withholder"),
    NameOfAllah(21, "الْبَاسِطُ", "Al-Basit", "The Extender"),
    NameOfAllah(22, "الْخَافِضُ", "Al-Khafid", "The Reducer"),
    NameOfAllah(23, "الرَّافِعُ", "Ar-Rafi", "The Exalter"),
    NameOfAllah(24, "الْمُعِزُّ", "Al-Mu'izz", "The Honourer"),
    NameOfAllah(25, "المُذِلُّ", "Al-Mudhill", "The Humiliator"),
    NameOfAllah(26, "السَّمِيعُ", "As-Sami", "The All-Hearing"),
    NameOfAllah(27, "الْبَصِيرُ", "Al-Basir", "The All-Seeing"),
    NameOfAllah(28, "الْحَكَمُ", "Al-Hakam", "The Judge"),
    NameOfAllah(29, "الْعَدْلُ", "Al-Adl", "The Utterly Just"),
    NameOfAllah(30, "اللَّطِيفُ", "Al-Latif", "The Most Gentle"),
    NameOfAllah(31, "الْخَبِيرُ", "Al-Khabir", "The All-Aware"),
    NameOfAllah(32, "الْحَلِيمُ", "Al-Halim", "The Most Forbearing"),
    NameOfAllah(33, "الْعَظِيمُ", "Al-Azim", "The Magnificent"),
    NameOfAllah(34, "الْغَفُورُ", "Al-Ghafur", "The Great Forgiver"),
    NameOfAllah(35, "الشَّكُورُ", "Ash-Shakur", "The Most Appreciative"),
    NameOfAllah(36, "الْعَلِيُّ", "Al-Ali", "The Most High"),
    NameOfAllah(37, "الْكَبِيرُ", "Al-Kabir", "The Most Great"),
    NameOfAllah(38, "الْحَفِيظُ", "Al-Hafiz", "The Preserver"),
    NameOfAllah(39, "المُقيِت", "Al-Muqit", "The Sustainer"),
    NameOfAllah(40, "الْحسِيبُ", "Al-Hasib", "The Reckoner"),
    NameOfAllah(41, "الْجَلِيلُ", "Al-Jalil", "The Majestic"),
    NameOfAllah(42, "الْكَرِيمُ", "Al-Karim", "The Most Generous"),
    NameOfAllah(43, "الرَّقِيبُ", "Ar-Raqib", "The Watchful"),
    NameOfAllah(44, "الْمُجِيبُ", "Al-Mujib", "The Responsive"),
    NameOfAllah(45, "الْوَاسِعُ", "Al-Wasi", "The All-Encompassing"),
    NameOfAllah(46, "الْحَكِيمُ", "Al-Hakim", "The All-Wise"),
    NameOfAllah(47, "الْوَدُودُ", "Al-Wadud", "The Most Loving"),
    NameOfAllah(48, "الْمَجِيدُ", "Al-Majeed", "The Glorious"),
    NameOfAllah(49, "الْبَاعِثُ", "Al-Ba'ith", "The Resurrector"),
    NameOfAllah(50, "الشَّهِيدُ", "Ash-Shahid", "The Witness"),
    NameOfAllah(51, "الْحَقُّ", "Al-Haqq", "The Truth"),
    NameOfAllah(52, "الْوَكِيلُ", "Al-Wakil", "The Trustee"),
    NameOfAllah(53, "الْقَوِيُّ", "Al-Qawiyy", "The All-Strong"),
    NameOfAllah(54, "الْمَتِينُ", "Al-Matin", "The Firm"),
    NameOfAllah(55, "الْوَلِيُّ", "Al-Waliyy", "The Protecting Friend"),
    NameOfAllah(56, "الْحَمِيدُ", "Al-Hamid", "The Praiseworthy"),
    NameOfAllah(57, "الْمُحْصِي", "Al-Muhsi", "The All-Enumerating"),
    NameOfAllah(58, "الْمُبْدِئُ", "Al-Mubdi", "The Originator"),
    NameOfAllah(59, "الْمُعِيدُ", "Al-Mu'id", "The Restorer"),
    NameOfAllah(60, "الْمُحْيِي", "Al-Muhyi", "The Giver of Life"),
    NameOfAllah(61, "اَلْمُمِيتُ", "Al-Mumit", "The Bringer of Death"),
    NameOfAllah(62, "الْحَيُّ", "Al-Hayy", "The Ever-Living"),
    NameOfAllah(63, "الْقَيُّومُ", "Al-Qayyum", "The Self-Sustaining"),
    NameOfAllah(64, "الْوَاجِدُ", "Al-Wajid", "The Perceiver"),
    NameOfAllah(65, "الْمَاجِدُ", "Al-Majid", "The Illustrious"),
    NameOfAllah(66, "الْواحِدُ", "Al-Wahid", "The One"),
    NameOfAllah(67, "اَلاَحَدُ", "Al-Ahad", "The Unique"),
    NameOfAllah(68, "الصَّمَدُ", "As-Samad", "The Eternal Refuge"),
    NameOfAllah(69, "الْقَادِرُ", "Al-Qadir", "The All-Powerful"),
    NameOfAllah(70, "الْمُقْتَدِرُ", "Al-Muqtadir", "The Creator of All Power"),
    NameOfAllah(71, "الْمُقَدِّمُ", "Al-Muqaddim", "The Expediter"),
    NameOfAllah(72, "الْمُؤَخِّرُ", "Al-Mu'akhkhir", "The Delayer"),
    NameOfAllah(73, "الأوَّلُ", "Al-Awwal", "The First"),
    NameOfAllah(74, "الآخِرُ", "Al-Akhir", "The Last"),
    NameOfAllah(75, "الظَّاهِرُ", "Az-Zahir", "The Manifest"),
    NameOfAllah(76, "الْبَاطِنُ", "Al-Batin", "The Hidden"),
    NameOfAllah(77, "الْوَالِي", "Al-Wali", "The Governor"),
    NameOfAllah(78, "الْمُتَعَالِي", "Al-Muta'ali", "The Most Exalted"),
    NameOfAllah(79, "الْبَرُّ", "Al-Barr", "The Source of Goodness"),
    NameOfAllah(80, "التَّوَابُ", "At-Tawwab", "The Accepter of Repentance"),
    NameOfAllah(81, "الْمُنْتَقِمُ", "Al-Muntaqim", "The Just Retributor"),
    NameOfAllah(82, "العَفُوُّ", "Al-Afuww", "The Pardoner"),
    NameOfAllah(83, "الرَّؤُوفُ", "Ar-Ra'uf", "The Most Kind"),
    NameOfAllah(84, "مَالِكُ الْمُلْكِ", "Malik-ul-Mulk", "Master of the Kingdom"),
    NameOfAllah(85, "ذُوالْجَلَالِ وَالإكْرَامِ", "Dhul-Jalali wal-Ikram", "Lord of Majesty and Honour"),
    NameOfAllah(86, "الْمُقْسِطُ", "Al-Muqsit", "The Equitable"),
    NameOfAllah(87, "الْجَامِعُ", "Al-Jami", "The Gatherer"),
    NameOfAllah(88, "الْغَنِيُّ", "Al-Ghani", "The Self-Sufficient"),
    NameOfAllah(89, "الْمُغْنِي", "Al-Mughni", "The Enricher"),
    NameOfAllah(90, "اَلْمَانِعُ", "Al-Mani", "The Preventer"),
    NameOfAllah(91, "الضَّارَ", "Ad-Darr", "The Distresser"),
    NameOfAllah(92, "النَّافِعُ", "An-Nafi", "The Benefactor"),
    NameOfAllah(93, "النُّورُ", "An-Nur", "The Light"),
    NameOfAllah(94, "الْهَادِي", "Al-Hadi", "The Guide"),
    NameOfAllah(95, "الْبَدِيعُ", "Al-Badi", "The Incomparable Originator"),
    NameOfAllah(96, "اَلْبَاقِي", "Al-Baqi", "The Everlasting"),
    NameOfAllah(97, "الْوَارِثُ", "Al-Warith", "The Inheritor"),
    NameOfAllah(98, "الرَّشِيدُ", "Ar-Rashid", "The Guide to the Right Path"),
    NameOfAllah(99, "الصَّبُورُ", "As-Sabur", "The Most Patient")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AsmaUlHusnaScreen(viewModel: DeenViewModel) {
    var query by remember { mutableStateOf("") }
    val filteredNames = remember(query) {
        asmaUlHusnaList.filter {
            query.isBlank() || it.id.toString() == query ||
                it.transliteration.contains(query, true) ||
                it.meaning.contains(query, true) ||
                it.arabic.contains(query)
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("99 Names of Allah") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(DeenScreen.DASHBOARD) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search name or meaning") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(12.dp)
            )
            Text(
                "${filteredNames.size} of 99 names",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
            )
            LazyVerticalGrid(
                columns = GridCells.Adaptive(160.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredNames, key = { it.id }) { name ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth().height(190.dp)
                    ) {
                        Column(
                            Modifier.fillMaxSize().padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("#${name.id}", color = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.Start))
                            Text(name.arabic, fontSize = 27.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(10.dp))
                            Text(name.transliteration, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(5.dp))
                            Text(name.meaning, fontSize = 12.sp, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
