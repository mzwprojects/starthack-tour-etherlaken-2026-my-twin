package com.mzwprojects.mytwin.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mzwprojects.mytwin.chat.ChatMessage
import com.mzwprojects.mytwin.chat.ChatRepository
import com.mzwprojects.mytwin.chat.ChatRole
import com.mzwprojects.mytwin.di.ServiceLocator
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class ChatUiState(
    val messages: List<ChatMessage> = listOf(
        ChatMessage(
            id = "intro",
            role = ChatRole.ASSISTANT,
            text = "I am here. Ask me how your current pattern looks, what might help next, or what changes could shift your trajectory.",
        ),
    ),
    val input: String = "",
    val isSending: Boolean = false,
    val errorMessage: String? = null,
) {
    val canSend: Boolean
        get() = input.isNotBlank() && !isSending
}

class ChatViewModel(
    private val chatRepository: ChatRepository = ServiceLocator.chatRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    fun setInput(value: String) {
        _uiState.update { it.copy(input = value) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun sendMessage() {
        val prompt = _uiState.value.input.trim()
        if (prompt.isEmpty() || _uiState.value.isSending) return

        val userMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            role = ChatRole.USER,
            text = prompt,
        )
        val assistantId = UUID.randomUUID().toString()
        val assistantPlaceholder = ChatMessage(
            id = assistantId,
            role = ChatRole.ASSISTANT,
            text = "",
            isStreaming = true,
        )

        _uiState.update { current ->
            current.copy(
                input = "",
                isSending = true,
                errorMessage = null,
                messages = current.messages + userMessage + assistantPlaceholder,
            )
        }

        viewModelScope.launch {
            runCatching {
                chatRepository.streamTwinReply(
                    _uiState.value.messages.filterNot { it.id == assistantId },
                ).collect { chunk ->
                    appendChunk(assistantId, chunk)
                }
            }.onSuccess {
                finishAssistantMessage(assistantId)
            }.onFailure { error ->
                _uiState.update { current ->
                    current.copy(
                        isSending = false,
                        errorMessage = error.message ?: "Failed to contact Claude.",
                        messages = current.messages.map { message ->
                            if (message.id == assistantId) {
                                message.copy(
                                    text = if (message.text.isBlank()) {
                                        "I could not answer just now."
                                    } else {
                                        message.text
                                    },
                                    isStreaming = false,
                                )
                            } else {
                                message
                            }
                        },
                    )
                }
            }
        }
    }

    private suspend fun appendChunk(assistantId: String, chunk: String) {
        chunk.forEach { character ->
            _uiState.update { current ->
                current.copy(
                    messages = current.messages.map { message ->
                        if (message.id == assistantId) {
                            message.copy(text = message.text + character)
                        } else {
                            message
                        }
                    },
                )
            }
            delay(8)
        }
    }

    private fun finishAssistantMessage(assistantId: String) {
        _uiState.update { current ->
            current.copy(
                isSending = false,
                messages = current.messages.map { message ->
                    if (message.id == assistantId) {
                        message.copy(isStreaming = false)
                    } else {
                        message
                    }
                },
            )
        }
    }
}
