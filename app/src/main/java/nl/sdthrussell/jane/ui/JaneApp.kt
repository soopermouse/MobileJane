package nl.sdthrussell.jane.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import java.io.File
import java.text.DateFormat
import java.util.Date
import nl.sdthrussell.jane.model.*
import nl.sdthrussell.jane.voice.JaneVoiceController
import nl.sdthrussell.jane.voice.JaneVoiceCommands

private enum class Screen { HOME, CHAT, PROJECTS, DOCUMENTS, MORE }

@Composable
fun JaneApp(vm: JaneViewModel = viewModel(), dvm: DocumentViewModel = viewModel()) {
    var screen by remember { mutableStateOf(Screen.HOME) }
    val state by vm.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Jane Mobile") },
                actions = {
                    AssistChip(
                        onClick = vm::refresh,
                        label = { Text(if (state.connected) "Connected" else "Portable") },
                        leadingIcon = {
                            Icon(
                                if (state.connected) Icons.Outlined.CloudDone else Icons.Outlined.CloudOff,
                                contentDescription = null
                            )
                        }
                    )
                    Spacer(Modifier.width(8.dp))
                }
            )
        },
        bottomBar = {
            NavigationBar {
                listOf(
                    Triple(Screen.HOME, "Dashboard", Icons.Outlined.Home),
                    Triple(Screen.CHAT, "Jane", Icons.Outlined.Chat),
                    Triple(Screen.PROJECTS, "Projects", Icons.Outlined.Workspaces),
                    Triple(Screen.DOCUMENTS, "Documents", Icons.Outlined.DocumentScanner),
                    Triple(Screen.MORE, "More", Icons.Outlined.MoreHoriz)
                ).forEach { (target, label, icon) ->
                    NavigationBarItem(
                        selected = screen == target,
                        onClick = { screen = target },
                        icon = { Icon(icon, label) },
                        label = { Text(label) }
                    )
                }
            }
        },
        snackbarHost = {
            state.error?.let { message ->
                Snackbar(
                    action = { TextButton(vm::clearError) { Text("Dismiss") } },
                    modifier = Modifier.padding(12.dp)
                ) { Text(message) }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (screen) {
                Screen.HOME -> HomePane(vm, onOpenChat = { screen = Screen.CHAT }, onOpenProjects = { screen = Screen.PROJECTS }, onOpenDocuments = { screen = Screen.DOCUMENTS })
                Screen.CHAT -> ChatPane(vm, dvm) { destination ->
                    screen = when (destination) {
                        JaneVoiceCommands.Destination.DASHBOARD -> Screen.HOME
                        JaneVoiceCommands.Destination.CHAT -> Screen.CHAT
                        JaneVoiceCommands.Destination.PROJECTS -> Screen.PROJECTS
                        JaneVoiceCommands.Destination.DOCUMENTS -> Screen.DOCUMENTS
                        JaneVoiceCommands.Destination.MORE -> Screen.MORE
                    }
                }
                Screen.PROJECTS -> ProjectsPane(vm)
                Screen.DOCUMENTS -> DocumentPane(dvm)
                Screen.MORE -> MorePane(vm)
            }
            if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter))
        }
    }
}

