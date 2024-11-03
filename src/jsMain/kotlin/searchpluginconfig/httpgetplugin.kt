package searchpluginconfig

import com.jilesvangurp.rankquest.core.DEFAULT_JSON
import com.jilesvangurp.rankquest.core.DEFAULT_PRETTY_JSON
import com.jilesvangurp.rankquest.core.pluginconfiguration.SearchPluginConfiguration
import com.jilesvangurp.rankquest.core.plugins.JsonGetAPIPluginConfig
import components.para
import components.textField
import dev.fritz2.core.RenderContext
import dev.fritz2.core.Store
import dev.fritz2.core.storeOf
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
        val pathToSizeStore = storeOf(settings?.jsonPathToSize?.joinToString(".") ?: "")
        val searchContextParamsStore = storeOf(settings?.searchContextParams?: mapOf())
        val headersStore = storeOf(settings?.requestHeaders?: mapOf())

        textField("https://mydomain.com/mysearch", "url", "Url to your API") {
            value(urlStore)
        }
        h2 { +"Request Parameters" }
        para { +"These will be added to the request. Use these for parameters that should not be editable like API keys." }
        mapEditor(searchContextParamsStore)
        h2 { +"Headers" }
        mapEditor(headersStore)

        h2 {
            +"Response parsing"
        }
        para {
            +"""
                Rankquest needs a bit of information to help pick apart the json response of your API. 
                For this we use simple dotted paths. Responses are assumed to have a list of results that are 
                json objects, a result size, and document ids.
                """.trimIndent()
        }
        textField(
            placeHolder = "hits", label = "Path to Hits", description = "Property name of the list of results. Use dots for nested properties, e.g. Elasticsearch would have the list of hits under: 'hits.hits'"
        ) {
            value(pathToHitsStore)
        }
        textField(
            "size", "Path to Size", "Property name for the field that has the result size. E.g. 'total'"
        ) {
            value(pathToSizeStore)
        }
        textField(
            "documentId", "Document Id", "Property path for the document id relative to the object."
        ) {
            value(pathToIdStore)
        }
        textField(
            "title", "Document Label", "Path to a field that can be used as a label for your document. This is optional but of course helpful. E.g. title"
        ) {
            value(pathToLabelStore)
        }

        val metricConfigurationsStore = storeOf(existing?.metrics?: StandardConfigurations.defaults)
        val settingsGenerator = {
            JsonGetAPIPluginConfig(
                searchUrl = urlStore.current,
                requestHeaders = headersStore.current,
                searchContextParams = searchContextParamsStore.current,
                jsonPathToHits = pathToHitsStore.current.split('.'),
                jsonPathToId = pathToIdStore.current.split('.'),
                jsonPathToLabel = pathToLabelStore.current.split('.'),
                jsonPathToSize = pathToSizeStore.current.split('.'),
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


