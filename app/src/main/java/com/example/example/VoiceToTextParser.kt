package com.example.example

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

<<<<<<< Updated upstream
=======
data class LanguageOption(
    val code: String,
    val displayName: String,
    val flag: String,
)

val supportedLanguages = listOf(
    LanguageOption(code = "ms-MY", displayName = "Malay (Bahasa Melayu)", flag = "🇲🇾"),
    LanguageOption(code = "iba-MY", displayName = "Iban (Jaku Iban)", flag = "🇲🇾"),
    LanguageOption(code = "en-US", displayName = "English (US)", flag = "🇺🇸"),
)

/**
 * Contract interface for Voice-To-Text Engines.
 */
interface VoiceToTextEngine {
    val state: StateFlow<VoiceToTextState>
    fun startListening(languageCode: String = state.value.selectedLanguage.code)
    fun stopListening()
    fun setLanguage(language: LanguageOption)
    fun clearText()
    fun clearError()
}

>>>>>>> Stashed changes
data class VoiceToTextState(
    val isListening: Boolean = false,
    val spokenText: String = "",
    val partialText: String = "",
    val error: String? = null,
    val isAvailable: Boolean = true
)

class VoiceToTextParser(private val context: Context) : RecognitionListener {

    private val _state = MutableStateFlow(VoiceToTextState())
    val state: StateFlow<VoiceToTextState> = _state.asStateFlow()

    private var recognizer: SpeechRecognizer? = null

    init {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _state.update { it.copy(isAvailable = false, error = "Speech recognition is not available on this device.") }
        }
    }

    fun startListening(languageCode: String = "en-US") {
        _state.update { it.copy(error = null) }

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _state.update { it.copy(isAvailable = false, error = "Speech recognition unavailable.") }
            return
        }

        stopListening()

        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(this@VoiceToTextParser)
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageCode)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        recognizer?.startListening(intent)
        _state.update { it.copy(isListening = true, error = null) }
    }

    fun stopListening() {
        try {
            recognizer?.stopListening()
            recognizer?.destroy()
        } catch (_: Exception) {
        } finally {
            recognizer = null
            _state.update { it.copy(isListening = false) }
        }
    }

    fun clearText() {
        _state.update { it.copy(spokenText = "", partialText = "", error = null) }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    override fun onReadyForSpeech(params: Bundle?) {
        _state.update { it.copy(error = null, isListening = true) }
    }

    override fun onBeginningOfSpeech() {
        _state.update { it.copy(isListening = true) }
    }

    override fun onRmsChanged(rmsdB: Float) {}

    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {
        _state.update { it.copy(isListening = false) }
    }

    override fun onError(error: Int) {
        if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
            // These are common when user pauses or doesn't speak right away
            _state.update {
                it.copy(
                    isListening = false,
                    partialText = "",
                    error = getErrorMessage(error)
                )
            }
        } else {
            _state.update {
                it.copy(
                    isListening = false,
                    partialText = "",
                    error = getErrorMessage(error)
                )
            }
        }
    }

    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val text = matches?.getOrNull(0)
        if (!text.isNullOrEmpty()) {
            _state.update { current ->
                val updatedText = if (current.spokenText.isBlank()) {
                    text
                } else {
                    "${current.spokenText} $text"
                }
                current.copy(
                    spokenText = updatedText,
                    partialText = "",
                    isListening = false
                )
            }
        } else {
            _state.update { it.copy(isListening = false, partialText = "") }
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val text = matches?.getOrNull(0)
        if (!text.isNullOrEmpty()) {
            _state.update { it.copy(partialText = text) }
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}

    private fun getErrorMessage(errorCode: Int): String {
        return when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Please try speaking again."
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer is busy. Try again."
            SpeechRecognizer.ERROR_SERVER -> "Server error"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input detected."
            else -> "Speech recognition error ($errorCode)"
        }
    }
}