@Composable
private fun HomePane(
    vm: JaneViewModel,
    onOpenChat: () -> Unit,
    onOpenProjects: () -> Unit,
    onOpenDocuments: () -> Unit
) {
    val state by vm.state.collectAsState()
    var capture by remember { mutableStateOf("") }
    val projects = state.snapshot.projects.sortedWith(
        compareBy<ProjectSummary> { priorityRank(it.metadata["priority"]) }
            .thenBy { it.status.lowercase() != "active" }
            .thenBy { it.name.lowercase() }
    )
    val activeProjects = projects.count { it.status.equals("active", true) }
    val blockedProjects = projects.count { !it.metadata["blocker"].isNullOrBlank() }
    val openAlerts = state.snapshot.alerts.filterNot { it.status.equals("resolved", true) || it.status.equals("closed", true) }
    val urgentAlerts = openAlerts.count { it.severity.equals("critical", true) || it.severity.equals("high", true) }
    val openGoals = state.snapshot.goals.count { !it.completed }

    LazyColumn(
        Modifier.fillMaxSize().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("${greeting()}, Simona", style = MaterialTheme.typography.headlineLarge)
            Text(
                if (state.connected) "Your Jane workspace is synchronized." else "Portable mode: your local workspace remains available.",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        item {
            Text("Project control", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DashboardMetric("Projects", projects.size.toString(), Modifier.weight(1f))
                DashboardMetric("Active", activeProjects.toString(), Modifier.weight(1f))
                DashboardMetric("Blocked", blockedProjects.toString(), Modifier.weight(1f))
                DashboardMetric("Alerts", openAlerts.size.toString(), Modifier.weight(1f))
            }
            if (openGoals > 0) {
                Spacer(Modifier.height(6.dp))
                Text("$openGoals open goals", style = MaterialTheme.typography.labelMedium)
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Column {
                    Text("Jane Alert", style = MaterialTheme.typography.titleLarge)
                    Text(
                        if (openAlerts.isEmpty()) "No open alerts" else "$urgentAlerts urgent · ${openAlerts.size} open",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                TextButton(vm::refresh) { Text("Refresh") }
            }
        }
        if (openAlerts.isEmpty()) {
            item { EmptyCard("Jane Alert has nothing requiring your attention.") }
        } else {
            items(openAlerts.sortedWith(compareBy<JaneAlertSummary> { alertSeverityRank(it.severity) }.thenByDescending { it.createdAt }).take(8), key = { it.id.ifBlank { it.title + it.createdAt } }) { alert ->
                MobileAlertCard(alert, projects.firstOrNull { it.id == alert.projectId }?.name)
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("All projects", style = MaterialTheme.typography.titleLarge)
                TextButton(onOpenProjects) { Text("Manage") }
            }
        }
        if (projects.isEmpty()) {
            item { EmptyCard("No projects loaded from Jane Agent yet.") }
        } else {
            items(projects, key = { it.id.ifBlank { it.name } }) { project ->
                MobileProjectDashboardCard(project, openAlerts.count { it.projectId == project.id }, onOpenChat)
            }
        }

        item {
            Text("Quick actions", style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onOpenChat, Modifier.weight(1f)) { Icon(Icons.Outlined.Mic, null); Text(" Jane") }
                FilledTonalButton(onOpenDocuments, Modifier.weight(1f)) { Icon(Icons.Outlined.DocumentScanner, null); Text(" Scan") }
            }
        }

        item {
            Card {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Capture on the go", fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        capture,
                        { capture = it },
                        Modifier.fillMaxWidth(),
                        placeholder = { Text("Idea, decision, reminder or observation") }
                    )
                    Button(
                        onClick = { vm.capture("idea", capture); capture = "" },
                        enabled = capture.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) { Icon(Icons.Outlined.Lightbulb, null); Text(" Save to Jane") }
                }
            }
        }
    }
}

@Composable
private fun DashboardMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun MobileAlertCard(alert: JaneAlertSummary, projectName: String?) {
    Card {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text(alert.title, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                AssistChip(onClick = {}, label = { Text(alert.severity.uppercase()) })
            }
            if (!projectName.isNullOrBlank()) Text(projectName, style = MaterialTheme.typography.labelMedium)
            if (alert.message.isNotBlank()) Text(alert.message)
            if (alert.createdAt > 0) Text(formatTime(alert.createdAt), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun MobileProjectDashboardCard(project: ProjectSummary, alertCount: Int, onOpenChat: () -> Unit) {
    val priority = project.metadata["priority"]?.takeIf { it.isNotBlank() } ?: "normal"
    val nextAction = project.metadata["next_action"]?.takeIf { it.isNotBlank() }
    val blocker = project.metadata["blocker"]?.takeIf { it.isNotBlank() }
    val lastActivity = project.metadata["last_activity"]?.takeIf { it.isNotBlank() }

    Card {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(project.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    if (project.description.isNotBlank()) Text(project.description, maxLines = 2, style = MaterialTheme.typography.bodySmall)
                }
                AssistChip(onClick = {}, label = { Text(priority.uppercase()) })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SuggestionChip(onClick = {}, label = { Text(project.status) })
                if (alertCount > 0) SuggestionChip(onClick = {}, label = { Text("$alertCount alert${if (alertCount == 1) "" else "s"}") })
            }
            blocker?.let {
                Text("BLOCKER", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
            nextAction?.let {
                Text("NEXT", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
            lastActivity?.let { Text("Last activity: $it", style = MaterialTheme.typography.labelSmall) }
            TextButton(onOpenChat, contentPadding = PaddingValues(0.dp)) { Text("Continue with Jane") }
        }
    }
}

private fun priorityRank(priority: String?): Int = when (priority?.lowercase()) {
    "critical", "urgent" -> 0
    "high" -> 1
    "medium" -> 2
    "normal" -> 3
    "low" -> 4
    else -> 3
}

private fun alertSeverityRank(severity: String): Int = when (severity.lowercase()) {
    "critical" -> 0
    "high", "error" -> 1
    "warning", "medium" -> 2
    "info", "low" -> 3
    else -> 4
}

@Composable
private fun ChatPane(vm: JaneViewModel, dvm: DocumentViewModel, onNavigate: (JaneVoiceCommands.Destination) -> Unit) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    var input by remember { mutableStateOf("") }
    val commands = remember { JaneVoiceCommands(context) }
    lateinit var voice: JaneVoiceController
    fun dispatch(text: String) {
        val result = commands.route(text)
        if (!result.handled) {
            vm.send(text) { reply -> voice.speak(reply) }
            return
        }
        result.documentLanguage?.let(dvm::setTargetLanguage)
        result.agentInstruction?.let { instruction -> vm.send(instruction) { reply -> voice.speak(reply) } }
        result.destination?.let(onNavigate)
        result.reply?.let(voice::speak)
    }
    voice = remember { JaneVoiceController(context, ::dispatch, vm::setVoiceState) }
    DisposableEffect(Unit) { onDispose { voice.close() } }
    val mic = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { if (it) voice.listen() }
    fun listen() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) voice.listen()
        else mic.launch(Manifest.permission.RECORD_AUDIO)
    }

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Column { Text("Talk to Jane", style = MaterialTheme.typography.headlineSmall); Text(state.voiceState, style = MaterialTheme.typography.labelSmall) }
            IconButton(::listen) { Icon(Icons.Outlined.Mic, "Speak") }
        }
        LazyColumn(Modifier.weight(1f), reverseLayout = true, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.snapshot.messages.reversed(), key = { it.id }) { message ->
                Card { Column(Modifier.padding(12.dp)) {
                    Text(if (message.role == "jane") "Jane" else "You", fontWeight = FontWeight.Bold)
                    Text(message.text)
                    Text(formatTime(message.timestamp), style = MaterialTheme.typography.labelSmall)
                } }
            }
        }
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(input, { input = it }, Modifier.weight(1f), placeholder = { Text("Tell Jane something…") })
            FilledIconButton({ input.trim().takeIf { it.isNotBlank() }?.let { text -> input = ""; dispatch(text) } }) {
                Icon(Icons.Outlined.Send, "Send")
            }
        }
    }
}

