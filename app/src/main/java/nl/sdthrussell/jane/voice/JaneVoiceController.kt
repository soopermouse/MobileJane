package nl.sdthrussell.jane.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import java.util.Locale

class JaneVoiceController(
    private val context: Context,
    private val onText: (String) -> Unit,
    private val onState: (String) -> Unit
) : RecognitionListener, TextToSpeech.OnInitListener {

    private val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
    private val speaker = TextToSpeech(context, this)
    private var speakerReady = false

    init {
        recognizer.setRecognitionListener(this)
    }

    fun listen() {
        onState("Listening")
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        recognizer.startListening(intent)
    }

    fun speak(text: String) {
        if (speakerReady) {
            speaker.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jane-reply")
        }
    }

    fun close() {
        recognizer.destroy()
        speaker.shutdown()
    }

    override fun onInit(status: Int) {
        speakerReady = status == TextToSpeech.SUCCESS
        if (speakerReady) speaker.language = Locale.getDefault()
    }

    override fun onResults(results: Bundle?) {
        results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull()
            ?.takeIf { it.isNotBlank() }
            ?.let(onText)
        onState("Idle")
    }

    override fun onPartialResults(results: Bundle?) {
        results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull()
            ?.let { onState("Hearing: $it") }
    }

    override fun onError(error: Int) = onState("Voice error $error")
    override fun onReadyForSpeech(params: Bundle?) = onState("Listening")
    override fun onBeginningOfSpeech() = onState("Hearing you")
    override fun onEndOfSpeech() = onState("Processing")
    override fun onRmsChanged(rmsdB: Float) = Unit
    override fun onBufferReceived(buffer: ByteArray?) = Unit
    override fun onEvent(eventType: Int, params: Bundle?) = Unit
}
