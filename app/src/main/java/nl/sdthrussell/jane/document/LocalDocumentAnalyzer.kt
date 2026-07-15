package nl.sdthrussell.jane.document
import nl.sdthrussell.jane.model.DocumentAnalysisResult
import java.util.Locale

object LocalDocumentAnalyzer {
 private val dates=Regex("""\b(?:\d{1,2}[./-]\d{1,2}[./-]\d{2,4}|\d{4}-\d{2}-\d{2})\b""")
 private val amounts=Regex("""\b(?:€|EUR|RON|lei|euro)?\s?\d{1,3}(?:[., ]\d{3})*(?:[.,]\d{2})?\s?(?:€|EUR|RON|lei|euro)?\b""",RegexOption.IGNORE_CASE)
 fun detectLanguage(text:String):String { val t=" ${text.lowercase(Locale.ROOT)} "; val s=mapOf("ro" to listOf(" și "," pentru "," termen "," în "),"nl" to listOf(" de "," het "," binnen "," gemeente "),"en" to listOf(" the "," and "," within "," please "),"fr" to listOf(" le "," la "," veuillez "," dans ")); return s.maxByOrNull{(_,w)->w.count{t.contains(it)}}?.key?:"unknown" }
 fun analyze(id:String,title:String,text:String)=DocumentAnalysisResult(documentId=id,detectedLanguage=detectLanguage(text),title=title,fullText=text,summary=text.lines().filter{it.isNotBlank()}.take(6).joinToString(" ").take(1200),explanation="Preliminary offline analysis. Connect to main Jane for deep translation, work-context analysis and tailored reply drafting.",deadlines=dates.findAll(text).map{it.value}.distinct().toList(),amounts=amounts.findAll(text).map{it.value.trim()}.filter{v->v.any(Char::isDigit)}.distinct().take(20).toList(),actionsRequired=text.lines().filter{l->listOf("must","required","deadline","trebuie","termen","moet","verplicht").any{l.contains(it,true)}}.take(12),confidence=if(text.length>200)0.68 else 0.4)
}
