package app.curser.ui.keyboard

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/** In-app recognizer: a separate recognizer activity would stop MainActivity, and onStop disconnects the host. */
class VoiceInput internal constructor(private val context: Context) {
    var listening by mutableStateOf(false)
        private set
    internal var onText: (String) -> Unit = {}
    internal var onMessage: (String) -> Unit = {}
    internal var requestPermission: () -> Unit = {}

    private val recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
        setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                listening = false
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (text.isNullOrBlank()) onMessage("Didn't catch that") else onText(text.trim() + " ")
            }

            override fun onError(error: Int) {
                listening = false
                onMessage("Didn't catch that")
            }

            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    fun toggle() {
        when {
            listening -> recognizer.stopListening()
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED -> start()
            else -> requestPermission()
        }
    }

    internal fun start() {
        listening = true
        recognizer.startListening(
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM),
        )
    }

    internal fun destroy() = recognizer.destroy()
}

/** null when the device has no recognition service; the mic key is then hidden. */
@Composable
fun rememberVoiceInput(onText: (String) -> Unit, onMessage: (String) -> Unit): VoiceInput? {
    val context = LocalContext.current
    if (!SpeechRecognizer.isRecognitionAvailable(context)) return null
    val voice = remember { VoiceInput(context) }
    voice.onText = rememberUpdatedState(onText).value
    voice.onMessage = rememberUpdatedState(onMessage).value
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) voice.start() else onMessage("Microphone permission needed")
    }
    voice.requestPermission = { launcher.launch(Manifest.permission.RECORD_AUDIO) }
    DisposableEffect(voice) { onDispose { voice.destroy() } }
    return voice
}
