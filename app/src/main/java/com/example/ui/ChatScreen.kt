package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.ChatMessage
import com.example.data.ChatThread
import com.example.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel = viewModel()
) {
    val threads by viewModel.threads.collectAsState()
    val messages by viewModel.currentMessages.collectAsState()
    val activeThreadId by viewModel.currentThreadId.collectAsState()
    val inputMessage by viewModel.inputMessage.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Interactive State Additions
    var showVoiceAssistant by remember { mutableStateOf(false) }
    var showImageSelector by remember { mutableStateOf(false) }
    var selectedImageName by remember { mutableStateOf<String?>(null) }
    var currentTtsMessageId by remember { mutableStateOf<Long?>(null) }
    var isTtsPlaying by remember { mutableStateOf(false) }
    
    // Feedback highlights
    var positiveFeedbackIds by remember { mutableStateOf(setOf<Long>()) }
    var negativeFeedbackIds by remember { mutableStateOf(setOf<Long>()) }

    // Smooth scroll helper
    LaunchedEffect(messages.size, isGenerating) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // TTS speaker player simulation
    LaunchedEffect(currentTtsMessageId) {
        if (currentTtsMessageId != null) {
            isTtsPlaying = true
            delay(5000) // simulated speech duration
            isTtsPlaying = false
            currentTtsMessageId = null
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .width(320.dp)
                    .fillMaxHeight(),
                drawerContainerColor = MaterialTheme.colorScheme.surface
            ) {
                Spacer(modifier = Modifier.statusBarsPadding())

                // Brand Header with Gemini sparkles
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(Color(0xFF2D8CFF), Color(0xFF9B72F8), Color(0xFFF06292))
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Bilal AI",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Advanced Companion",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }

                // New Chat CTA with dynamic accent border
                OutlinedButton(
                    onClick = {
                        viewModel.createNewChat()
                        selectedImageName = null
                        coroutineScope.launch { drawerState.close() }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    border = BorderStroke(
                        1.dp,
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF2D8CFF), Color(0xFF9B72F8))
                        )
                    )
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "New chat")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("New Chat", fontWeight = FontWeight.Bold)
                }

                Divider(
                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                Text(
                    text = "Conversations",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                // Navigation threads list
                if (threads.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No recent chats.\nTap 'New Chat' to start fresh.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(threads, key = { it.id }) { thread ->
                            val isSelected = thread.id == activeThreadId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                        else Color.Transparent
                                    )
                                    .clickable {
                                        viewModel.selectThread(thread.id)
                                        coroutineScope.launch { drawerState.close() }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ChatBubbleOutline,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = thread.title,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                
                                IconButton(
                                    onClick = { viewModel.deleteThread(thread.id) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Dedicated Gemini Footers
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    DrawerFooterItem(icon = Icons.Default.History, title = "Activity")
                    DrawerFooterItem(icon = Icons.Default.HelpOutline, title = "Help & Support")
                    DrawerFooterItem(icon = Icons.Default.Settings, title = "Settings")
                }
            }
        },
        content = {
            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        title = {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .background(
                                            brush = Brush.linearGradient(
                                                colors = listOf(Color(0xFF2D8CFF), Color(0xFF9B72F8))
                                            ),
                                            shape = CircleShape
                                        )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Bilal AI",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = {
                                coroutineScope.launch { drawerState.open() }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Menu"
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = { 
                                viewModel.createNewChat()
                                selectedImageName = null
                            }) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "Restart",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        )
                    )
                },
                bottomBar = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .imePadding()
                            .padding(bottom = 12.dp)
                    ) {
                        // Image Thumbnail Badge if selected
                        AnimatedVisibility(
                            visible = selectedImageName != null,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            selectedImageName?.let { imgName ->
                                Row(
                                    modifier = Modifier
                                        .padding(horizontal = 24.dp)
                                        .padding(bottom = 8.dp)
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Image,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = imgName,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    IconButton(
                                        onClick = { selectedImageName = null },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear",
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Speech Reader Simulator Tray
                        AnimatedVisibility(
                            visible = isTtsPlaying,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 4.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.VolumeUp,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Reading Bilal AI response aloud...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Box(modifier = Modifier.size(24.dp)) {
                                    CircularProgressIndicator(
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }

                        // Bottom tray with Gemini plus buttons & field
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant,
                                    RoundedCornerShape(28.dp)
                                )
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    RoundedCornerShape(28.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            // Plus/Attachment Icon
                            IconButton(onClick = { showImageSelector = true }) {
                                Icon(
                                    imageVector = Icons.Default.AddCircleOutline,
                                    contentDescription = "Attach Content",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            // Message Input Text Field
                            TextField(
                                value = inputMessage,
                                onValueChange = { viewModel.onInputChanged(it) },
                                placeholder = {
                                    Text(
                                        "Message Bilal AI...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 40.dp),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    disabledIndicatorColor = Color.Transparent
                                ),
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Sentences,
                                    imeAction = ImeAction.Send
                                ),
                                singleLine = false,
                                maxLines = 5
                            )

                            // Mic Button
                            IconButton(onClick = { showVoiceAssistant = true }) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Voice Input",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            // Send Button
                            IconButton(
                                onClick = {
                                    val finalPrompt = if (selectedImageName != null) {
                                        "[Attached Image: $selectedImageName] $inputMessage"
                                    } else {
                                        inputMessage
                                    }
                                    viewModel.sendMessage(finalPrompt)
                                    selectedImageName = null
                                },
                                enabled = inputMessage.isNotBlank() && !isGenerating,
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        if (inputMessage.isNotBlank() && !isGenerating) MaterialTheme.colorScheme.primary
                                        else Color.Transparent,
                                        CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send",
                                    tint = if (inputMessage.isNotBlank() && !isGenerating) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Bilal AI may present inaccurate info. Verify responses.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    if (messages.isEmpty() && activeThreadId == null) {
                        // Grand Modern AI Onboarding Dashboard (Empty state)
                        OnboardingDashboard(
                            suggestedPrompts = viewModel.suggestedPrompts,
                            onPromptSelected = { promptText ->
                                viewModel.sendMessage(promptText)
                            }
                        )
                    } else {
                        // Message feed without traditional bubble borders for Gemini AI response
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp),
                            contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
                            verticalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            items(messages) { message ->
                                ChatMessageItem(
                                    message = message,
                                    currentTtsMessageId = currentTtsMessageId,
                                    isTtsPlaying = isTtsPlaying,
                                    onTtsClicked = { msgId ->
                                        currentTtsMessageId = msgId
                                    },
                                    positiveFeedbackIds = positiveFeedbackIds,
                                    negativeFeedbackIds = negativeFeedbackIds,
                                    onPositiveFeedback = { msgId ->
                                        positiveFeedbackIds = if (positiveFeedbackIds.contains(msgId)) {
                                            positiveFeedbackIds - msgId
                                        } else {
                                            positiveFeedbackIds + msgId - negativeFeedbackIds
                                        }
                                    },
                                    onNegativeFeedback = { msgId ->
                                        negativeFeedbackIds = if (negativeFeedbackIds.contains(msgId)) {
                                            negativeFeedbackIds - msgId
                                        } else {
                                            negativeFeedbackIds + msgId - positiveFeedbackIds
                                        }
                                    }
                                )
                            }

                            // Pulsating Gemini typing indicator
                            if (isGenerating) {
                                item {
                                    TypingBubble()
                                }
                            }
                        }
                    }

                    // Simulated Voice Assistant overlay
                    AnimatedVisibility(
                        visible = showVoiceAssistant,
                        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                        exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
                        modifier = Modifier.align(Alignment.BottomCenter)
                    ) {
                        VoiceAssistantModal(
                            onDismiss = { showVoiceAssistant = false },
                            onSpeechResult = { text ->
                                viewModel.onInputChanged(text)
                                showVoiceAssistant = false
                            }
                        )
                    }

                    // Simulated Image Attachment picker
                    AnimatedVisibility(
                        visible = showImageSelector,
                        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                        exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
                        modifier = Modifier.align(Alignment.BottomCenter)
                    ) {
                        ImageSelectorModal(
                            onDismiss = { showImageSelector = false },
                            onImageSelected = { img ->
                                selectedImageName = img
                                showImageSelector = false
                            }
                        )
                    }
                }
            }
        }
    )
}

@Composable
fun DrawerFooterItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    var showExplanation by remember { mutableStateOf(false) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { showExplanation = true }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }

    if (showExplanation) {
        AlertDialog(
            onDismissRequest = { showExplanation = false },
            title = { Text(title) },
            text = { Text("Thanks for using Bilal AI! This setting ($title) acts on your local client cache environment instantly.") },
            confirmButton = {
                TextButton(onClick = { showExplanation = false }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun OnboardingDashboard(
    suggestedPrompts: List<com.example.ui.viewmodel.SuggestedPrompt>,
    onPromptSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        // Large Premium Greeting exactly like Gemini Web
        Text(
            text = buildAnnotatedString {
                withStyle(
                    style = SpanStyle(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF2D8CFF), Color(0xFF9B72F8), Color(0xFFF06292))
                        ),
                        fontWeight = FontWeight.Bold,
                        fontSize = 38.sp
                    )
                ) {
                    append("Hello, Bilal\n")
                }
                withStyle(
                    style = SpanStyle(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 34.sp
                    )
                ) {
                    append("How can I help you today?")
                }
            },
            lineHeight = 44.sp,
            modifier = Modifier.padding(bottom = 36.dp)
        )

        // Suggestion prompt slider cards
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Get started with suggestions:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(suggestedPrompts) { promptObj ->
                PromptSuggestedCard(
                    category = promptObj.category,
                    prompt = promptObj.prompt,
                    iconName = promptObj.iconName,
                    onClick = { onPromptSelected(promptObj.prompt) }
                )
            }
        }
    }
}

@Composable
fun PromptSuggestedCard(
    category: String,
    prompt: String,
    iconName: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(220.dp)
            .height(130.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = prompt,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = category,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                val mIcon = when (iconName) {
                    "email" -> Icons.Default.Email
                    "code" -> Icons.Default.Code
                    "emoji_objects" -> Icons.Default.EmojiObjects
                    "school" -> Icons.Default.School
                    else -> Icons.Default.AutoAwesome
                }

                Icon(
                    imageVector = mIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun ChatMessageItem(
    message: ChatMessage,
    currentTtsMessageId: Long?,
    isTtsPlaying: Boolean,
    onTtsClicked: (Long) -> Unit,
    positiveFeedbackIds: Set<Long>,
    negativeFeedbackIds: Set<Long>,
    onPositiveFeedback: (Long) -> Unit,
    onNegativeFeedback: (Long) -> Unit
) {
    val isUser = message.role == "user"
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            // Sleek Sparkle Badge Icon
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF2D8CFF), Color(0xFF9B72F8))
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
        }

        Column(
            modifier = Modifier.weight(1f, fill = false)
        ) {
            // Sender display name (only user side)
            if (isUser) {
                Text(
                    text = "You",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(bottom = 6.dp, end = 4.dp)
                )
            }

            // Message context text wrapping
            if (isUser) {
                // User Message gets a beautiful elegant gray soft capsule
                Box(
                    modifier = Modifier
                        .align(Alignment.End)
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    SelectionContainer {
                        Text(
                            text = message.content,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyLarge,
                            lineHeight = 22.sp
                        )
                    }
                }
            } else {
                // Bilal AI responses DO NOT have bubble borders - they flow cleanly directly on the canvas!
                // Just like real Gemini Web / iOS / Android. It feels extremely clean & airy.
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (message.isError) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
                                .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = message.content,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    } else {
                        SimpleMarkdownText(
                            text = message.content,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Gemini-Style action bar row directly beneath responses
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Thumbs up
                        val isLiked = positiveFeedbackIds.contains(message.id)
                        IconButton(
                            onClick = { onPositiveFeedback(message.id) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (isLiked) Icons.Default.ThumbUpAlt else Icons.Default.ThumbUp,
                                contentDescription = "Like",
                                tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Thumbs down
                        val isDisliked = negativeFeedbackIds.contains(message.id)
                        IconButton(
                            onClick = { onNegativeFeedback(message.id) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (isDisliked) Icons.Default.ThumbDownAlt else Icons.Default.ThumbDown,
                                contentDescription = "Dislike",
                                tint = if (isDisliked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // TTS speaker read-aloud simulation
                        val isThisTts = currentTtsMessageId == message.id && isTtsPlaying
                        IconButton(
                            onClick = { onTtsClicked(message.id) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (isThisTts) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                                contentDescription = "Listen",
                                tint = if (isThisTts) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Google "search" item
                        IconButton(
                            onClick = {
                                // simulated web search verification
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = "Verify with Google",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Copy item
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(message.content))
                                copied = true
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                                contentDescription = "Copy text",
                                tint = if (copied) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SimpleMarkdownText(text: String, modifier: Modifier = Modifier) {
    val blocks = remember(text) { parseChatText(text) }
    
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        blocks.forEach { block ->
            when (block) {
                is TextBlock.Code -> {
                    CodeBlockCard(code = block.content, language = block.language)
                }
                is TextBlock.Paragraph -> {
                    SelectionContainer {
                        Text(
                            text = block.content,
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyLarge,
                            lineHeight = 24.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CodeBlockCard(code: String, language: String) {
    val clipboardManager = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E1E)
        )
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2D2D2D))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = language.uppercase().ifEmpty { "CODE" },
                    color = Color(0xFFAAAAAA),
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = FontFamily.Monospace
                )
                
                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(code))
                        copied = true
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (copied) Icons.Default.Done else Icons.Default.ContentCopy,
                        contentDescription = "Copy code",
                        tint = if (copied) Color.Green else Color(0xFFAAAAAA),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            
            SelectionContainer {
                Text(
                    text = code,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .horizontalScroll(rememberScrollState()),
                    color = Color(0xFFD4D4D4),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
fun TypingBubble() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF2D8CFF), Color(0xFF9B72F8))
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                val transition = rememberInfiniteTransition()
                
                val dot1Scale by transition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(600, easing = EaseInOutSine),
                        repeatMode = RepeatMode.Reverse
                    )
                )
                val dot2Scale by transition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(600, delayMillis = 200, easing = EaseInOutSine),
                        repeatMode = RepeatMode.Reverse
                    )
                )
                val dot3Scale by transition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(600, delayMillis = 400, easing = EaseInOutSine),
                        repeatMode = RepeatMode.Reverse
                    )
                )

                Box(modifier = Modifier.size(8.dp).scale(dot1Scale).background(MaterialTheme.colorScheme.primary, CircleShape))
                Box(modifier = Modifier.size(8.dp).scale(dot2Scale).background(MaterialTheme.colorScheme.primary, CircleShape))
                Box(modifier = Modifier.size(8.dp).scale(dot3Scale).background(MaterialTheme.colorScheme.primary, CircleShape))
            }
        }
    }
}

// Simulated Voice Capture Assistant overlay panel
@Composable
fun VoiceAssistantModal(
    onDismiss: () -> Unit,
    onSpeechResult: (String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var stateText by remember { mutableStateOf("Listening to Bilal...") }
    val simulatedPhrases = listOf(
        "Explain quantum computing in code",
        "Generate a professional response to client",
        "Write a complete quicksort program in Kotlin",
        "Brainstorm five names for my photography store"
    )

    LaunchedEffect(Unit) {
        delay(2000)
        stateText = "Processing audio signals..."
        delay(1500)
        val selected = simulatedPhrases.random()
        stateText = "Transcribing: \"$selected\""
        delay(1000)
        onSpeechResult(selected)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(MaterialTheme.colorScheme.surface),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Voice Search",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Pulse wave simulator
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.height(60.dp)
            ) {
                val waveTransition = rememberInfiniteTransition()
                for (i in 0..6) {
                    val scaleY by waveTransition.animateFloat(
                        initialValue = 0.2f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(400 + i * 80, easing = EaseInOutSine),
                            repeatMode = RepeatMode.Reverse
                        )
                    )
                    Box(
                        modifier = Modifier
                            .width(6.dp)
                            .fillMaxHeight(scaleY)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color(0xFF2D8CFF), Color(0xFF9B72F8))
                                ),
                                shape = RoundedCornerShape(3.dp)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stateText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Say whatever you want to ask Bilal AI",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

// Simulated Image Picker selection Modal
@Composable
fun ImageSelectorModal(
    onDismiss: () -> Unit,
    onImageSelected: (String) -> Unit
) {
    val simulatedImages = listOf(
        "SalesChart2026.png",
        "AppBugTraceback.log",
        "MarketingBannerDraft.jpg",
        "MockupLayoutSpec.pdf"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(MaterialTheme.colorScheme.surface),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Simulated File Attachment",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Pick a reference file to include in Bilal AI context window:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                simulatedImages.forEach { filename ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .clickable { onImageSelected(filename) }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (filename.endsWith("png") || filename.endsWith("jpg")) Icons.Default.Image else Icons.Default.Code,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = filename,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MaterialTheme.surfaceColorAtElevation(elevation: androidx.compose.ui.unit.Dp): Color {
    return if (elevation == 0.dp) {
        this.colorScheme.surface
    } else {
        val alpha = (4.5f * elevation.value + 2f) / 100f
        this.colorScheme.primary.copy(alpha = alpha.coerceAtMost(0.15f))
    }
}

sealed class TextBlock {
    data class Paragraph(val content: AnnotatedString) : TextBlock()
    data class Code(val content: String, val language: String = "") : TextBlock()
}

fun parseChatText(rawText: String): List<TextBlock> {
    val blocks = mutableListOf<TextBlock>()
    val parts = rawText.split("```")
    
    for (i in parts.indices) {
        val part = parts[i]
        if (i % 2 != 0) {
            // It's a code block
            val lines = part.split("\n")
            val language = lines.firstOrNull()?.trim() ?: ""
            val codeLines = if (lines.size > 1) lines.drop(1) else lines
            val codeContent = codeLines.joinToString("\n").trim()
            blocks.add(TextBlock.Code(codeContent, language))
        } else {
            // It's standard text
            blocks.add(TextBlock.Paragraph(parseFormattedText(part)))
        }
    }
    return blocks
}

fun parseFormattedText(text: String): AnnotatedString {
    return buildAnnotatedString {
        val parts = text.split("**")
        for (i in parts.indices) {
            val part = parts[i]
            if (i % 2 != 0) {
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(part)
                }
            } else {
                // Parse bullet lines
                val lines = part.split("\n")
                lines.forEachIndexed { lineIdx, line ->
                    if (line.trim().startsWith("* ") || line.trim().startsWith("- ")) {
                        append("  •  ")
                        append(line.trim().substring(2))
                    } else {
                        append(line)
                    }
                    if (lineIdx < lines.lastIndex) {
                        append("\n")
                    }
                }
            }
        }
    }
}