@Composable
private fun ProjectsPane(vm: JaneViewModel) {
    val state by vm.state.collectAsState()
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    LazyColumn(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("Projects", style = MaterialTheme.typography.headlineSmall); Text("Projects are synchronized with Jane Agent.") }
        item {
            Card {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("Project name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(description, { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                    Button({ vm.addProject(name, description); name = ""; description = "" }, enabled = name.isNotBlank() && !state.loading, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Outlined.Add, null); Text(" Save project")
                    }
                }
            }
        }
        if (state.snapshot.projects.isEmpty()) item { EmptyCard("No projects found.") }
        items(state.snapshot.projects.sortedBy { it.name.lowercase() }, key = { it.id.ifBlank { it.name } }) { project -> ProjectCard(project) }
    }
}

@Composable
private fun ProjectCard(project: ProjectSummary) {
    Card { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(project.name, fontWeight = FontWeight.Bold)
        if (project.description.isNotBlank()) Text(project.description)
        AssistChip(onClick = {}, label = { Text(project.status) })
    } }
}

@Composable
private fun MorePane(vm: JaneViewModel) {
    val state by vm.state.collectAsState()
    var goal by remember { mutableStateOf("") }
    var investigation by remember { mutableStateOf("") }
    var endpoint by remember(state.endpoint) { mutableStateOf(state.endpoint) }
    var bearerToken by remember { mutableStateOf("") }

    LazyColumn(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Jane workspace", style = MaterialTheme.typography.headlineSmall) }
        item { Text("Goals", style = MaterialTheme.typography.titleLarge) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(goal, { goal = it }, Modifier.weight(1f), placeholder = { Text("New goal") })
                FilledIconButton({ vm.addGoal(goal); goal = "" }, enabled = goal.isNotBlank()) { Icon(Icons.Outlined.Add, "Add goal") }
            }
        }
        if (state.snapshot.goals.isEmpty()) item { EmptyCard("No goals yet.") }
        items(state.snapshot.goals.sortedBy { it.completed }, key = { it.id }) { item ->
            ListItem(
                headlineContent = { Text(item.title) },
                supportingContent = { Text(if (item.completed) "Completed" else "Active") },
                leadingContent = { Checkbox(item.completed, { vm.toggleGoal(item.id) }) }
            )
        }
        item { HorizontalDivider(); Text("Investigations", style = MaterialTheme.typography.titleLarge) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(investigation, { investigation = it }, Modifier.weight(1f), placeholder = { Text("Question to investigate") })
                FilledIconButton({ vm.addInvestigation(investigation); investigation = "" }, enabled = investigation.isNotBlank()) { Icon(Icons.Outlined.Search, "Start investigation") }
            }
        }
        if (state.snapshot.investigations.isEmpty()) item { EmptyCard("No investigations yet.") }
        items(state.snapshot.investigations.reversed(), key = { it.id }) { item ->
            Card { Column(Modifier.padding(12.dp)) {
                Text(item.question, fontWeight = FontWeight.Bold)
                Text(item.status, style = MaterialTheme.typography.labelSmall)
            } }
        }
        item { HorizontalDivider(); Text("Connection", style = MaterialTheme.typography.titleLarge) }
        item { OutlinedTextField(endpoint, { endpoint = it }, label = { Text("Jane Agent URL") }, modifier = Modifier.fillMaxWidth()) }
        item {
            OutlinedTextField(
                bearerToken,
                { bearerToken = it },
                label = { Text(if (state.authConfigured) "Bearer token (stored securely; leave blank to keep current)" else "Bearer token") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item { Text("Production endpoints must use HTTPS. Debug emulator access may use http://10.0.2.2:8000 only.") }
        item { Button({ vm.saveConnection(endpoint, bearerToken); bearerToken = "" }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Outlined.Wifi, null); Text(" Save secure connection and test") } }
        item {
            Card { Column(Modifier.padding(14.dp)) {
                Text("Compatibility", fontWeight = FontWeight.Bold)
                Text("Jane Agent API v4.1.1")
                Text("JaneOS v4 through Jane Agent")
                Text("Mobile structured goals and investigations are local until matching Agent endpoints are published.")
            } }
        }
    }
}

