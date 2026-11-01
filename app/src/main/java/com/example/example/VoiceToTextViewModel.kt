package com.example.example

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.StateFlow

class VoiceToTextViewModel(application: Application) : AndroidViewModel(application) {

    val parser = VoiceToTextParser(application)
    val state: StateFlow<VoiceToTextState> = parser.state

    fun startListening(languageCode: String = "en-US") {
        parser.startListening(languageCode)
    }

    fun stopListening() {
        parser.stopListening()
    }

    fun clearText() {
        parser.clearText()
    }

    fun clearError() {
        parser.clearError()
    }

    override fun onCleared() {
        super.onCleared()
        parser.stopListening()
    }
}
