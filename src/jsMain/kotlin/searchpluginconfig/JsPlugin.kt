package searchpluginconfig

import com.jilesvangurp.rankquest.core.DEFAULT_JSON
import com.jilesvangurp.rankquest.core.SearchPlugin
import com.jilesvangurp.rankquest.core.SearchResults
import com.jilesvangurp.rankquest.core.pluginconfiguration.SearchPluginConfiguration
import com.jilesvangurp.rankquest.core.plugins.PluginFactory
import components.textAreaField
import dev.fritz2.core.RenderContext
import dev.fritz2.core.Store
import dev.fritz2.core.storeOf
import koin
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject

@Serializable
data class JsPluginConfiguration(val javascript: String)

val defaultJs = """
// add your function to window 
// so we can call it
window.rankquestSearch=
    function(searchContext, numberOfItems) {
    
    // use fetch to fetch some results 
    // from somewhere or do whatever 
    // you need to do  with javascript
    
    // construct a response that 
    // looks something like this 
    var response = {
        "total":1,
        "responseTime":"PT0.001S",
        "searchResultList":[
            {
                "id":"1",
                "label":"Title"
            }
        ]
    }
    // return valid json as a string
    return JSON.stringify(response)
}
    
""".trimIndent()

class JsPlugin(val javascript: String = defaultJs) : SearchPlugin {

    override suspend fun fetch(searchContext: Map<String, String>, numberOfItemsToFetch: Int): Result<SearchResults> {
        return try {
            runArbitraryJs(javascript)
            val params = DEFAULT_JSON.encodeToString(MapSerializer(String.serializer(),String.serializer()), searchContext)
            val value = js("window.rankquestSearch( JSON.parse(params), numberOfItemsToFetch)") as String
            Result.success(DEFAULT_JSON.decodeFromString<SearchResults>(value))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class JsPluginFactory: PluginFactory {
    override fun create(configuration: SearchPluginConfiguration): SearchPlugin {
        val settings = (configuration.pluginSettings?: error("js plugin configuration is missing pluginSettings")).let {
            DEFAULT_JSON.decodeFromJsonElement<JsPluginConfiguration>(it)
        }
        return JsPlugin(settings.javascript)
    }
}

fun runArbitraryJs(javascript: Any) {
    js("eval(javascript)")
}

external class Object
inline fun jsObj(init: dynamic.() -> Unit): dynamic {
    return (Object()).apply(init)
}

fun RenderContext.jsPluginEditor(
    selectedPluginStore: Store<String>,
    configNameStore: Store<String>,
    editConfigurationStore: Store<SearchPluginConfiguration?>

) {
    val pluginConfigurationStore = koin.get<PluginConfigurationsStore>()

    editConfigurationStore.data.render { existing ->
        val metricConfigurationsStore = storeOf(existing?.metrics?: StandardConfigurations.defaults)
        val settings = existing?.pluginSettings?.let {
            DEFAULT_JSON.decodeFromJsonElement(
                JsPluginConfiguration.serializer(), it
            )
        }
        val jsStore = storeOf(settings?.javascript ?: defaultJs)
        textAreaField(
            placeHolder = defaultJs,
            label = "Javascript function",
            description = "Add a function to window with the name rankquestSearch that returns `SearchResults` as a json string."
        ) {
            value(jsStore)
        }

        pluginEditorButtonsAndSearchContextEditor(
            selectedPluginStore = selectedPluginStore,
            existing = existing,
            configNameStore = configNameStore,
            metricConfigurationsStore = metricConfigurationsStore,
            settingsGenerator = {
                JsPluginConfiguration(jsStore.current).let {
                    DEFAULT_JSON.encodeToJsonElement<JsPluginConfiguration>(it).jsonObject
                }
            },
            editConfigurationStore = editConfigurationStore,
            queryTemplateStore = null,
            "Configuring thw Elasticsearch Plugin",
            """
Add a function to `window`  called `rankquestSearch` that returns `SearchResults` as a json string.

For example:
 
```javascript
$defaultJs
```
            """.trimIndent()
        )
    }
}