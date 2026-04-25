package com.mzwprojects.mytwin.chat

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.mzwprojects.mytwin.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.net.HttpURLConnection
import java.net.URL

class ClaudeApiService(
    private val gson: Gson = Gson(),
) {

    fun streamMessage(
        systemPrompt: String,
        messages: List<ChatMessage>,
    ): Flow<String> = flow {
        if (BuildConfig.CLAUDE_API_KEY.isBlank()) {
            error("Claude API key missing. Add claudeApiKey=... to local.properties.")
        }

        val connection = (URL(API_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doInput = true
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("x-api-key", BuildConfig.CLAUDE_API_KEY)
            setRequestProperty("anthropic-version", ANTHROPIC_VERSION)
            setRequestProperty("Accept", "text/event-stream")
        }

        val payload = buildPayload(systemPrompt, messages)
        connection.outputStream.bufferedWriter().use { writer ->
            writer.write(payload)
        }

        val responseCode = connection.responseCode
        if (responseCode !in 200..299) {
            val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() }
            error(parseErrorMessage(errorBody))
        }

        connection.inputStream.bufferedReader().useLines { lines ->
            var currentEvent = ""
            lines.forEach { rawLine ->
                when {
                    rawLine.startsWith("event:") -> {
                        currentEvent = rawLine.removePrefix("event:").trim()
                    }

                    rawLine.startsWith("data:") -> {
                        val data = rawLine.removePrefix("data:").trim()
                        if (data.isBlank() || data == "[DONE]") return@forEach

                        if (currentEvent == "content_block_delta") {
                            val json = JsonParser.parseString(data).asJsonObject
                            val delta = json.getAsJsonObject("delta")
                            val text = delta?.get("text")?.asString
                            if (!text.isNullOrEmpty()) emit(text)
                        } else if (currentEvent == "error") {
                            val message = parseErrorMessage(data)
                            error(message)
                        }
                    }
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun buildPayload(
        systemPrompt: String,
        messages: List<ChatMessage>,
    ): String {
        val root = JsonObject().apply {
            addProperty("model", BuildConfig.CLAUDE_MODEL)
            addProperty("max_tokens", 700)
            addProperty("temperature", 0.6)
            addProperty("stream", true)
            addProperty("system", systemPrompt)
            add(
                "messages",
                gson.toJsonTree(
                    messages.map {
                        mapOf(
                            "role" to if (it.role == ChatRole.USER) "user" else "assistant",
                            "content" to it.text,
                        )
                    },
                ),
            )
        }
        return gson.toJson(root)
    }

    private fun parseErrorMessage(raw: String?): String {
        if (raw.isNullOrBlank()) return "Claude request failed."
        return runCatching {
            val json = JsonParser.parseString(raw).asJsonObject
            json.getAsJsonObject("error")?.get("message")?.asString
                ?: json.get("message")?.asString
                ?: raw
        }.getOrElse {
            Log.w(TAG, "Failed to parse Claude error body", it)
            raw
        }
    }

    companion object {
        private const val TAG = "ClaudeApiService"
        private const val API_URL = "https://api.anthropic.com/v1/messages"
        private const val ANTHROPIC_VERSION = "2023-06-01"
    }
}
