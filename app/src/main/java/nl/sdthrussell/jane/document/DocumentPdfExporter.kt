package nl.sdthrussell.jane.document
import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import nl.sdthrussell.jane.model.DocumentAnalysisResult

object DocumentPdfExporter {
 fun export(context:Context,uri:Uri,r:DocumentAnalysisResult){ context.contentResolver.openOutputStream(uri)?.use{out-> val pdf=PdfDocument(); val p=Paint().apply{textSize=11f}; val h=Paint(p).apply{textSize=17f;isFakeBoldText=true}; var n=1; var page=pdf.startPage(PdfDocument.PageInfo.Builder(595,842,n).create()); var y=48f; fun line(t:String,paint:Paint=p){ for(c in t.chunked(82)){ if(y>805){ pdf.finishPage(page); n++; page=pdf.startPage(PdfDocument.PageInfo.Builder(595,842,n).create()); y=48f }; page.canvas.drawText(c,38f,y,paint); y+=paint.textSize+6 }}; line(r.title,h); line("Language: ${r.detectedLanguage}"); r.summary?.let{line("Summary",h);line(it)}; r.translation?.let{line("Translation",h);line(it)}; r.explanation?.let{line("Explanation",h);line(it)}; line("Deadlines",h); r.deadlines.ifEmpty{listOf("None detected")}.forEach{line(it)}; line("Amounts",h); r.amounts.ifEmpty{listOf("None detected")}.forEach{line(it)}; line("Required actions",h); r.actionsRequired.ifEmpty{listOf("None detected")}.forEach{line(it)}; r.replyDraft?.let{line("Draft reply",h);line(it)}; line("Extracted text",h); r.fullText.lines().forEach{line(it)}; pdf.finishPage(page); pdf.writeTo(out); pdf.close() } }
}
