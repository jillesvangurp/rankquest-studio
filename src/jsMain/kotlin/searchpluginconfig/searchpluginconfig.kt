package searchpluginconfig

import com.jilesvangurp.rankquest.core.DEFAULT_JSON
import com.jilesvangurp.rankquest.core.DEFAULT_PRETTY_JSON
import com.jilesvangurp.rankquest.core.SearchResults
import com.jilesvangurp.rankquest.core.pluginconfiguration.SearchContextField
import com.jilesvangurp.rankquest.core.pluginconfiguration.SearchPluginConfiguration
import com.jilesvangurp.rankquest.core.plugins.BuiltinPlugins
import com.jilesvangurp.rankquest.core.plugins.ElasticsearchPluginConfiguration
import com.jilesvangurp.rankquest.core.plugins.PluginFactoryRegistry
import components.*
import dev.fritz2.core.*
import examples.quotesearch.demoSearchPlugins
import examples.quotesearch.movieQuotesNgramsSearchPluginConfig
import examples.quotesearch.movieQuotesSearchPluginConfig
import handlerScope
import koin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.w3c.dom.HTMLHeadingElement
import search.SearchResultsStore
import utils.md5Hash
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds


val configurationModule = module {
    singleOf(::PluginConfigurationsStore)
    singleOf(::ActiveSearchPluginConfigurationStore)
}

class PluginConfigurationsStore : LocalStoringStore<List<SearchPluginConfiguration>>(
    listOf(), "plugin-configurations", ListSerializer(SearchPluginConfiguration.serializer())
) {
    val addOrReplace = handle<SearchPluginConfiguration> { old, config ->
        (current ?: listOf()).map {
            if (it.id == config.id) {
                config
            } else {
                it
            }
        }.let {
            if (it.firstOrNull { it.id == config.id } == null) {
                it + config
            } else {
                it
            }
        }
    }
    val remove = handle<String> { old, id ->
        confirm {
            update((current ?: listOf()).filter { it.id != id })
        }
        old
    }
}

class ActiveSearchPluginConfigurationStore : LocalStoringStore<SearchPluginConfiguration?>(
    null, "active-search-plugin-configuration", SearchPluginConfiguration.serializer().nullable
) {
    // using get forces an early init ;-), fixes bug where first search is empty because it does not create the store until you use it
    private val searchResultsStore = koin.get<SearchResultsStore>()
    private val pluginFactoryRegistry = koin.get<PluginFactoryRegistry>()

    val search = handle<Map<String, String>> { config, query ->
        busyResult({
            var outcome: Result<SearchResults>? = null
            coroutineScope {
                launch {
                    if (config != null) {
                        val selectedPlugin = current
                        if (selectedPlugin != null) {
                            handlerScope.launch {
                                console.log("SEARCH $query")
                                val searchPlugin = pluginFactoryRegistry.get(config.pluginType)?.create(config)
                                outcome = searchPlugin?.fetch(query, query["size"]?.toInt() ?: 10)
                            }
                        } else {
                            outcome = Result.failure(IllegalArgumentException("no plugin selected"))
                        }
                    }
                }
                // whichever takes longer; make sure the spinner doesn't flash in and out
                launch {
                    delay(200.milliseconds)
                }
            }.join()
            searchResultsStore.update(outcome)
            Result.success(true)
        }, initialTitle = "Searching", initialMessage = "Query for $query")
        config
    }
}

