This branch implements Modularity to the Main so that certain function can be reused.

1. Engine Abstraction Layer (VoiceToTextEngine)
Defined a contract interface in  VoiceToTextParser.kt:

[
interface VoiceToTextEngine {
val state: StateFlow<VoiceToTextState>
fun startListening(languageCode: String = "en-US")
fun stopListening()
fun clearText()
fun clearError()
}
i) Future Proofing Benefit: The app is not locked into Android's default SpeechRecognizer. If we want to switch to an on-device AI model (e.g. OpenAI Whisper / LiteRT), an offline speech library (Vosk / PocketSphinx), or a cloud speech API (Google Cloud Speech, Azure, AWS), simply create a new class that implements VoiceToTextEngine. Neither the ViewModel nor the UI code needs to be modified.
]

2. Speech Engine Implementation (VoiceToTextParser)
i) Implements VoiceToTextEngine and Android's RecognitionListener.
ii) Encapsulates all low-level SpeechRecognizer logic, partial result streaming, and error code translations into human-readable strings.
iii) Modularity Benefit: The engine is completely isolated from the UI and ViewModel. It can be tested independently or reused in a background service, wearable app, or widget.

3. ViewModel Layer (VoiceToTextViewModel)
In  VoiceToTextViewModel.kt:

[
class VoiceToTextViewModel(
application: Application,
private val engine: VoiceToTextEngine = VoiceToTextParser(application)
) : AndroidViewModel(application) {
val state: StateFlow<VoiceToTextState> = engine.state
...
}
]

i) Dependency Inversion: The ViewModel depends on the VoiceToTextEngine abstraction rather than a concrete implementation.
ii) Testing & Reusability: In unit tests, we can inject a MockVoiceToTextEngine into VoiceToTextViewModel to simulate speech input instantly without needing a physical microphone or speech service.
iii) Hilt / Koin Ready: Ready for Dependency Injection frameworks.

4. UI Layer Separation (Stateful vs. Stateless Composable)
In  VoiceToTextScreen.kt:
i) VoiceToTextScreen (Stateful): Connects to the ViewModel, checks permissions, and handles system intents (clipboard, share intent).
ii) VoiceToTextScreenContent (Stateless): Pure declarative UI component that receives immutable state (VoiceToTextState) and emits user actions as callbacks (onStartListening, onStopListening, onCopyText, onClearText).
iii) Future Proofing Benefit: The stateless UI can easily be embedded inside a tab, bottom sheet, or multi-pane tablet/foldable layout without altering any internal business logic.

Architecture Summary:
graph TD
UI[VoiceToTextScreenContent <br/> Stateless Compose UI] -->|Events| VM[VoiceToTextViewModel]
VM -->|StateFlow| UI
VM -->|Interface Contract| Engine[VoiceToTextEngine]
Engine <|.. Implements .. Parser[VoiceToTextParser <br/> Android SpeechRecognizer]
Engine <|.. Future Proof .. Whisper[Whisper / Offline AI Engine]
