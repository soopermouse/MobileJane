package nl.sdthrussell.jane.voice

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import java.util.Calendar
import java.util.Locale

/** Lightweight local intent router. Jane Agent remains the fallback for open-ended language. */
class JaneVoiceCommands(private val context: Context) {
    enum class Destination { DASHBOARD, CHAT, PROJECTS, DOCUMENTS, MORE }
    data class Result(
        val handled: Boolean,
        val reply: String? = null,
        val destination: Destination? = null,
        val agentInstruction: String? = null,
        val documentLanguage: String? = null
    )

    fun route(raw: String): Result {
        val text = raw.trim()
        val lower = text.lowercase(Locale.getDefault())

        if ((lower.contains("show") && lower.contains("alert")) || lower.contains("urgent alert"))
            return Result(true, "Opening your dashboard and Jane Alerts.", Destination.DASHBOARD)
        if ((lower.contains("show") && lower.contains("project")) || lower == "projects")
            return Result(true, "Opening your projects.", Destination.PROJECTS)

        if (lower.contains("scan") || (lower.contains("document") && (lower.contains("translate") || lower.contains("ocr")))) {
            val language = Regex("translate(?: it| this)? (?:to|into) ([a-zA-Z -]+?)(?: and|,|$)", RegexOption.IGNORE_CASE)
                .find(text)?.groupValues?.getOrNull(1)?.trim()
            val project = Regex("(?:add|save|put)(?: it| this| everything)? (?:to|in) (?:project )?([\\w ._-]+)", RegexOption.IGNORE_CASE)
                .find(text)?.groupValues?.getOrNull(1)?.trim()
            val instruction = project?.let { "When the mobile document analysis is complete, associate the document and its analysis with project '$it'. Original voice request: $text" }
            return Result(true, "Opening the document scanner${language?.let { " and setting translation to $it" } ?: ""}.", Destination.DOCUMENTS, instruction, language)
        }

        if (lower.contains("meeting") && (lower.contains("calendar") || lower.startsWith("add ") || lower.startsWith("schedule "))) {
            openCalendar(text)
            return Result(true, "I've opened a calendar event with the details I could extract. Review it and tap Save.")
        }

        if ((lower.startsWith("add") || lower.startsWith("save") || lower.startsWith("put")) && lower.contains("project")) {
            return Result(true, "I'll add that to the project through Jane.", agentInstruction = "Treat this as a requested mobile project action. Resolve the target project, enforce Jane Agent authorization and human-approval policy where required, then perform only permitted changes: $text")
        }

        if (lower.contains("what needs my attention") || lower.contains("what's blocked") || lower.contains("what is blocked") || lower.contains("next actions"))
            return Result(true, agentInstruction = "Answer this using my current projects and Jane Alert state: $text")

        return Result(false)
    }

    private fun openCalendar(text: String) {
        val lower = text.lowercase(Locale.getDefault())
        val start = Calendar.getInstance().apply {
            if (lower.contains("tomorrow")) add(Calendar.DAY_OF_YEAR, 1)
            val time = Regex("\\b(?:at\\s+)(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?\\b|\\b(\\d{1,2}):(\\d{2})\\s*(am|pm)?\\b|\\b(\\d{1,2})\\s*(am|pm)\\b", RegexOption.IGNORE_CASE)
                .find(text)?.let { m ->
                    val h = (m.groupValues[1].ifBlank { m.groupValues[4] }.ifBlank { m.groupValues[7] }).toIntOrNull()
                    val minute = (m.groupValues[2].ifBlank { m.groupValues[5] }).toIntOrNull() ?: 0
                    val ap = m.groupValues[3].ifBlank { m.groupValues[6] }.ifBlank { m.groupValues[8] }.lowercase()
                    if (h != null && h in 0..23 && minute in 0..59) Triple(h, minute, ap) else null
                }
            time?.let { (rawHour, minute, ap) ->
                var hour = rawHour
                if (ap == "pm" && hour < 12) hour += 12
                if (ap == "am" && hour == 12) hour = 0
                set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute); set(Calendar.SECOND, 0)
            }
        }
        val title = text
            .replace(Regex("(?i)\\b(add|schedule|put|create)\\b"), "")
            .replace(Regex("(?i)\\b(to|in|on) my calendar\\b"), "")
            .replace(Regex("(?i)\\b(today|tomorrow)\\b"), "")
            .replace(Regex("(?i)\\bat \\d{1,2}(?::\\d{2})?\\s*(am|pm)?\\b"), "")
            .trim(' ', ',', '.')
            .ifBlank { "Meeting" }
        val end = start.timeInMillis + 60 * 60 * 1000L
        val intent = Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI)
            .putExtra(CalendarContract.Events.TITLE, title)
            .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start.timeInMillis)
            .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, end)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
