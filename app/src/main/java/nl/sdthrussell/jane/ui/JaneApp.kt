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

private enum class Screen{JANE,DOCUMENTS}
@Composable fun JaneApp(vm:JaneViewModel=viewModel(),dvm:DocumentViewModel=viewModel()){var screen by remember{mutableStateOf(Screen.JANE)}; Scaffold(bottomBar={NavigationBar{NavigationBarItem(screen==Screen.JANE,{screen=Screen.JANE},{Icon(Icons.Outlined.Chat,null)},{Text("Jane")});NavigationBarItem(screen==Screen.DOCUMENTS,{screen=Screen.DOCUMENTS},{Icon(Icons.Outlined.DocumentScanner,null)},{Text("Documents")})}}){pad->Box(Modifier.padding(pad).fillMaxSize()){if(screen==Screen.JANE)JanePane(vm) else DocumentPane(dvm)}}}
@Composable private fun JanePane(vm:JaneViewModel){val s by vm.state.collectAsState(); val c=LocalContext.current; var input by remember{mutableStateOf("")}; lateinit var voice:JaneVoiceController; voice=remember{JaneVoiceController(c,{vm.send(it){r->voice.speak(r)}},vm::setVoiceState)}; DisposableEffect(Unit){onDispose{voice.close()}}; val mic=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){if(it)voice.listen()}; fun listen(){if(ContextCompat.checkSelfPermission(c,Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED)voice.listen() else mic.launch(Manifest.permission.RECORD_AUDIO)}; Column(Modifier.fillMaxSize().padding(12.dp)){Row(Modifier.fillMaxWidth(),Arrangement.SpaceBetween,Alignment.CenterVertically){Column{Text("Jane",style=MaterialTheme.typography.headlineSmall);Text(if(s.connected)s.snapshot.status.currentFocus else "Portable mode",style=MaterialTheme.typography.labelSmall)};Row{IconButton(::listen){Icon(Icons.Outlined.Mic,"Speak")};IconButton(vm::synchronize){Icon(Icons.Outlined.Sync,"Sync")}}};LazyColumn(Modifier.weight(1f),reverseLayout=true,verticalArrangement=Arrangement.spacedBy(8.dp)){items(s.snapshot.messages.reversed(),key={it.id}){m->Card{Column(Modifier.padding(12.dp)){Text(if(m.role=="jane")"Jane" else "You",fontWeight=FontWeight.Bold);Text(m.text)}}}};Row(verticalAlignment=Alignment.Bottom){OutlinedTextField(input,{input=it},Modifier.weight(1f),placeholder={Text("Tell Jane something…")});FilledIconButton({input.trim().takeIf{it.isNotBlank()}?.let{t->input="";vm.send(t){r->voice.speak(r)}}}){Icon(Icons.Outlined.Send,"Send")}}}}
@Composable private fun DocumentPane(vm:DocumentViewModel){val s by vm.state.collectAsState(); val c=LocalContext.current; var uri by remember{mutableStateOf<Uri?>(null)}; val take=rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()){ok->if(ok)uri?.let(vm::addPage)}; val pick=rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()){it.forEach(vm::addPage)}; val pdf=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")){u->if(u!=null&&s.result!=null)DocumentPdfExporter.export(c,u,s.result!!)}; fun scan(){val d=File(c.cacheDir,"documents").apply{mkdirs()};val f=File(d,"page-${System.currentTimeMillis()}.jpg");val u=FileProvider.getUriForFile(c,"${c.packageName}.files",f);uri=u;take.launch(u)};LazyColumn(Modifier.fillMaxSize().padding(12.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){item{Text("Jane Document Workbench",style=MaterialTheme.typography.headlineSmall);Text("Scan letters, translate, summarize, explain, find deadlines and draft replies.")};item{OutlinedTextField(s.title,vm::setTitle,label={Text("Document title")},modifier=Modifier.fillMaxWidth())};item{OutlinedTextField(s.targetLanguage,vm::setTargetLanguage,label={Text("Translate to")},supportingText={Text("en, ro, nl, fr")},modifier=Modifier.fillMaxWidth())};item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Button(::scan){Icon(Icons.Outlined.DocumentScanner,null);Text(" Scan page")};OutlinedButton({pick.launch("image/*")}){Icon(Icons.Outlined.Collections,null);Text(" Import")}}};items(s.pages,key={it.id}){p->Card{Column(Modifier.padding(12.dp)){Row(Modifier.fillMaxWidth(),Arrangement.SpaceBetween){Text("Page ${p.pageNumber}",fontWeight=FontWeight.Bold);IconButton({vm.removePage(p.id)}){Icon(Icons.Outlined.Delete,"Remove")}};OutlinedTextField(p.extractedText,{vm.updatePageText(p.id,it)},Modifier.fillMaxWidth(),label={Text("Recognized text")},minLines=5)}}};item{Button(vm::analyze,enabled=s.pages.isNotEmpty()&&!s.analyzing,modifier=Modifier.fillMaxWidth()){Icon(Icons.Outlined.AutoAwesome,null);Text(" Analyze with Jane")}};s.error?.let{item{Text(it,color=MaterialTheme.colorScheme.error)}};s.result?.let{r->item{Text("Analysis",style=MaterialTheme.typography.titleLarge)};item{Section("Detected language",r.detectedLanguage)};r.summary?.let{item{Section("Summary",it)}};r.translation?.let{item{Section("Translation",it)}};r.explanation?.let{item{Section("Explanation",it)}};if(r.deadlines.isNotEmpty())item{Section("Deadlines",r.deadlines.joinToString("
"))};if(r.amounts.isNotEmpty())item{Section("Amounts",r.amounts.joinToString("
"))};if(r.actionsRequired.isNotEmpty())item{Section("Actions required",r.actionsRequired.joinToString("
"))};r.replyDraft?.let{item{Section("Draft reply",it)}};item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Button({pdf.launch("${r.title}.pdf")}){Icon(Icons.Outlined.PictureAsPdf,null);Text(" Export PDF")};OutlinedButton(vm::reset){Text("New")}}}};if(s.analyzing)item{LinearProgressIndicator(Modifier.fillMaxWidth())}}}
@Composable private fun Section(t:String,b:String){Card{Column(Modifier.padding(12.dp)){Text(t,fontWeight=FontWeight.Bold);Text(b)}}}
