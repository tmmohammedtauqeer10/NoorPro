package com.noorpro.app.learning

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AcademyScreen(onBack: () -> Unit) {
    val auth = remember { runCatching { FirebaseAuth.getInstance() }.getOrNull() }
    var accountId by remember { mutableStateOf(auth?.currentUser?.uid ?: "guest") }
    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { accountId = it.currentUser?.uid ?: "guest" }
        auth?.addAuthStateListener(listener)
        onDispose { auth?.removeAuthStateListener(listener) }
    }
    // Account changes discard the previous account's UI state and private notes.
    key(accountId) { AcademyContent(accountId, onBack) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AcademyContent(accountId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    val snackbars = remember { SnackbarHostState() }
    val store = remember(accountId) { LearningProgressStore(context, accountId) }
    var catalog by remember { mutableStateOf<AcademyCatalog?>(null) }
    var error by remember { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    var enrolled by remember { mutableStateOf(store.enrolled()) }
    var completed by remember { mutableStateOf(store.completed()) }
    var saving by remember { mutableStateOf(false) }
    var selectedCourse by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedLesson by rememberSaveable { mutableStateOf<String?>(null) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    var subject by rememberSaveable { mutableStateOf("All") }
    var language by rememberSaveable { mutableStateOf("All languages") }
    var resetDialog by remember { mutableStateOf(false) }
    LaunchedEffect(retry) {
        error = false
        try { catalog = LearningCatalog.load(context) } catch (_: Exception) { error = true }
    }
    val course = catalog?.courses?.firstOrNull { it.id == selectedCourse }
    val lesson = course?.lessons?.firstOrNull { it.id == selectedLesson }
    fun openCourse(id: String) { selectedCourse = id; selectedLesson = null }
    fun back() {
        when {
            selectedLesson != null -> selectedLesson = null
            selectedCourse != null -> selectedCourse = null
            else -> onBack()
        }
    }
    BackHandler(enabled = selectedCourse != null) { back() }
    fun write(message: String, action: () -> Boolean) {
        if (saving) return
        saving = true
        scope.launch {
            val success = withContext(Dispatchers.IO) { runCatching(action).getOrDefault(false) }
            enrolled = store.enrolled()
            completed = store.completed()
            saving = false
            snackbars.showSnackbar(if (success) message else "Could not save on this device. Please try again.")
        }
    }
    fun openUrl(url: String) {
        runCatching { require(LearningCatalog.isSafeExternalUrl(url)); uriHandler.openUri(url) }
            .onFailure { scope.launch { snackbars.showSnackbar("Could not open the official classroom.") } }
    }
    Scaffold(
        topBar = { TopAppBar(title = { Text(if (course == null) "NoorPro Academy" else if (lesson == null) "Course overview" else "Study room") },
            navigationIcon = { IconButton(onClick = { back() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }) },
        snackbarHost = { SnackbarHost(snackbars) }
    ) { padding ->
        when {
            error -> Column(Modifier.padding(padding).fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("The course catalog could not be loaded.")
                Button(onClick = { retry++ }) { Text("Retry") }
            }
            catalog == null -> Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            lesson != null && course != null -> key(lesson.id) {
                AcademyLessonView(
                    course, lesson, lesson.id in completed, saving, store.note(lesson.id), Modifier.padding(padding),
                    onOpenUrl = ::openUrl,
                    onSaveNote = { text -> write("Note saved on this device") { store.saveNote(lesson.id, text) } },
                    onToggleComplete = {
                        val done = lesson.id !in completed
                        write(if (done) "Study progress saved" else "Lesson marked incomplete") {
                            store.enroll(course.id) && store.setCompleted(lesson.id, done)
                        }
                    },
                    onNext = {
                        val index = course.lessons.indexOfFirst { it.id == lesson.id }
                        selectedLesson = course.lessons.getOrNull(index + 1)?.id
                    }
                )
            }
            course != null -> AcademyCourseView(
                course, catalog!!.providers.first { it.id == course.providerId }, course.id in enrolled, completed, saving,
                Modifier.padding(padding), onEnroll = { write("Added to My learning") { store.enroll(course.id) } },
                onLesson = { selectedLesson = it }, onOpenUrl = ::openUrl, onReset = { resetDialog = true },
                onShare = {
                    val text = "NoorPro personal study record\n${course.title}\n${completedLessonCount(course, completed)} / ${course.lessons.size} learning activities completed.\nSelf-paced, device-local record; not an accredited certificate, ijazah, or provider-issued credential."
                    runCatching { context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }, "Share study record")) }
                        .onFailure { scope.launch { snackbars.showSnackbar("No app is available to share the record.") } }
                }
            )
            else -> Column(Modifier.padding(padding)) {
                TabRow(selectedTabIndex = tab) {
                    listOf("Discover", "My learning", "Paths").forEachIndexed { index, label ->
                        Tab(selected = tab == index, onClick = { tab = index }, text = { Text(label) })
                    }
                }
                if (tab == 2) {
                    LazyColumn(Modifier.fillMaxSize().testTag("academy_paths"), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        item { Text("A clear direction for your learning", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
                        items(catalog!!.paths, key = { it.id }) { path ->
                            Card {
                                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(path.title, style = MaterialTheme.typography.titleLarge)
                                    Text(path.description)
                                    path.courseIds.forEachIndexed { index, id ->
                                        val target = catalog!!.courses.first { it.id == id }
                                        TextButton(onClick = { openCourse(id) }) { Text("${index + 1}. ${target.title}") }
                                    }
                                }
                            }
                        }
                        item { Text("Suggested routes. Official providers control their own course prerequisites and availability.", style = MaterialTheme.typography.bodySmall) }
                    }
                } else {
                    val visible = catalog!!.courses.filter { c ->
                        (tab != 1 || c.id in enrolled) && (subject == "All" || c.subject == subject) &&
                            (language == "All languages" || c.language.contains(language)) &&
                            listOf(c.title, c.description, c.subject, c.instructor, c.language).any { it.contains(query, ignoreCase = true) }
                    }
                    LazyColumn(Modifier.fillMaxSize().testTag("academy_catalog"), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        item {
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), shape = RoundedCornerShape(24.dp)) {
                                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Icon(Icons.Default.School, contentDescription = null)
                                    Text("Learn with intention.", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                                    Text("Free learning, thoughtful paths, and a little progress every day.")
                                    Text("24 offline reading lessons · In-app lecture · External classrooms", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                        item { OutlinedTextField(value = query, onValueChange = { query = it }, singleLine = true, label = { Text("Search courses, teachers or topics") }, leadingIcon = { Icon(Icons.Default.Search, null) }, modifier = Modifier.fillMaxWidth()) }
                        item {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(listOf("All") + catalog!!.courses.map { it.subject }.distinct()) { value ->
                                    FilterChip(selected = subject == value, onClick = { subject = value }, label = { Text(value) })
                                }
                            }
                        }
                        item {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(listOf("All languages", "English", "Urdu", "Arabic")) { value ->
                                    FilterChip(selected = language == value, onClick = { language = value }, label = { Text(value) })
                                }
                            }
                        }
                        item { Text(if (tab == 1) "Continue your learning" else "Explore courses & programs", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
                        if (visible.isEmpty()) item { Text(if (tab == 1 && enrolled.isEmpty()) "Add a course to begin. Your study log is kept on this device." else "No courses match these filters. Try another subject or language.") }
                        items(visible, key = { it.id }) { c ->
                            AcademyCourseCard(c, catalog!!.providers.first { it.id == c.providerId }.name, c.id in enrolled, completed) { openCourse(c.id) }
                        }
                        item { Text("External classrooms may require a free provider account. Lecture access and certificates remain with the provider. NoorPro is not affiliated with the listed organizations.", style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }
        }
    }
    if (resetDialog && course != null) AlertDialog(
        onDismissRequest = { resetDialog = false }, title = { Text("Reset this course?") },
        text = { Text("This removes this course’s local completion marks and notes. It stays in My learning.") },
        confirmButton = { TextButton(onClick = { resetDialog = false; write("Course progress reset") { store.reset(course) } }) { Text("Reset") } },
        dismissButton = { TextButton(onClick = { resetDialog = false }) { Text("Cancel") } }
    )
}

@Composable
private fun AcademyCourseCard(course: AcademyCourse, provider: String, enrolled: Boolean, completed: Set<String>, onOpen: () -> Unit) {
    Card(onClick = onOpen, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("${course.subject.uppercase()} · ${course.language}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Text(course.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(provider, style = MaterialTheme.typography.labelLarge)
            Text(course.description, style = MaterialTheme.typography.bodyMedium)
            Text(course.accessNote, style = MaterialTheme.typography.bodySmall)
            Text(if (course.format == "video") "${course.lessons.size} licensed in-app lecture" else if (course.format == "reading") "${course.lessons.size} reading lessons with knowledge checks" else if (course.format == "directory") "Official program directory" else "Lectures / lessons on the provider platform", style = MaterialTheme.typography.labelMedium)
            if (enrolled) {
                val count = completedLessonCount(course, completed)
                LinearProgressIndicator(progress = { count.toFloat() / course.lessons.size }, modifier = Modifier.fillMaxWidth())
                Text(if (course.format == "reading") "$count / ${course.lessons.size} lessons" else "Personal study log · ${if (count > 0) "Marked studied" else "Not yet marked"}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun AcademyCourseView(course: AcademyCourse, provider: AcademyProvider, enrolled: Boolean, completed: Set<String>, saving: Boolean,
    modifier: Modifier, onEnroll: () -> Unit, onLesson: (String) -> Unit, onOpenUrl: (String) -> Unit, onReset: () -> Unit, onShare: () -> Unit) {
    LazyColumn(modifier.fillMaxSize().testTag("academy_course"), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text(course.subject.uppercase(), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
            Text(course.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp)); Text(course.description)
            Spacer(Modifier.height(12.dp)); Text("${course.language} · ${course.level}")
            Text(course.accessNote, style = MaterialTheme.typography.bodySmall)
        }
        item {
            Card {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Offered by ${provider.name}", fontWeight = FontWeight.Bold)
                    Text(course.instructor)
                    Text(if (course.format == "reading") "Original reading lessons. No recorded instructor lectures are claimed for this course." else if (course.format == "video") "Licensed lecture streamed inside NoorPro. Credit and license are shown in the study room. No provider affiliation is implied." else "Independent provider. NoorPro links to the official classroom; it does not host these lectures.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item {
            if (!enrolled) Button(onClick = onEnroll, enabled = !saving, modifier = Modifier.fillMaxWidth()) { Text("Add to My learning") }
            else Text("Saved in My learning · ${completedLessonCount(course, completed)} / ${course.lessons.size} activities", color = MaterialTheme.colorScheme.primary)
            if (course.format in setOf("external", "directory")) OutlinedButton(onClick = { onOpenUrl(course.sourceUrl) }, modifier = Modifier.fillMaxWidth()) { Text("Open official classroom") }
        }
        item { Text("What you will explore", style = MaterialTheme.typography.titleLarge); course.outcomes.forEach { Text("• $it", Modifier.padding(top = 8.dp)) } }
        item { Text("Before you begin", style = MaterialTheme.typography.titleLarge); Text(course.prerequisites, Modifier.padding(top = 8.dp)) }
        item { Text(if (course.format in setOf("reading", "video")) "Curriculum" else "Your study activity", style = MaterialTheme.typography.titleLarge) }
        items(course.modules, key = { it.id }) { module ->
            Card {
                Column(Modifier.padding(16.dp)) {
                    Text(module.title, fontWeight = FontWeight.Bold)
                    module.lessons.forEach { lesson ->
                        TextButton(onClick = { onLesson(lesson.id) }, modifier = Modifier.fillMaxWidth()) {
                            Icon(if (lesson.id in completed) Icons.Default.CheckCircle else if (lesson.format == "video") Icons.Default.PlayCircle else if (lesson.format == "reading") Icons.Default.MenuBook else Icons.Default.OpenInNew, null)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(lesson.title, modifier = Modifier.fillMaxWidth())
                                Text(if (lesson.format == "video") "${lesson.minutes} min · In-app video" else if (lesson.minutes > 0) "${lesson.minutes} min · Reading & practice" else "Provider classroom · Self-reported study", style = MaterialTheme.typography.labelSmall, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                }
            }
        }
        item { Text("Reading & resources", style = MaterialTheme.typography.titleLarge) }
        items(course.resources, key = { it.title + it.url }) { resource -> TextButton(onClick = { onOpenUrl(resource.url) }) { Text(resource.title) } }
        item {
            Text(course.certificate, style = MaterialTheme.typography.bodySmall)
            if (course.format == "reading" && isCourseComplete(course, completed)) Button(onClick = onShare) { Text("Share personal study record") }
            if (enrolled) TextButton(onClick = onReset, enabled = !saving) { Text("Reset course progress & notes") }
        }
    }
}

@Composable
private fun AcademyLessonView(course: AcademyCourse, lesson: AcademyLesson, done: Boolean, saving: Boolean, savedNote: String,
    modifier: Modifier, onOpenUrl: (String) -> Unit, onSaveNote: (String) -> Unit, onToggleComplete: () -> Unit, onNext: () -> Unit) {
    var answer by rememberSaveable(lesson.id) { mutableStateOf<Int?>(null) }
    var checked by rememberSaveable(lesson.id) { mutableStateOf(false) }
    var studied by rememberSaveable(lesson.id) { mutableStateOf(false) }
    var note by rememberSaveable(lesson.id) { mutableStateOf(savedNote) }
    val quiz = lesson.quiz
    val canComplete = done || if (quiz == null) studied else checked && answer == quiz.correctIndex
    Column(modifier.fillMaxSize().testTag("academy_lesson").verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text(course.title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text(lesson.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(lesson.body, style = MaterialTheme.typography.bodyLarge)
        lesson.media?.let { media ->
            LecturePlayer(media)
            Text("${lesson.title} — ${media.creator}", style = MaterialTheme.typography.labelLarge)
            Text("${media.license} · ${media.changes}", style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = { onOpenUrl(media.source) }) { Text("Original source & license evidence") }
            TextButton(onClick = { onOpenUrl(media.licenseUrl) }) { Text("View ${media.license} license") }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = studied, onCheckedChange = { studied = it })
                Text("I have studied this lecture. This is my personal study log.", Modifier.weight(1f))
            }
        }
        if (lesson.format == "external") {
            Button(onClick = { onOpenUrl(lesson.url) }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.OpenInNew, null); Spacer(Modifier.width(8.dp)); Text("Open official lectures / lessons") }
            Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = studied, onCheckedChange = { studied = it }); Text("I have studied this material on the provider platform.", Modifier.weight(1f)) }
        }
        lesson.resources.forEach { r -> TextButton(onClick = { onOpenUrl(r.url) }) { Icon(Icons.Default.OpenInNew, null); Spacer(Modifier.width(8.dp)); Text(r.title) } }
        if (quiz != null) {
            Text("Knowledge check", style = MaterialTheme.typography.titleLarge)
            Text(quiz.question)
            quiz.options.forEachIndexed { index, option ->
                OutlinedButton(onClick = { answer = index; checked = false }, modifier = Modifier.fillMaxWidth()) {
                    RadioButton(selected = answer == index, onClick = null)
                    Text(option, Modifier.weight(1f))
                }
            }
            TextButton(onClick = { checked = true }, enabled = answer != null) { Text("Check answer") }
            if (checked) Text(if (answer == quiz.correctIndex) "Correct. ${quiz.explanation}" else "Try again. Revisit the reading and its references.")
        }
        Text("Your notes", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(value = note, onValueChange = { note = it.take(2000) }, label = { Text("A thought or question to revisit") }, modifier = Modifier.fillMaxWidth(), minLines = 3, supportingText = { Text("Stored on this device for this account. Avoid sensitive personal details. ${note.length}/2000") })
        TextButton(onClick = { onSaveNote(note) }, enabled = !saving) { Text("Save note") }
        Button(onClick = onToggleComplete, enabled = canComplete && !saving, modifier = Modifier.fillMaxWidth()) { Text(if (done) "Mark incomplete" else if (lesson.format == "external") "Save my study mark" else "Complete lesson") }
        OutlinedButton(onClick = onNext, modifier = Modifier.fillMaxWidth()) { Text(if (course.lessons.last().id == lesson.id) "Return to course" else "Next lesson") }
        Text(if (lesson.format == "external") "NoorPro cannot verify provider attendance or exams. This mark is a personal log." else "Completion records study activity, not spiritual rank or scholarly qualification.", style = MaterialTheme.typography.bodySmall)
    }
}
