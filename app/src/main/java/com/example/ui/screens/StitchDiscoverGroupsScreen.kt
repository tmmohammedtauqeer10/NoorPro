package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.UmmahGroup
import com.example.data.UmmahRepository
import com.example.ui.components.StitchCard
import com.example.ui.components.StitchEmerald
import com.example.ui.components.StitchGold
import com.example.ui.components.StitchLine
import com.example.ui.components.StitchScreen
import com.example.ui.components.stitchMutedText
import com.example.ui.components.stitchPrimary
import com.example.ui.components.stitchSoftSurface
import com.example.ui.components.stitchSurface
import com.example.ui.components.stitchText
import com.example.ui.viewmodel.DeenScreen
import com.example.ui.viewmodel.DeenViewModel

/**
 * Discover & join community groups (WhatsApp/WeChat-style). Lists public groups, filters by name/
 * description as you type, and lets you join with one tap. Groups you're already in are marked.
 */
@Composable
fun StitchDiscoverGroupsScreen(viewModel: DeenViewModel) {
    val context = LocalContext.current
    val repository = remember { UmmahRepository() }
    val currentUid = remember(viewModel.isLoggedIn) { repository.currentUserUid().orEmpty() }
    var groups by remember { mutableStateOf<List<UmmahGroup>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var query by remember { mutableStateOf("") }
    var joining by remember { mutableStateOf<Set<String>>(emptySet()) }

    DisposableEffect(Unit) {
        val repo = UmmahRepository()
        repo.observeDiscoverGroups { list, _ ->
            groups = list
            loading = false
        }
        onDispose { repo.close() }
    }

    val q = query.trim().lowercase()
    val results = remember(groups, q) {
        if (q.isBlank()) groups
        else groups.filter { it.name.lowercase().contains(q) || it.description.lowercase().contains(q) }
    }

    StitchScreen {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, top = 42.dp, end = 20.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.statusBarsPadding()) {
                    IconButton(onClick = { if (!viewModel.goBack()) viewModel.navigateTo(DeenScreen.UMMAH_MESSAGES) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = stitchText())
                    }
                    Spacer(Modifier.weight(1f))
                    Text("Discover Groups", color = stitchText(), fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = com.example.ui.theme.LibreCaslon)
                    Spacer(Modifier.weight(1f))
                    Spacer(Modifier.size(48.dp))
                }
            }
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = stitchPrimary()) },
                    placeholder = { Text("Search groups by name or topic...") },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            when {
                loading -> item {
                    Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = stitchPrimary())
                    }
                }
                results.isEmpty() -> item {
                    StitchCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                        Column(modifier = Modifier.fillMaxWidth().padding(26.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(StitchEmerald.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Groups, contentDescription = null, tint = stitchPrimary(), modifier = Modifier.size(28.dp))
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(if (q.isBlank()) "No groups yet" else "No groups found", color = stitchText(), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                if (q.isBlank()) "Be the first to create a community group from Messages." else "Try a different name or topic.",
                                color = stitchMutedText(), fontSize = 13.sp
                            )
                        }
                    }
                }
                else -> {
                    if (q.isBlank()) {
                        item {
                            Text("Suggested for you", color = stitchMutedText(), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    items(results, key = { it.id }) { group ->
                        val joined = currentUid.isNotBlank() && currentUid in group.memberUids
                        StitchDiscoverGroupRow(
                            group = group,
                            joined = joined,
                            joining = group.id in joining,
                            onOpen = { if (joined) viewModel.navigateTo(DeenScreen.UMMAH_MESSAGES) },
                            onJoin = {
                                if (!viewModel.isLoggedIn) {
                                    Toast.makeText(context, "Sign in to join groups", Toast.LENGTH_SHORT).show()
                                } else {
                                    joining = joining + group.id
                                    repository.joinGroup(group.id) { ok, err ->
                                        joining = joining - group.id
                                        if (ok) Toast.makeText(context, "Joined ${group.name}", Toast.LENGTH_SHORT).show()
                                        else Toast.makeText(context, err ?: "Unable to join", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StitchDiscoverGroupRow(
    group: UmmahGroup,
    joined: Boolean,
    joining: Boolean,
    onOpen: () -> Unit,
    onJoin: () -> Unit
) {
    StitchCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen), shape = RoundedCornerShape(18.dp)) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(52.dp).clip(CircleShape).background(StitchEmerald.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Groups, contentDescription = null, tint = stitchPrimary(), modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(group.name, color = stitchText(), fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(
                    group.description.ifBlank { "${group.memberUids.size} members" },
                    color = stitchMutedText(),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text("${group.memberUids.size} members", color = StitchGold, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.width(10.dp))
            if (joined) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(stitchSoftSurface()).border(1.dp, StitchLine, RoundedCornerShape(999.dp)).padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = stitchPrimary(), modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Joined", color = stitchPrimary(), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(stitchPrimary()).clickable(enabled = !joining, onClick = onJoin).padding(horizontal = 18.dp, vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (joining) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Join", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
