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

/**
 * Contract interface for Voice-To-Text Engines.
 * Allows swapping Android SpeechRecognizer with offline models (e.g., Whisper) or Cloud APIs.
 */
interface VoiceToTextEngine {
    val state: StateFlow<VoiceToTextState>
    fun startListening(languageCode: String = "en-US")
    fun stopListening()
    fun clearText()
    fun clearError()
}

data class VoiceToTextState(
    val isListening: Boolean = false,
    val spokenText: String = "",
    val partialText: String = "",
    val error: String? = null,
    val isAvailable: Boolean = true,
)

class VoiceToTextParser(private val context: Context) : VoiceToTextEngine, RecognitionListener {

    private val _state = MutableStateFlow(VoiceToTextState())
    override val state: StateFlow<VoiceToTextState> = _state.asStateFlow()

    private var recognizer: SpeechRecognizer? = null

    init {
        try {
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                _state.update { it.copy(isAvailable = false, error = "Speech recognition is not available on this device.") }
            }
        } catch (_: Exception) {
            _state.update { it.copy(isAvailable = false, error = "Failed to initialize speech recognition.") }
        }
    }

    override fun startListening(languageCode: String) {
        _state.update { it.copy(error = null) }

        try {
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                _state.update { it.copy(isAvailable = false, error = "Speech recognition unavailable on this device.") }
                return
            }

            stopListening()

            val newRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            if (newRecognizer == null) {
                _state.update { it.copy(isListening = false, error = "Could not initialize speech recognizer service.") }
                return
            }

            newRecognizer.setRecognitionListener(this@VoiceToTextParser)
            recognizer = newRecognizer

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageCode)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }

            newRecognizer.startListening(intent)
            _state.update { it.copy(isListening = true, error = null) }
        } catch (e: Exception) {
            _state.update {
                it.copy(
                    isListening = false,
                    error = "Speech error: ${e.localizedMessage ?: e.javaClass.simpleName}"
                )
            }
        }
    }

    override fun stopListening() {
        try {
            recognizer?.stopListening()
            recognizer?.destroy()
        } catch (_: Exception) {
        } finally {
            recognizer = null
            _state.update { it.copy(isListening = false) }
        }
    }

    override fun clearText() {
        _state.update { it.copy(spokenText = "", partialText = "", error = null) }
    }

    override fun clearError() {
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
        _state.update {
            it.copy(
                isListening = false,
                partialText = "",
                error = getErrorMessage(error)
            )
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
