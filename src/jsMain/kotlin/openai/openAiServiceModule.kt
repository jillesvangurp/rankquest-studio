package openai

import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.chat.StreamOptions
import com.aallam.openai.api.http.Timeout
import com.aallam.openai.api.model.Model
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.aallam.openai.client.OpenAIHost
import kotlin.time.Duration.Companion.seconds
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val openAiServiceModule = module {
    singleOf(::OpenAiService)
}

class OpenAiService() {
    private var _openAI: OpenAI? = null
    private var model = "gpt-4o"

    fun configure(
        apiKey: String?=null,
        baseUrl: String?=null,
        model: String
    ): OpenAI? {
        this.model = model
        _openAI = OpenAI(
            token = apiKey?:"",
            timeout = Timeout(socket = 60.seconds),
            host = baseUrl.takeIf { !it.isNullOrBlank() }?.let { OpenAIHost(it) } ?: OpenAIHost.OpenAI,
        )

        console.log("reconfigure model to $model")
        return _openAI
    }

    val openAI: OpenAI get() = _openAI ?: error("not configured yet")

    fun enabled() = _openAI != null

    suspend fun listModels(): List<Model> {
        return openAI.models().also {
            console.log("models",it)
        }
    }

    suspend fun sendPrompt(systemPrompt: String, prompt: String): String {
        val ccr = ChatCompletionRequest(
            model = ModelId(model),
            streamOptions = StreamOptions(false),
            messages = listOf(
                ChatMessage(
                    role = ChatRole.System,
                    content = systemPrompt,
                ),
                ChatMessage(
                    role = ChatRole.User,
                    content = """
    $prompt
    """.trimIndent(),
                ),
            ),
        )

        console.log(systemPrompt, prompt)
        val result = openAI.chatCompletion(ccr)
        result.usage?.let {
            console.log(it.promptTokens, it.completionTokens, it.totalTokens)
        }
        return result.choices.last().message.content.orEmpty()
    }
}
