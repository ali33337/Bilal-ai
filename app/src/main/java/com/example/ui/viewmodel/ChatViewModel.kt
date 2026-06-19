package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.ChatMessage
import com.example.data.ChatRepository
import com.example.data.ChatThread
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val chatDao = AppDatabase.getDatabase(application).chatDao()
    private val repository = ChatRepository(chatDao)

    // All available chat sessions
    val threads: StateFlow<List<ChatThread>> = repository.allThreads
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _currentThreadId = MutableStateFlow<String?>(null)
    val currentThreadId: StateFlow<String?> = _currentThreadId.asStateFlow()

    // Reactive list of messages for the selected conversation thread
    val currentMessages: StateFlow<List<ChatMessage>> = _currentThreadId
        .flatMapLatest { threadId ->
            if (threadId != null) {
                repository.getMessagesForThread(threadId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _inputMessage = MutableStateFlow("")
    val inputMessage: StateFlow<String> = _inputMessage.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    // Standard high quality starter recommendations like Gemini
    val suggestedPrompts = listOf(
        SuggestedPrompt("Write a formal mail", "Request a recommendation letter from a college mentor", "email"),
        SuggestedPrompt("Coding Assistant", "Help me write a solid implementation for visual ripple in Compose", "code"),
        SuggestedPrompt("Brainstorm ideas", "Brainstorm names for an eco-friendly transport agency", "emoji_objects"),
        SuggestedPrompt("Analyze text", "Explain quantum physics to a teenager in 3 analogical cases", "school")
    )

    init {
        // Automatically load the most recent thread if it exists on boot
        viewModelScope.launch {
            threads.collect { list ->
                if (_currentThreadId.value == null && list.isNotEmpty()) {
                    _currentThreadId.value = list.first().id
                }
            }
        }
    }

    fun onInputChanged(newValue: String) {
        _inputMessage.value = newValue
    }

    fun selectThread(threadId: String?) {
        _currentThreadId.value = threadId
    }

    fun createNewChat() {
        _currentThreadId.value = null
        _inputMessage.value = ""
    }

    fun deleteThread(threadId: String) {
        viewModelScope.launch {
            repository.deleteThread(threadId)
            if (_currentThreadId.value == threadId) {
                _currentThreadId.value = null
            }
        }
    }

    fun sendMessage(customPrompt: String? = null) {
        val promptToSend = customPrompt ?: _inputMessage.value
        if (promptToSend.isBlank()) return

        val activeThreadId = _currentThreadId.value ?: UUID.randomUUID().toString()
        val isNewThread = _currentThreadId.value == null

        // Clear user typing field if they typed it manually
        if (customPrompt == null) {
            _inputMessage.value = ""
        }

        viewModelScope.launch {
            _isGenerating.value = true

            if (isNewThread) {
                // Instantly register the new Thread in Database and update ID to hold screen
                repository.createThread(activeThreadId, "New Conversation")
                _currentThreadId.value = activeThreadId
            }

            // Save User prompt to DB instantly
            val userMsg = ChatMessage(
                threadId = activeThreadId,
                role = "user",
                content = promptToSend
            )
            repository.insertMessage(userMsg)

            // Contact Gemini and wait for response
            repository.sendMessageAndGetResponse(activeThreadId, promptToSend)
            
            _isGenerating.value = false
        }
    }
}

data class SuggestedPrompt(
    val category: String,
    val prompt: String,
    val iconName: String
)
