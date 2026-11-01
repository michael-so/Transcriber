package com.example.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.example.ui.theme.ExampleTheme
import kotlinx.coroutines.launch

@Composable
fun VoiceToTextScreen(
    viewModel: VoiceToTextViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun isPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    var hasAudioPermission by remember {
        mutableStateOf(isPermissionGranted())
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasAudioPermission = isGranted
        if (!isGranted) {
            scope.launch {
                snackbarHostState.showSnackbar("Microphone permission is required to detect and transcribe voice.")
            }
        } else {
            viewModel.startListening()
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let { err ->
            snackbarHostState.showSnackbar(err)
            viewModel.clearError()
        }
    }

    VoiceToTextScreenContent(
        state = state,
        hasAudioPermission = hasAudioPermission,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
        onRequestPermission = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
        onStartListening = {
            val granted = isPermissionGranted()
            hasAudioPermission = granted
            if (granted) {
                viewModel.startListening()
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        },
        onStopListening = { viewModel.stopListening() },
        onSelectLanguage = { language -> viewModel.setLanguage(language) },
        onClearText = { viewModel.clearText() },
        onCopyText = { text ->
            clipboardManager.setText(AnnotatedString(text))
            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
        },
        onShareText = { text ->
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share transcribed text"))
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceToTextScreenContent(
    state: VoiceToTextState,
    hasAudioPermission: Boolean,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    onRequestPermission: () -> Unit = {},
    onStartListening: () -> Unit = {},
    onStopListening: () -> Unit = {},
    onSelectLanguage: (LanguageOption) -> Unit = {},
    onClearText: () -> Unit = {},
    onCopyText: (String) -> Unit = {},
    onShareText: (String) -> Unit = {},
) {
    var isLanguageMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Voice Transcriber",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Speech Recognition in Malay & English",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Language Dropdown Selector Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    AssistChip(
                        onClick = { isLanguageMenuExpanded = true },
                        label = {
                            Text("${state.selectedLanguage.flag} ${state.selectedLanguage.displayName}")
                        },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Select language",
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    )

                    DropdownMenu(
                        expanded = isLanguageMenuExpanded,
                        onDismissRequest = { isLanguageMenuExpanded = false }
                    ) {
                        supportedLanguages.forEach { language ->
                            DropdownMenuItem(
                                text = {
                                    Text("${language.flag}  ${language.displayName}")
                                },
                                onClick = {
                                    onSelectLanguage(language)
                                    isLanguageMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                if (state.isListening) {
                    Text(
                        text = "● MENDENGAR",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Permission Warning Card if missing permission
            if (!hasAudioPermission) {
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Microphone Permission Required",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "To transcribe your spoken voice into text, please grant audio recording access.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onRequestPermission
                        ) {
                            Text("Grant Permission")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Transcribed Text Display Box
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    val scrollState = rememberScrollState()

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                    ) {
                        val hasSpokenText = state.spokenText.isNotBlank()
                        val hasPartialText = state.partialText.isNotBlank()

                        if (!hasSpokenText && !hasPartialText) {
                            val placeholderText = if (state.selectedLanguage.code.startsWith("ms")) {
                                "Teks pertuturan anda akan muncul di sini.\n\nTekan butang mikrofon di bawah dan mula bercakap dalam Bahasa Melayu."
                            } else {
                                "Your transcribed text will appear here.\n\nTap the microphone button below and start speaking in English."
                            }

                            Text(
                                text = placeholderText,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                fontStyle = FontStyle.Italic,
                                modifier = Modifier.padding(top = 20.dp)
                            )
                        } else {
                            if (hasSpokenText) {
                                Text(
                                    text = state.spokenText,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontSize = 18.sp,
                                        lineHeight = 26.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            if (hasPartialText) {
                                if (hasSpokenText) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                                Text(
                                    text = state.partialText,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontSize = 18.sp,
                                        lineHeight = 26.sp,
                                        fontStyle = FontStyle.Italic
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // Word Count Badge
                    val fullText = "${state.spokenText} ${state.partialText}".trim()
                    val wordCount = if (fullText.isEmpty()) 0 else fullText.split("\\s+".toRegex()).size
                    Surface(
                        modifier = Modifier.align(Alignment.BottomEnd),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 4.dp
                    ) {
                        val perkataanLabel = if (state.selectedLanguage.code.startsWith("ms")) "perkataan" else "words"
                        Text(
                            text = "$wordCount $perkataanLabel",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons Bar (Copy, Share, Clear)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val fullText = state.spokenText.trim()
                val isTextAvailable = fullText.isNotEmpty()

                // Copy Button
                IconButton(
                    onClick = {
                        if (isTextAvailable) {
                            onCopyText(fullText)
                        }
                    },
                    enabled = isTextAvailable,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy text"
                    )
                }

                // Share Button
                IconButton(
                    onClick = {
                        if (isTextAvailable) {
                            onShareText(fullText)
                        }
                    },
                    enabled = isTextAvailable,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share text"
                    )
                }

                // Clear Button
                IconButton(
                    onClick = onClearText,
                    enabled = isTextAvailable || state.partialText.isNotEmpty(),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear text"
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Microphone Button with Pulse Animation
            MicButtonSection(
                isListening = state.isListening,
                hasPermission = hasAudioPermission,
                language = state.selectedLanguage
            ) {
                if (!hasAudioPermission) {
                    onRequestPermission()
                } else if (state.isListening) {
                    onStopListening()
                } else {
                    onStartListening()
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun MicButtonSection(
    isListening: Boolean,
    hasPermission: Boolean,
    language: LanguageOption,
    onMicClick: () -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulseAnimation")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            // Pulse Ring Background when listening
            if (isListening) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.25f))
                )
            }

            // Main Mic Button
            val containerColor = when {
                !hasPermission -> MaterialTheme.colorScheme.outlineVariant
                isListening -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.primary
            }

            val iconColor = when {
                !hasPermission -> MaterialTheme.colorScheme.onSurfaceVariant
                isListening -> MaterialTheme.colorScheme.onError
                else -> MaterialTheme.colorScheme.onPrimary
            }

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(containerColor)
                    .border(
                        width = 3.dp,
                        color = if (isListening) MaterialTheme.colorScheme.errorContainer else Color.Transparent,
                        shape = CircleShape
                    )
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onMicClick,
                    modifier = Modifier.fillMaxSize()
                ) {
                    val icon = when {
                        !hasPermission -> Icons.Default.MicOff
                        isListening -> Icons.Default.Stop
                        else -> Icons.Default.Mic
                    }

                    Icon(
                        imageVector = icon,
                        contentDescription = if (isListening) "Stop listening" else "Start listening",
                        tint = iconColor,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        val buttonLabel = when {
            !hasPermission -> "Tekan untuk kebenaran mikrofon"
            isListening -> if (language.code.startsWith("ms")) "Mendengar... Sila bercakap" else "Listening... Speak now"
            language.code.startsWith("ms") -> "Tekan untuk bercakap Bahasa Melayu"
            else -> "Tap to speak in English"
        }

        Text(
            text = buttonLabel,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = if (isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Preview(showBackground = true)
@Composable
fun VoiceToTextScreenPreview() {
    ExampleTheme {
        VoiceToTextScreenContent(
            state = VoiceToTextState(
                spokenText = "Selamat datang! Ini adalah demo pengecaman suara Bahasa Melayu dalam masa nyata.",
                isListening = false,
                selectedLanguage = supportedLanguages[0]
            ),
            hasAudioPermission = true,
            snackbarHostState = remember { SnackbarHostState() }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun VoiceToTextScreenListeningPreview() {
    ExampleTheme {
        VoiceToTextScreenContent(
            state = VoiceToTextState(
                spokenText = "Mengenal pasti pertuturan...",
                partialText = "dan menukar suara kepada teks",
                isListening = true,
                selectedLanguage = supportedLanguages[0]
            ),
            hasAudioPermission = true,
            snackbarHostState = remember { SnackbarHostState() }
        )
    }
}
