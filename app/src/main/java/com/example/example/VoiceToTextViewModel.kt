package com.example.example

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.StateFlow

class VoiceToTextViewModel @JvmOverloads constructor(
    application: Application,
    private val engine: VoiceToTextEngine = VoiceToTextParser(application),
) : AndroidViewModel(application) {

    val state: StateFlow<VoiceToTextState> = engine.state

    fun startListening(languageCode: String = "en-US") {
        engine.startListening(languageCode)
    }

    fun stopListening() {
        engine.stopListening()
    }

    fun clearText() {
        engine.clearText()
    }

    fun clearError() {
        engine.clearError()
    }

    override fun onCleared() {
        super.onCleared()
        engine.stopListening()
    }
}
