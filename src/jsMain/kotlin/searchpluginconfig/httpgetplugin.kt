package searchpluginconfig

import com.jilesvangurp.rankquest.core.DEFAULT_JSON
import com.jilesvangurp.rankquest.core.DEFAULT_PRETTY_JSON
import com.jilesvangurp.rankquest.core.pluginconfiguration.SearchPluginConfiguration
import com.jilesvangurp.rankquest.core.plugins.ElasticsearchPluginConfiguration
import com.jilesvangurp.rankquest.core.plugins.JsonGetAPIPluginConfig
import com.jilesvangurp.rankquest.core.plugins.JsonPostAPIPluginConfig
import components.*
import dev.fritz2.core.RenderContext
import dev.fritz2.core.Store
import dev.fritz2.core.handledBy
import dev.fritz2.core.storeOf
import koin
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject

fun RenderContext.httpGetPluginEditor(
    selectedPluginStore: Store<String>,
    configNameStore: Store<String>,
    editConfigurationStore: Store<SearchPluginConfiguration?>
) {
    editConfigurationStore.data.render { existing ->
        val settings = existing?.pluginSettings?.let {
            DEFAULT_JSON.decodeFromJsonElement(
                JsonGetAPIPluginConfig.serializer(), it
            )
        }

        val urlStore = storeOf(settings?.searchUrl ?: "")
        val pathToHitsStore = storeOf(settings?.jsonPathToHits?.joinToString(".") ?: "")
        val pathToIdStore = storeOf(settings?.jsonPathToId?.joinToString(".") ?: "")
        val pathToLabelStore = storeOf(settings?.jsonPathToLabel?.joinToString(".") ?: "")
        val searchContextParamsStore = storeOf(settings?.searchContextParams?: mapOf())
        val headersStore = storeOf(settings?.requestHeaders?: mapOf())

        textField("https://mydomain.com/mysearch", "url", "Url to your API") {
            value(urlStore)
        }
        h2 { +"Request Parameters" }
        para { +"These will be used for your search context." }
        mapEditor(searchContextParamsStore)
        h2 { +"Headers" }
        mapEditor(headersStore)

        textField(
            "hits", "pathToHits", ""
        ) {
            value(pathToHitsStore)
        }
        textField(
            "documentId", "id", ""
        ) {
            value(pathToIdStore)
        }
        textField(
            "title", "label", ""
        ) {
            value(pathToIdStore)
        }

        val metricConfigurationsStore = storeOf(existing?.metrics.orEmpty())
        val settingsGenerator = {
            JsonGetAPIPluginConfig(
                searchUrl = urlStore.current,
                requestHeaders = headersStore.current,
                searchContextParams = searchContextParamsStore.current,
                jsonPathToHits = pathToHitsStore.current.split('.'),
                jsonPathToId = pathToIdStore.current.split('.'),
                jsonPathToLabel = pathToLabelStore.current.split('.'),
            ).let { DEFAULT_PRETTY_JSON.encodeToJsonElement<JsonGetAPIPluginConfig>(it) }.jsonObject
        }

        pluginEditorButtonsAndSearchContextEditor(
            selectedPluginStore = selectedPluginStore,
            existing = existing,
            configNameStore = configNameStore,
            metricConfigurationsStore = metricConfigurationsStore,
            settingsGenerator = settingsGenerator,
            editConfigurationStore = editConfigurationStore,
            queryTemplateStore = null,
            helpTitle = "Configuring a REST GET API",
            helpText = """
                           This plugin is intended for APIs that return a Json object with a list of hits 
                           in response to an HTTP GET. You can configure the URL 
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