fun RenderContext.pluginConfiguration() {
    val pluginConfigurationStore = koin.get<PluginConfigurationsStore>()
    val activeSearchPluginConfigurationStore = koin.get<ActiveSearchPluginConfigurationStore>()
    div {
        h1(content = fun HtmlTag<HTMLHeadingElement>.() {
            +"Search Plugin Configuration"
        })

        activeSearchPluginConfigurationStore.data.render { activePluginConfig ->
            val activeIsDemo = activePluginConfig?.id in demoSearchPlugins.map { it.id }
            val showDemoContent = storeOf(activeIsDemo)
            div("flex flex-col items-left space-y-1 w-3/6 items-center m-auto") {
                if (activePluginConfig != null) {
                    para { +"Current configuration: ${activePluginConfig.name}" }
                } else {
                    para { +"No active search plugin comfiguration" }
                }
                val editConfigurationStore = storeOf<SearchPluginConfiguration?>(null)
                showDemoContent.data.filterNotNull().render { demoContentEnabled ->
                    pluginConfigurationStore.data.filterNotNull().render { configurations ->
                        val editConfiguration = storeOf<SearchPluginConfiguration?>(null)
                        if (demoContentEnabled) {
                            configurations + demoSearchPlugins
                        } else {
                            configurations
                        }.also {
                            if (it.isEmpty()) {
                                para {
                                    +"""You have no search plugin configurations yet. Add a 
                                    |configuration or use one of the demo configurations.""".trimMargin()
                                }
                            }
                        }.forEach { pluginConfig ->

                            div("flex flex-row w-full items-center") {
                                div("mr-5 w-2/6 text-right") {
                                    +pluginConfig.name
                                }
                                div("w-4/6") {
                                    val editable = pluginConfig.id in demoSearchPlugins.map { dp -> dp.id }
                                    secondaryButton {
                                        +"Edit"
                                        disabled(editable)
                                        clicks.map { pluginConfig } handledBy editConfigurationStore.update

                                    }
                                    primaryButton {
                                        +"Use"
                                        disabled(activePluginConfig?.id == pluginConfig.id)
                                        clicks.map { pluginConfig } handledBy activeSearchPluginConfigurationStore.update
                                    }
                                    secondaryButton {
                                        +"Delete"
                                        disabled(editable)
                                        clicks.map { pluginConfig.id } handledBy pluginConfigurationStore.remove
                                    }
                                }
                            }
                        }
                        listOf(movieQuotesSearchPluginConfig, movieQuotesNgramsSearchPluginConfig)

                    }
                }
                switchField("Show Demo Plugins") {
                    value(showDemoContent)
                }

                createOrEditPlugin(editConfigurationStore)

                if (activePluginConfig != null) {
                    val showStore = storeOf(false)
                    showStore.data.render { show ->
                        a {
                            +"Show json"
                            clicks.map { showStore.current.not() } handledBy showStore.update
                        }
                        if (show) {
                            pre("overflow-auto w-full") {
                                console.log(DEFAULT_PRETTY_JSON.encodeToString(activePluginConfig))
                                +DEFAULT_PRETTY_JSON.encodeToString(activePluginConfig)
                            }
                        }
                    }
                }
            }
        }
    }
}

fun RenderContext.createOrEditPlugin(editConfigurationStore: Store<SearchPluginConfiguration?>) {
    editConfigurationStore.data.render { existing ->

        val selectedPluginStore = storeOf(existing?.pluginType ?: "")

        div("flex flex-row") {
            BuiltinPlugins.entries.forEach { p ->
                primaryButton {
                    +"New ${p.name}"
                    clicks.map { p.name } handledBy selectedPluginStore.update
                }
            }
        }

        selectedPluginStore.data.render { selectedPlugin ->
            BuiltinPlugins.entries.firstOrNull { it.name == selectedPlugin }?.let { plugin ->
                overlayLarge {
                    val configName = storeOf(plugin.name)

                    div("flex flex-col items-left space-y-1 w-3/6 items-center m-auto") {
                        h1 { +"New search configuration for $selectedPlugin" }
                        textField(
                            placeHolder = selectedPlugin, "Name", "A descriptive name for your configuration"
                        ) {
                            value(configName)
                        }
                        // plugin settings
                        when (plugin) {
                            BuiltinPlugins.ElasticSearch -> elasticsearchEditor(
                                selectedPluginStore = selectedPluginStore,
                                configName = configName,
                                editConfigurationStore = editConfigurationStore
                            )

                            BuiltinPlugins.JsonGetAPIPlugin -> {
                                p {
                                    +"TODO"
                                }
                            }

                            BuiltinPlugins.JsonPostAPIPlugin -> {
                                p {
                                    +"TODO"
                                }
                            }
                        }
                    }
                }
                // search context config
                // metric config
            }
        }
    }
}

