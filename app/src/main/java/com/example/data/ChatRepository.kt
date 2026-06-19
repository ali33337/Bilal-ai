package com.example.data

import android.util.Log
import com.example.BuildConfig
import com.example.data.api.Content
import com.example.data.api.GenerateContentRequest
import com.example.data.api.GenerationConfig
import com.example.data.api.Part
import com.example.data.api.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

class ChatRepository(private val chatDao: ChatDao) {

    val allThreads: Flow<List<ChatThread>> = chatDao.getAllThreads()

    fun getMessagesForThread(threadId: String): Flow<List<ChatMessage>> {
        return chatDao.getMessagesForThread(threadId)
    }

    suspend fun createThread(id: String, initialTitle: String) = withContext(Dispatchers.IO) {
        val thread = ChatThread(id = id, title = initialTitle)
        chatDao.insertThread(thread)
    }

    suspend fun updateThreadTitle(threadId: String, newTitle: String) = withContext(Dispatchers.IO) {
        chatDao.updateThreadTitle(threadId, newTitle)
    }

    suspend fun deleteThread(threadId: String) = withContext(Dispatchers.IO) {
        chatDao.deleteMessagesForThread(threadId)
        chatDao.deleteThread(threadId)
    }

    suspend fun insertMessage(message: ChatMessage) = withContext(Dispatchers.IO) {
        chatDao.insertMessage(message)
    }

    suspend fun sendMessageAndGetResponse(
        threadId: String,
        promptContent: String,
        userMessageId: Long = 0
    ): ChatMessage = withContext(Dispatchers.IO) {
        // 1. If it's a first message or if user prompt is saved, fetch all prior history to construct context
        val previousDbMessages = chatDao.getMessagesForThread(threadId).firstOrNull() ?: emptyList()
        
        // 2. Format history for Google API
        // Role conversion: "user" stays "user", "model" maps to "model" (our database role is "model" represent artificial intelligence response)
        val contentsList = previousDbMessages.map { msg ->
            Content(
                role = if (msg.role == "user") "user" else "model",
                parts = listOf(Part(text = msg.content))
            )
        }.toMutableList()

        // If the latest message is not already in the fetched list, append it
        if (contentsList.none { it.role == "user" && it.parts.firstOrNull()?.text == promptContent }) {
            contentsList.add(
                Content(
                    role = "user",
                    parts = listOf(Part(text = promptContent))
                )
            )
        }

        // 3. Setup system instructions to give Bilal AI personality
        val systemInstructionText = """
            You are Bilal AI, a premium and highly professional AI Assistant designed by Bilal. 
            You must reply with high intelligence, elegance, and precision. 
            Maintain a polite, polite, and sophisticated companion persona. 
            Highlight structure in your responses by using clear, beautiful Markdown: 
            use bullet points, custom spaced spacing, sub-sections, and inline code formatting where relevant. No self-praising or slang; sound helpful, intelligent and expert.
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = contentsList,
            generationConfig = GenerationConfig(temperature = 0.7f),
            systemInstruction = Content(parts = listOf(Part(text = systemInstructionText)))
        )

        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            val errMsg = "API Key not configured. Please add your GEMINI_API_KEY in the secure Secrets panel on the Google AI Studio sidebar."
            val botMessage = ChatMessage(
                threadId = threadId,
                role = "model",
                content = errMsg,
                isError = true
            )
            chatDao.insertMessage(botMessage)
            return@withContext botMessage
        }

        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            
            // Check if response contains candidates or api level errors
            val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (responseText != null) {
                val botMessage = ChatMessage(
                    threadId = threadId,
                    role = "model",
                    content = responseText,
                    isError = false
                )
                chatDao.insertMessage(botMessage)
                
                // If there's only 1 user message in this thread, let's update thread title dynamically!
                if (previousDbMessages.size <= 1) {
                    val summaryTitle = if (promptContent.length > 28) {
                        promptContent.take(25) + "..."
                    } else {
                        promptContent
                    }
                    chatDao.updateThreadTitle(threadId, summaryTitle)
                }
                
                return@withContext botMessage
            } else {
                val apiErr = response.error?.message ?: response.candidates?.firstOrNull()?.finishReason ?: "No response from Gemini API"
                val botMessage = ChatMessage(
                    threadId = threadId,
                    role = "model",
                    content = "API Warning/Error: $apiErr",
                    isError = true
                )
                chatDao.insertMessage(botMessage)
                return@withContext botMessage
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error sending message", e)
            val botMessage = ChatMessage(
                threadId = threadId,
                role = "model",
                content = "Connectivity issue: ${e.localizedMessage ?: "Could not connect to Gemini services"}",
                isError = true
            )
            chatDao.insertMessage(botMessage)
            return@withContext botMessage
        }
    }
}
