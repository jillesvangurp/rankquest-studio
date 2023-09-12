package searchpluginconfig

import com.jilesvangurp.rankquest.core.DEFAULT_JSON
import com.jilesvangurp.rankquest.core.DEFAULT_PRETTY_JSON
import com.jilesvangurp.rankquest.core.pluginconfiguration.SearchPluginConfiguration
import com.jilesvangurp.rankquest.core.plugins.ElasticsearchPluginConfiguration
import components.switchField
import components.textAreaField
import components.textField
import dev.fritz2.core.RenderContext
import dev.fritz2.core.Store
import dev.fritz2.core.storeOf
import koin
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject

fun RenderContext.elasticsearchEditor(
    selectedPluginStore: Store<String>,
    configNameStore: Store<String>,
    editConfigurationStore: Store<SearchPluginConfiguration?>
) {
    editConfigurationStore.data.render { existing ->
        val pluginConfigurationStore = koin.get<PluginConfigurationsStore>()
        val settings = existing?.pluginSettings?.let {
            DEFAULT_JSON.decodeFromJsonElement(
                ElasticsearchPluginConfiguration.serializer(), it
            )
        }

        val queryTemplateStore = storeOf(
            settings?.queryTemplate ?: """
            {
              "size": {{ size }}, 
              "query": {
                
                "multi_match": {
                  "query": "{{ text }}",
                  "fields": ["title^2","description","ingredients","directions","author.name"],
                  "fuzziness": "AUTO"
                }
              }
            }
        """.trimIndent()
        )
        val indexStore = storeOf(settings?.index ?: "")
        val labelFieldsStore = storeOf(settings?.labelFields?.joinToString(", ") ?: "titel, author.name")
        val hostStore = storeOf(settings?.host ?: "localhost")
        val portStore = storeOf(settings?.port?.toString() ?: "")
        val httpsStore = storeOf(settings?.https ?: false)
        val userStore = storeOf(settings?.user ?: "")
        val passwordStore = storeOf(settings?.password ?: "")
        val loggingStore = storeOf(settings?.logging ?: false)

        textField("myindex", "index", "Index or alias name that you want to query") {
            value(indexStore)
        }
        textField(
            "localhost", "host", ""
        ) {
            value(hostStore)
        }
        textField(
            "9200", "port", ""
        ) {
            value(portStore)
        }
        switchField("Https", "Use https:// instead of http://") {
            value(httpsStore)
        }
        switchField(
            "Logging", "Turn on request logging in the client (use the browser console)."
        ) {
            value(loggingStore)
        }

        textField(
            "elastic", "user", ""
        ) {
            value(userStore)
        }
        textField(
            "secret", "password", ""
        ) {
            value(passwordStore)
        }
        textAreaField(
            placeHolder = """
            {
              "query": {
                "match": {
                  "title": "{{ query }}"
                }
              }
            }""".trimIndent(),
            label = "Query Template",
            description = "Paste a query and use variable names surrounded " + "by {{ myvariable }} where parameters from your search context will be substituted"
        ) {
            value(queryTemplateStore)
        }
        textField(
            "title,author",
            "Label fields",
            "Comma separated list of fields that will be used to generate the labels for your search results"
        ) {
            value(labelFieldsStore)
        }

        val metricConfigurationsStore = storeOf(existing?.metrics.orEmpty())
        val settingsGenerator = {
            ElasticsearchPluginConfiguration(
                queryTemplate = queryTemplateStore.current,
                index = indexStore.current,
                labelFields = labelFieldsStore.current.split(',').map { it.trim() },
                host = hostStore.current,
                port = portStore.current.toIntOrNull() ?: 9200,
                https = httpsStore.current,
                user = userStore.current,
                password = passwordStore.current,
                logging = loggingStore
                    .current
            ).let { DEFAULT_PRETTY_JSON.encodeToJsonElement<ElasticsearchPluginConfiguration>(it) }.jsonObject
        }
        pluginEditorButtonsAndSearchContextEditor(
            selectedPluginStore = selectedPluginStore,
            existing = existing,
            configNameStore = configNameStore,
            metricConfigurationsStore = metricConfigurationsStore,
            settingsGenerator = settingsGenerator,
            editConfigurationStore = editConfigurationStore,
            queryTemplateStore = queryTemplateStore,
        )
    }
}


