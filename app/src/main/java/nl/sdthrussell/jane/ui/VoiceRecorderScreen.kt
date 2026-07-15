package nl.sdthrussell.jane.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Hearing
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import nl.sdthrussell.jane.data.VoiceRecordingRepository
import nl.sdthrussell.jane.voice.VoiceActivatedRecorderService

@Composable
fun VoiceRecorderScreen(){
    val context=LocalContext.current; val repo=remember{VoiceRecordingRepository(context)}
    var active by remember{mutableStateOf(false)}; var recordings by remember{mutableStateOf(repo.list())}
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){granted-> if(granted){
        ContextCompat.startForegroundService(context,Intent(context,VoiceActivatedRecorderService::class.java).apply{action=VoiceActivatedRecorderService.ACTION_START}); active=true
    }}
    fun start(){ if(ContextCompat.checkSelfPermission(context,Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED){ ContextCompat.startForegroundService(context,Intent(context,VoiceActivatedRecorderService::class.java).apply{action=VoiceActivatedRecorderService.ACTION_START}); active=true } else permission.launch(Manifest.permission.RECORD_AUDIO) }
    fun stop(){ context.startService(Intent(context,VoiceActivatedRecorderService::class.java).apply{action=VoiceActivatedRecorderService.ACTION_STOP}); active=false; recordings=repo.list() }
    LaunchedEffect(active){ while(active){ kotlinx.coroutines.delay(2000); recordings=repo.list() } }
    LazyColumn(Modifier.fillMaxSize().padding(12.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
        item{ Text("Voice-activated recorder",style=MaterialTheme.typography.headlineSmall); Text("Records locally when speech is detected and stops after silence. Android shows a persistent microphone notification while active.") }
        item{ Button(onClick=if(active)::stop else ::start,modifier=Modifier.fillMaxWidth()){ Icon(if(active)Icons.Outlined.Stop else Icons.Outlined.Hearing,null); Spacer(Modifier.width(8.dp)); Text(if(active)"Stop voice activation" else "Enable voice activation") } }
        items(recordings,key={it.path}){r-> Card{Column(Modifier.padding(12.dp)){Text(r.path.substringAfterLast('/'),fontWeight=FontWeight.Bold);Text("${r.durationMs/1000.0} sec · ${r.bytes/1024} KB");TextButton(onClick={repo.delete(r);recordings=repo.list()}){Icon(Icons.Outlined.Delete,null);Text("Delete")}}} }
    }
}
