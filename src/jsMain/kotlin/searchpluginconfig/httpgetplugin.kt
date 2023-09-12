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
        val searchContextParamsStore = storeOf(mapOf<String,String>())
        val headersStore = storeOf(mapOf<String,String>())

        textField("https://mydomain.com/mysearch", "url", "Url to your API") {
            value(urlStore)
        }

        mapEditor(searchContextParamsStore)
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
                requestHeaders = mapOf(),
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
            queryTemplateStore = null
        )
    }
}


