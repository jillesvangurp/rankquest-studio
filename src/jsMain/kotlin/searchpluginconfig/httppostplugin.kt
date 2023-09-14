package searchpluginconfig

import com.jilesvangurp.rankquest.core.DEFAULT_JSON
import com.jilesvangurp.rankquest.core.DEFAULT_PRETTY_JSON
import com.jilesvangurp.rankquest.core.pluginconfiguration.SearchPluginConfiguration
import com.jilesvangurp.rankquest.core.plugins.ElasticsearchPluginConfiguration
import com.jilesvangurp.rankquest.core.plugins.JsonPostAPIPluginConfig
import components.*
import dev.fritz2.core.RenderContext
import dev.fritz2.core.Store
import dev.fritz2.core.handledBy
import dev.fritz2.core.storeOf
import koin
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject

fun RenderContext.httpPostPluginEditor(
    selectedPluginStore: Store<String>,
    configNameStore: Store<String>,
    editConfigurationStore: Store<SearchPluginConfiguration?>
) {
    editConfigurationStore.data.render { existing ->
        val pluginConfigurationStore = koin.get<PluginConfigurationsStore>()
        val settings = existing?.pluginSettings?.let {
            DEFAULT_JSON.decodeFromJsonElement(
                JsonPostAPIPluginConfig.serializer(), it
            )
        }

        val bodyTemplateStore = storeOf(
            settings?.requestBodyTemplate ?: ""
        )
        val urlStore = storeOf(settings?.searchUrl ?: "")
        val pathToHitsStore = storeOf(settings?.jsonPathToHits?.joinToString(".") ?: "")
        val pathToIdStore = storeOf(settings?.jsonPathToId?.joinToString(".") ?: "")
        val pathToLabelStore = storeOf(settings?.jsonPathToLabel?.joinToString(".") ?: "")
        val pathToSizeStore = storeOf(settings?.jsonPathToLabel?.joinToString(".") ?: "")
        val headersStore = storeOf(settings?.requestHeaders?: mapOf())

        textField("https://mydomain.com/mysearch", "url", "Url to your API") {
            value(urlStore)
        }

        mapEditor(headersStore)
        textAreaField(
            placeHolder = """
            {
              "query": {
                "match": {
                  "title": "{{ query }}"
                }
              }
            }""".trimIndent(),
            label = "Request Payload Template",
            description = "Paste a query and use variable names surrounded " + "by {{ myvariable }} where parameters from your search context will be substituted"
        ) {
            value(bodyTemplateStore)
        }

        textField(
            "hits", "pathToHits", ""
        ) {
            value(pathToHitsStore)
        }
        textField(
            "size", "pathToSize", ""
        ) {
            value(pathToSizeStore)
        }
        textField(
            "documentId", "id", ""
        ) {
            value(pathToIdStore)
        }
        textField(
            "title", "label", ""
        ) {
            value(pathToLabelStore)
        }

        val metricConfigurationsStore = storeOf(existing?.metrics.orEmpty())
        val settingsGenerator = {
            JsonPostAPIPluginConfig(
                searchUrl = urlStore.current,
                requestHeaders = headersStore.current,
                requestBodyTemplate = bodyTemplateStore.current,
                jsonPathToHits = pathToHitsStore.current.split('.'),
                jsonPathToId = pathToIdStore.current.split('.'),
                jsonPathToLabel = pathToLabelStore.current.split('.'),
                jsonPathToSize = pathToSizeStore.current.split('.'),
            ).let { DEFAULT_PRETTY_JSON.encodeToJsonElement<JsonPostAPIPluginConfig>(it) }.jsonObject
        }

        pluginEditorButtonsAndSearchContextEditor(
            selectedPluginStore = selectedPluginStore,
            existing = existing,
            configNameStore = configNameStore,
            metricConfigurationsStore = metricConfigurationsStore,
            settingsGenerator = settingsGenerator,
            editConfigurationStore = editConfigurationStore,
            queryTemplateStore = bodyTemplateStore,
            helpTitle = "Configuring a REST POST API",
            helpText = """
                           This plugin is intended for APIs that return a Json object with a list of hits 
                           in response to an HTTP POST with a request payload. You can configure the URL 
                           and specify headers. 
                            
                           You can configure a templated string to use as the payload. Any variables,
                           which you should surround with `{{ my_variable }}`, will be added to your
                           search context variables.
                           
                           You can further tweak those and configure default values.
                           
                           To extract information from the response you need to provide json paths:
                           
                           - The path to the list with hits.
                           - The relative path to the id field. For example `documentId`
                           - A relative path to field that may be used as a label. For example `author.name`.
                        """.trimIndent()
        )
    }
}