fun RenderContext.elasticsearchEditor(
    selectedPluginStore: Store<String>,
    configName: Store<String>,
    editConfigurationStore: Store<SearchPluginConfiguration?>
) {
    val activeSearchPluginConfigurationStore = koin.get<ActiveSearchPluginConfigurationStore>()

    editConfigurationStore.data.render { existing ->
        val pluginConfigurationStore = koin.get<PluginConfigurationsStore>()
        val settings = existing?.pluginSettings?.let {
            DEFAULT_JSON.decodeFromJsonElement(
                ElasticsearchPluginConfiguration.serializer(), it
            )
        }

        val queryTemplate = storeOf(
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
        val index = storeOf(settings?.index ?: "")
        val labelFields = storeOf(settings?.labelFields?.joinToString(", ") ?: "titel, author.name")
        val host = storeOf(settings?.host ?: "")
        val port = storeOf(settings?.port?.toString() ?: "")
        val https = storeOf(settings?.https ?: false)
        val user = storeOf(settings?.user ?: "")
        val password = storeOf(settings?.password ?: "")
        val logging = storeOf(settings?.logging ?: false)

        textField("myindex", "index", "Index or alias name that you want to query") {
            value(index)
        }
        textField(
            "localhost", "host", ""
        ) {
            value(host)
        }
        textField(
            "9200", "port", ""
        ) {
            value(port)
        }
        switchField("Https", "Use https:// instead of http://") {
            value(https)
        }
        switchField(
            "Logging", "Turn on request logging in the client (use the browser console)."
        ) {
            value(logging)
        }

        textField(
            "elastic", "user", ""
        ) {
            value(user)
        }
        textField(
            "secret", "password", ""
        ) {
            value(password)
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
            value(queryTemplate)
        }
        textField(
            "title,author",
            "Label fields",
            "Comma separated list of fields that will be used to generate the labels for your search results"
        ) {
            value(labelFields)
        }
        val templateVariableStore = storeOf(existing?.fieldConfig.orEmpty())
        templateVarEditor(templateVariableStore, queryTemplate)

        div("flex flex-row") {
            secondaryButton {
                +"Cancel"
                clicks.map { "_" } handledBy selectedPluginStore.update
            }
            primaryButton {
                if (existing == null) {
                    +"Add Configuration"
                } else {
                    +"Save"
                }
                clicks.map {
                    SearchPluginConfiguration(
                        id = existing?.id ?: md5Hash(Random.nextLong()),
                        name = configName.current,
                        pluginType = BuiltinPlugins.ElasticSearch.name,
                        fieldConfig = templateVariableStore.current,
                        metrics = listOf(),
                        pluginSettings = ElasticsearchPluginConfiguration(
                            queryTemplate = queryTemplate.current,
                            index = index.current,
                            labelFields = labelFields.current.split(',').map { it.trim() },
                            host = host.current,
                            port = port.current.toIntOrNull() ?: 9200,
                            https = https.current,
                            user = user.current,
                            password = password.current,
                            logging = logging.current
                        ).let { DEFAULT_PRETTY_JSON.encodeToJsonElement(it) }.jsonObject
                    ).also { updated ->
                        activeSearchPluginConfigurationStore.current?.let { active ->
                            // a little hacky but it ensures everything uses the new plugin
                            if (active.id == updated.id) {
                                activeSearchPluginConfigurationStore.update(updated)
                            }
                        }
                    }
                } handledBy pluginConfigurationStore.addOrReplace
                clicks handledBy {
                    // hide the overlay
                    selectedPluginStore.update("")
                    editConfigurationStore.update(null)
                }
            }
        }
    }
}


fun RenderContext.templateVarEditor(
    templateVarStore: Store<List<SearchContextField>>,
    queryTemplateStore: Store<String>
) {
    queryTemplateStore.data handledBy {
        // make sure any new variables from the template are added
        val templateVarsRE = "\\{\\{\\s*(.*?)\\s*\\}\\}".toRegex(RegexOption.MULTILINE)
        val newVars = templateVarsRE.findAll(queryTemplateStore.current).let { matchResult ->
            matchResult.mapNotNull { m ->
                m.groups[1]?.value?.let { field ->
                    console.log(field)
                    SearchContextField.StringField(field)
                }
            }
        }.sortedBy { it.name }.distinctBy { it.name }.toList()

        newVars.filter { newVar ->
            console.log(newVar, templateVarStore.current.toString())
            templateVarStore.current.firstOrNull { it -> it.name == newVar.name } == null
        }.takeIf { it.isNotEmpty() }?.let {
            console.log(it.toString())
            templateVarStore.update((templateVarStore.current + it).distinctBy { it.name })
        }
    }

    h2 { +"Search Context Variables" }
    templateVarStore.data.render { fields ->

        fields.forEach { field ->
            val nameStore = storeOf(field.name)
            val typeStore = storeOf(field::class.simpleName!!)
            val defaultValueStore = when(field) {
                is SearchContextField.BoolField -> storeOf( field.defaultValue.toString())
                is SearchContextField.IntField -> storeOf( field.defaultValue.toString())
                is SearchContextField.StringField -> storeOf( field.defaultValue.toString())
            }
            val placeHolderStore = when(field) {
                is SearchContextField.BoolField -> storeOf( "")
                is SearchContextField.IntField -> storeOf( field.placeHolder)
                is SearchContextField.StringField -> storeOf( field.placeHolder)
            }             

            div("flex flex-row") {
                textField("", "name") {
                    value(nameStore)
                }
                defaultValueStore.data.render { defaultValue ->
                    when (field) {
                        is SearchContextField.BoolField -> {
                            val boolStore = storeOf(defaultValue.toBoolean())
                            boolStore.data handledBy {
                                defaultValueStore.update(it.toString())
                            }
                            switchField {
                                value(boolStore)
                            }
                        }

                        else -> {
                            textField("", "Default Value") {
                                value(defaultValueStore)
                            }
                            textField("", "PlaceHolder") {
                                value(placeHolderStore)
                            }

                        }
                    }
                }
                primaryButton {
                    +"OK"
                    clicks handledBy {
                        val updatedField = when (typeStore.current) {
                            SearchContextField.BoolField::class.simpleName!! -> {
                                SearchContextField.BoolField(
                                    name = nameStore.current,
                                    defaultValue = defaultValueStore.current.toBoolean()
                                )
                            }

                            SearchContextField.IntField::class.simpleName!! -> {
                                SearchContextField.IntField(
                                    name = nameStore.current,
                                    defaultValue = defaultValueStore.current.toIntOrNull() ?: 0,
                                    placeHolder = placeHolderStore.current,
                                )

                            }

                            else -> {
                                SearchContextField.StringField(
                                    name = nameStore.current,
                                    defaultValue = defaultValueStore.current,
                                    placeHolder = placeHolderStore.current,
                                )
                            }
                        }
                        templateVarStore.update(templateVarStore.current.map {
                            if (it.name == updatedField.name) {
                                updatedField
                            } else {
                                it
                            }
                        })
                    }
                }
            }

            div("flex flex-row") {
                typeStore.data.render { fieldType ->
                    div {
                        primaryButton {
                            +"int"
                            disabled(fieldType == SearchContextField.IntField::class.simpleName!!)
                            clicks.map { SearchContextField.IntField::class.simpleName!! } handledBy typeStore.update
                        }
                        primaryButton {
                            +"bool"
                            disabled(fieldType == SearchContextField.BoolField::class.simpleName!!)
                            clicks.map { SearchContextField.BoolField::class.simpleName!! } handledBy typeStore.update
                        }
                        primaryButton {
                            +"string"
                            disabled(fieldType == SearchContextField.StringField::class.simpleName!!)
                            clicks.map { SearchContextField.StringField::class.simpleName!! } handledBy typeStore.update
                        }
                    }
                }
            }
        }
    }
}


