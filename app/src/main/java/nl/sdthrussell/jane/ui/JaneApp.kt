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
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import java.io.File
import nl.sdthrussell.jane.document.DocumentPdfExporter
import nl.sdthrussell.jane.voice.JaneVoiceController

private enum class Screen { JANE, DOCUMENTS, RECORDER }

@Composable
fun JaneApp(
    vm: JaneViewModel = viewModel(),
    documentVm: DocumentViewModel = viewModel()
) {
    var screen by remember { mutableStateOf(Screen.JANE) }
    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(screen == Screen.JANE, { screen = Screen.JANE }, { Icon(Icons.Outlined.Chat, null) }, { Text("Jane") })
                NavigationBarItem(screen == Screen.DOCUMENTS, { screen = Screen.DOCUMENTS }, { Icon(Icons.Outlined.DocumentScanner, null) }, { Text("Documents") })
                NavigationBarItem(screen == Screen.RECORDER, { screen = Screen.RECORDER }, { Icon(Icons.Outlined.Mic, null) }, { Text("Recorder") })
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (screen) {
                Screen.JANE -> JanePane(vm)
                Screen.DOCUMENTS -> DocumentPane(documentVm)
                Screen.RECORDER -> VoiceRecorderScreen()
            }
        }
    }
}

@Composable
private fun JanePane(vm: JaneViewModel) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    var input by remember { mutableStateOf("") }
    lateinit var voice: JaneVoiceController
    voice = remember {
        JaneVoiceController(context, { vm.send(it) { reply -> voice.speak(reply) } }, vm::setVoiceState)
    }
    DisposableEffect(Unit) { onDispose { voice.close() } }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { if (it) voice.listen() }
    fun listen() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) voice.listen()
        else permission.launch(Manifest.permission.RECORD_AUDIO)
    }
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Column {
                Text("Jane", style = MaterialTheme.typography.headlineSmall)
                Text(if (state.connected) state.snapshot.status.currentFocus else "Portable mode", style = MaterialTheme.typography.labelSmall)
            }
            Row {
                IconButton(::listen) { Icon(Icons.Outlined.Mic, "Speak") }
                IconButton(vm::synchronize) { Icon(Icons.Outlined.Sync, "Synchronize") }
            }
        }
        LazyColumn(Modifier.weight(1f), reverseLayout = true, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.snapshot.messages.reversed(), key = { it.id }) { message ->
                Card {
                    Column(Modifier.padding(12.dp)) {
                        Text(if (message.role == "jane") "Jane" else "You", fontWeight = FontWeight.Bold)
                        Text(message.text)
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.Bottom) {
            OutlinedTextField(input, { input = it }, Modifier.weight(1f), placeholder = { Text("Tell Jane something…") })
            FilledIconButton({
                input.trim().takeIf { it.isNotBlank() }?.let { text ->
                    input = ""
                    vm.send(text) { reply -> voice.speak(reply) }
                }
            }) { Icon(Icons.Outlined.Send, "Send") }
        }
    }
}

@Composable
private fun DocumentPane(vm: DocumentViewModel) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok -> if (ok) photoUri?.let(vm::addPage) }
    val pickImages = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris -> uris.forEach(vm::addPage) }
    val exportPdf = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null && state.result != null) DocumentPdfExporter.export(context, uri, state.result!!)
    }
    fun scan() {
        val dir = File(context.cacheDir, "documents").apply { mkdirs() }
        val file = File(dir, "page-${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        photoUri = uri
        takePhoto.launch(uri)
    }
    LazyColumn(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("Jane Document Workbench", style = MaterialTheme.typography.headlineSmall); Text("Scan letters, translate, summarize, explain, find deadlines and draft replies.") }
        item { OutlinedTextField(state.title, vm::setTitle, label = { Text("Document title") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(state.targetLanguage, vm::setTargetLanguage, label = { Text("Translate to") }, supportingText = { Text("en, ro, nl, fr") }, modifier = Modifier.fillMaxWidth()) }
        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Button(::scan) { Icon(Icons.Outlined.DocumentScanner, null); Text(" Scan page") }; OutlinedButton({ pickImages.launch("image/*") }) { Icon(Icons.Outlined.Collections, null); Text(" Import") } } }
        items(state.pages, key = { it.id }) { page ->
            Card { Column(Modifier.padding(12.dp)) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text("Page ${page.pageNumber}", fontWeight = FontWeight.Bold); IconButton({ vm.removePage(page.id) }) { Icon(Icons.Outlined.Delete, "Remove") } }
                OutlinedTextField(page.extractedText, { vm.updatePageText(page.id, it) }, Modifier.fillMaxWidth(), label = { Text("Recognized text") }, minLines = 5)
            } }
        }
        item { Button(vm::analyze, enabled = state.pages.isNotEmpty() && !state.analyzing, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Outlined.AutoAwesome, null); Text(" Analyze with Jane") } }
        state.error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
        state.result?.let { result ->
            item { Text("Analysis", style = MaterialTheme.typography.titleLarge) }
            item { Section("Detected language", result.detectedLanguage) }
            result.summary?.let { item { Section("Summary", it) } }
            result.translation?.let { item { Section("Translation", it) } }
            result.explanation?.let { item { Section("Explanation", it) } }
            if (result.deadlines.isNotEmpty()) item { Section("Deadlines", result.deadlines.joinToString("\n")) }
            if (result.amounts.isNotEmpty()) item { Section("Amounts", result.amounts.joinToString("\n")) }
            if (result.actionsRequired.isNotEmpty()) item { Section("Actions required", result.actionsRequired.joinToString("\n")) }
            result.replyDraft?.let { item { Section("Draft reply", it) } }
            item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Button({ exportPdf.launch("${result.title}.pdf") }) { Icon(Icons.Outlined.PictureAsPdf, null); Text(" Export PDF") }; OutlinedButton(vm::reset) { Text("New") } } }
        }
        if (state.analyzing) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
    }
}

@Composable
private fun Section(title: String, body: String) {
    Card { Column(Modifier.padding(12.dp)) { Text(title, fontWeight = FontWeight.Bold); Text(body) } }
}
