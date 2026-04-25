package com.mzwprojects.mytwin.chat

enum class ChatRole {
    USER,
    ASSISTANT,
}

data class ChatMessage(
    val id: String,
    val role: ChatRole,
    val text: String,
    val isStreaming: Boolean = false,
)

data class ClaudeContextSnapshot(
    val systemPrompt: String,
)
