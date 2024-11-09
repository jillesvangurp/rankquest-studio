package searchpluginconfig

import components.LocalStoringStore
import components.selectBox
import components.textField
import dev.fritz2.core.RenderContext
import dev.fritz2.core.lensOf
import dev.fritz2.core.map
import dev.fritz2.core.storeOf
import koin
import kotlinx.serialization.Serializable
import openai.OpenAiService
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

@Serializable
data class Settings(
    val openAiBaseUrl: String? = null,
    val openAiApiKey: String = "",
    val model: String = ""
)

class SettingsStore : LocalStoringStore<Settings>(
    Settings(), "rankquest-settings", Settings.serializer()
) {
    var models = storeOf(listOf("o1"), job)

    suspend fun configure() {
        val ai = koin.get<OpenAiService>()
        val settings = current
        if (settings != null) {
            console.log("configuring open ai service")
            ai.configure(settings.openAiApiKey, settings.openAiBaseUrl, settings.model)
        } else {
            console.warn("null config")
        }
        if (ai.enabled()) {
            console.log("fetching models")
            runCatching {
                models.update(ai.listModels().map { it.id.id })
            }
        }

    }
}

val SettingsStore.openAiBaseUrlStore
    get() = map(
        lensOf<Settings, String>(
            "openAiBaseUrl",
            { s -> s.openAiBaseUrl ?: "" },
            { s, v -> s.copy(openAiBaseUrl = v) })
    )
val SettingsStore.openAiApiKeyStore
    get() = map(
        lensOf<Settings, String>(
            "openAiApiKey",
            { s -> s.openAiApiKey },
            { s, v -> s.copy(openAiApiKey = v) })
    )
val SettingsStore.modelStore
    get() = map(
        lensOf<Settings, String>(
            "model",
            { s -> s.model },
            { s, v -> s.copy(model = v) })
    )

val settingsModule = module {
    singleOf(::SettingsStore)
}


fun RenderContext.settings() {
    val settingsStore = koin.get<SettingsStore>()
    settingsStore.data handledBy {
        settingsStore.configure()
    }
    val collapseStore = storeOf(true)
    h2 {
        a {
            +"AI Configuration"
            clicks handledBy {
                collapseStore.update(!collapseStore.current)
            }
        }
    }
    collapseStore.data.render {collapsed ->
        if(!collapsed) {
            textField(
                "http://localhost:11434/v1",
                "Base Url",
                description = "Leave blank to use OpenAI or provide compatible alternative. When using ollama, this is the correct url: http://localhost:11434/v1"
            ) {
                value(settingsStore.openAiBaseUrlStore)
            }
            val showApiKeyFieldStore = storeOf(false)
            div {
                a {
                    showApiKeyFieldStore.data.render { show ->
                        if (show) {
                            +"Hide API Key"
                        } else {
                            +"Edit API Key"
                        }
                        clicks handledBy {
                            showApiKeyFieldStore.update(!showApiKeyFieldStore.current)
                        }
                    }
                }
                showApiKeyFieldStore.data.render { show ->
                    if (show) {
                        textField(
                            "XYZ",
                            "API Key",
                            description = "Your API key if one is needed. Leave blank for ollama."
                        ) {
                            value(settingsStore.openAiApiKeyStore)
                        }
                    }
                }
            }
            settingsStore.data.render { s ->
                settingsStore.models.data.render { models ->
                    label {
                        +"Pick a model"
//                    selectBox(settingsStore.modelStore, models, emptyItem = "-")
                        // buggy select in fritz2
                        settingsStore.modelStore.data.render { current ->
                            ul {
                                models.forEach { modelId ->
                                    li {
                                        if (current == modelId) {
                                            b {
                                                +modelId
                                            }
                                        } else {
                                            a {
                                                +modelId
                                                clicks handledBy {
                                                    settingsStore.modelStore.update(modelId)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}