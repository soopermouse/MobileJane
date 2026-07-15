package nl.sdthrussell.jane.ui
import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import nl.sdthrussell.jane.data.JaneApi
import nl.sdthrussell.jane.data.LocalJaneStore
import nl.sdthrussell.jane.document.LocalDocumentAnalyzer
import nl.sdthrussell.jane.model.*
import nl.sdthrussell.jane.vision.JaneOcr

data class DocumentUiState(val title:String="Scanned document",val targetLanguage:String="en",val pages:List<ScannedPage> = emptyList(),val analyzing:Boolean=false,val result:DocumentAnalysisResult?=null,val error:String?=null)
class DocumentViewModel(app:Application):AndroidViewModel(app){ private val store=LocalJaneStore(app); private val ocr=JaneOcr(app); private val _state=MutableStateFlow(DocumentUiState()); val state=_state.asStateFlow(); fun setTitle(v:String){_state.value=_state.value.copy(title=v)}; fun setTargetLanguage(v:String){_state.value=_state.value.copy(targetLanguage=v)}; fun addPage(uri:Uri)=viewModelScope.launch{_state.value=_state.value.copy(analyzing=true,error=null); runCatching{ocr.extract(uri)}.onSuccess{t->_state.value=_state.value.copy(pages=_state.value.pages+ScannedPage(UUID.randomUUID().toString(),uri.toString(),t,_state.value.pages.size+1,if(t.isBlank())0.1 else 0.8),analyzing=false)}.onFailure{_state.value=_state.value.copy(analyzing=false,error=it.message)}}; fun updatePageText(id:String,t:String){_state.value=_state.value.copy(pages=_state.value.pages.map{if(it.id==id)it.copy(extractedText=t) else it})}; fun removePage(id:String){_state.value=_state.value.copy(pages=_state.value.pages.filterNot{it.id==id}.mapIndexed{i,p->p.copy(pageNumber=i+1)})}; fun analyze()=viewModelScope.launch{ val s=_state.value; if(s.pages.isEmpty()){_state.value=s.copy(error="Scan at least one page");return@launch}; _state.value=s.copy(analyzing=true,error=null); val id=UUID.randomUUID().toString(); val req=DocumentAnalysisRequest(id,s.title,targetLanguage=s.targetLanguage,pages=s.pages); val remote=runCatching{JaneApi(store.loadEndpoint()).analyzeDocument(req)}; val result=remote.getOrElse{LocalDocumentAnalyzer.analyze(id,s.title,s.pages.joinToString("

"){it.extractedText})}; _state.value=_state.value.copy(analyzing=false,result=result)}; fun reset(){_state.value=DocumentUiState()}}