@Composable
private fun DocumentPane(vm: DocumentViewModel) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    var pendingUri by remember { mutableStateOf<Uri?>(null) }
    val take = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok -> if (ok) pendingUri?.let(vm::addPage) }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris -> uris.forEach(vm::addPage) }
    fun scan() {
        val directory = File(context.cacheDir, "documents").apply { mkdirs() }
        val file = File(directory, "page-${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        pendingUri = uri
        take.launch(uri)
    }

    LazyColumn(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("Document Workbench", style = MaterialTheme.typography.headlineSmall); Text("Scan, OCR, translate, explain and extract actions.") }
        item { OutlinedTextField(state.title, vm::setTitle, label = { Text("Document title") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(state.targetLanguage, vm::setTargetLanguage, label = { Text("Translate to") }, modifier = Modifier.fillMaxWidth()) }
        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(::scan) { Icon(Icons.Outlined.DocumentScanner, null); Text(" Scan") }
            OutlinedButton({ pick.launch("image/*") }) { Icon(Icons.Outlined.Collections, null); Text(" Import") }
        } }
        items(state.pages, key = { it.id }) { page ->
            Card { Column(Modifier.padding(12.dp)) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Text("Page ${page.pageNumber}", fontWeight = FontWeight.Bold)
                    IconButton({ vm.removePage(page.id) }) { Icon(Icons.Outlined.Delete, "Remove") }
                }
                OutlinedTextField(page.extractedText, { vm.updatePageText(page.id, it) }, Modifier.fillMaxWidth(), minLines = 5)
            } }
        }
        item { Button(vm::analyze, enabled = state.pages.isNotEmpty() && !state.analyzing, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Outlined.AutoAwesome, null); Text(" Analyze with Jane") } }
        state.error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
        state.result?.let { result ->
            item { Text("Analysis", style = MaterialTheme.typography.titleLarge) }
            item { Section("Summary", result.summary) }
            result.translation?.takeIf { it.isNotBlank() }?.let { item { Section("Translation", it) } }
            result.explanation?.takeIf { it.isNotBlank() }?.let { item { Section("Explanation", it) } }
            if (result.deadlines.isNotEmpty()) item { Section("Deadlines", result.deadlines.joinToString("\n")) }
            if (result.amounts.isNotEmpty()) item { Section("Amounts", result.amounts.joinToString("\n")) }
            if (result.actionsRequired.isNotEmpty()) item { Section("Actions required", result.actionsRequired.joinToString("\n")) }
            if (result.risks.isNotEmpty()) item { Section("Risks", result.risks.joinToString("\n")) }
            result.replyDraft?.takeIf { it.isNotBlank() }?.let { item { Section("Draft reply", it) } }
            item { OutlinedButton(vm::reset, modifier = Modifier.fillMaxWidth()) { Text("New document") } }
        }
        if (state.analyzing) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
    }
}

@Composable
private fun Section(title: String, body: String) {
    Card { Column(Modifier.padding(12.dp)) { Text(title, fontWeight = FontWeight.Bold); Text(body.ifBlank { "No result" }) } }
}

@Composable
private fun EmptyCard(text: String) {
    Card { Text(text, Modifier.padding(14.dp), style = MaterialTheme.typography.bodyMedium) }
}

private fun greeting(): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "Good morning"
        in 12..17 -> "Good afternoon"
        else -> "Good evening"
    }
}

private fun formatTime(timestamp: Long): String = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(timestamp))
