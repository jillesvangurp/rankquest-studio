package searchpluginconfig

import com.jilesvangurp.rankquest.core.DEFAULT_JSON
import com.jilesvangurp.rankquest.core.DEFAULT_PRETTY_JSON
import com.jilesvangurp.rankquest.core.SearchResults
import com.jilesvangurp.rankquest.core.pluginconfiguration.*
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
        activeSearchPluginConfigurationStore.data.render { activePluginConfig ->
            val activeIsDemo = activePluginConfig?.id in demoSearchPlugins.map { it.id }
            val showDemoContentStore = storeOf(activeIsDemo)
            div("flex flex-col items-left space-y-1 w-3/6 items-center m-auto") {
                if (activePluginConfig != null) {
                    para { +"Current configuration: ${activePluginConfig.name}" }
                } else {
                    para { +"No active search plugin comfiguration" }
                }
                val editConfigurationStore = storeOf<SearchPluginConfiguration?>(null)

                showDemoContentStore.data.filterNotNull().render { showDemoContent ->
                    pluginConfigurationStore.data.filterNotNull().render { configurations ->
//                        val editConfiguration = storeOf<SearchPluginConfiguration?>(null)
                        configurations.also {
                            if (it.isEmpty()) {
                                para {
                                    +"""You have no search plugin configurations yet. Add a 
                                    |configuration or use one of the demo configurations.""".trimMargin()
                                }
                            }
                        }.forEach { pluginConfig ->
                            val metricConfigurationsStore = storeOf(pluginConfig.metrics)
                            val showMetricsEditor = storeOf(false)
                            div("flex flex-row w-full items-center") {
                                div("mr-5 w-2/6 text-right") {
                                    +pluginConfig.name
                                }
                                div("w-4/6") {

                                    secondaryButton {
                                        +"Edit"
                                        clicks.map { pluginConfig } handledBy editConfigurationStore.update
                                    }
                                    secondaryButton {
                                        +"Metrics"
                                        clicks.map { true } handledBy showMetricsEditor.update
                                    }

                                    primaryButton {
                                        +"Use"
                                        disabled(activePluginConfig?.id == pluginConfig.id)
                                        clicks.map { pluginConfig } handledBy activeSearchPluginConfigurationStore.update
                                    }
                                    secondaryButton {
                                        +"Delete"
                                        clicks.map { pluginConfig.id } handledBy pluginConfigurationStore.remove
                                    }
                                }
                                metricsEditor(showMetricsEditor, metricConfigurationsStore)
                            }
                        }
                        listOf(movieQuotesSearchPluginConfig, movieQuotesNgramsSearchPluginConfig)

                        switchField("Show Demo Plugins") {
                            value(showDemoContentStore)
                        }
                        if(showDemoContent) {
                            secondaryButton {
                                +"Add Movie Quotes Search"
                                clicks handledBy {
                                    val c = movieQuotesSearchPluginConfig
                                    if(pluginConfigurationStore.current?.map { it.id }?.contains(c.id) != true) {
                                        pluginConfigurationStore.update((pluginConfigurationStore.current.orEmpty()) + c)
                                    }
                                }
                            }
                            secondaryButton {
                                +"Add Movie Quotes Search with n-grams"
                                clicks handledBy {
                                    val c = movieQuotesNgramsSearchPluginConfig
                                    if(pluginConfigurationStore.current?.map { it.id }?.contains(c.id) != true) {
                                        pluginConfigurationStore.update((pluginConfigurationStore.current.orEmpty()) + c)
                                    }
                                }
                            }
                        }
                    }
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
                                jsonGetEditor(
                                    selectedPluginStore = selectedPluginStore,
                                    configName = configName,
                                    editConfigurationStore = editConfigurationStore
                                )
                            }

                            BuiltinPlugins.JsonPostAPIPlugin -> {
                                jsonPostEditor(
                                    selectedPluginStore = selectedPluginStore,
                                    configName = configName,
                                    editConfigurationStore = editConfigurationStore
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

fun RenderContext.jsonGetEditor(
    selectedPluginStore: Store<String>,
    configName: Store<String>,
    editConfigurationStore: Store<SearchPluginConfiguration?>
) {
    editConfigurationStore.data.render { existing ->
        p {
            +"NOT IMPLEMENTED YET"
        }
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
//                clicks.map {
//                    SearchPluginConfiguration(
//                        id = existing?.id ?: md5Hash(Random.nextLong()),
//                        name = configName.current,
//                        pluginType = BuiltinPlugins.ElasticSearch.name,
//                        fieldConfig = templateVariableStore.current,
//                        metrics = metricConfigurationsStore.current,
//                        pluginSettings = ElasticsearchPluginConfiguration(
//                            queryTemplate = queryTemplate.current,
//                            index = index.current,
//                            labelFields = labelFields.current.split(',').map { it.trim() },
//                            host = host.current,
//                            port = port.current.toIntOrNull() ?: 9200,
//                            https = https.current,
//                            user = user.current,
//                            password = password.current,
//                            logging = logging.current
//                        ).let { DEFAULT_PRETTY_JSON.encodeToJsonElement(it) }.jsonObject
//                    ).also { updated ->
//                        activeSearchPluginConfigurationStore.current?.let { active ->
//                            // a little hacky but it ensures everything uses the new plugin
//                            if (active.id == updated.id) {
//                                activeSearchPluginConfigurationStore.update(updated)
//                            }
//                        }
//                    }
//                } handledBy pluginConfigurationStore.addOrReplace
                clicks handledBy {
                    // hide the overlay
                    selectedPluginStore.update("-")
                    editConfigurationStore.update(null)
                }
            }
        }
    }
}

fun RenderContext.jsonPostEditor(
    selectedPluginStore: Store<String>,
    configName: Store<String>,
    editConfigurationStore: Store<SearchPluginConfiguration?>
) {
    editConfigurationStore.data.render { existing ->
        p {
            +"NOT IMPLEMENTED YET"
        }
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
//                clicks.map {
//                    SearchPluginConfiguration(
//                        id = existing?.id ?: md5Hash(Random.nextLong()),
//                        name = configName.current,
//                        pluginType = BuiltinPlugins.ElasticSearch.name,
//                        fieldConfig = templateVariableStore.current,
//                        metrics = metricConfigurationsStore.current,
//                        pluginSettings = ElasticsearchPluginConfiguration(
//                            queryTemplate = queryTemplate.current,
//                            index = index.current,
//                            labelFields = labelFields.current.split(',').map { it.trim() },
//                            host = host.current,
//                            port = port.current.toIntOrNull() ?: 9200,
//                            https = https.current,
//                            user = user.current,
//                            password = password.current,
//                            logging = logging.current
//                        ).let { DEFAULT_PRETTY_JSON.encodeToJsonElement(it) }.jsonObject
//                    ).also { updated ->
//                        activeSearchPluginConfigurationStore.current?.let { active ->
//                            // a little hacky but it ensures everything uses the new plugin
//                            if (active.id == updated.id) {
//                                activeSearchPluginConfigurationStore.update(updated)
//                            }
//                        }
//                    }
//                } handledBy pluginConfigurationStore.addOrReplace
                clicks handledBy {
                    // hide the overlay
                    selectedPluginStore.update("-")
                    editConfigurationStore.update(null)
                }
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

        val metricConfigurationsStore = storeOf(existing?.metrics.orEmpty())

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
                        metrics = metricConfigurationsStore.current,
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

fun RenderContext.metricsEditor(
    showMetricsEditor: Store<Boolean>,
    metricConfigurationsStore: Store<List<MetricConfiguration>>
) {
    val editMetricStore = storeOf<MetricConfiguration?>(null)
    val showMetricsPickerStore = storeOf(false)
    val newMetricTypeStore = storeOf<Metric?>(null)
    showMetricsEditor.data.render { show ->
        if (show) {
            overlayLarge {

                metricConfigurationsStore.data.render { mcs ->
                    h2 { +"Metric Configuration" }
                    mcs.forEach { mc ->
                        div("flex flex-row place-items-center") {
                        div("flex flex-col") {
                            div {
                                +"${mc.name} (${mc.metric})"
                            }
                            div {
                                +mc.params.map { "${it.name} = ${it.value}" }.joinToString(", ")
                            }
                        }
                            secondaryButton {
                                +"Delete"
                            }
                            primaryButton {
                                +"Edit"
                                clicks.map { mc } handledBy editMetricStore.update
                            }
                        }
                        editMetricStore.data.render { editMetricConfiguration ->
                            val paramMap = mc.params.map { it.name to storeOf(it.value.content) }.toMap()
                            if (editMetricConfiguration?.name == mc.name) {
                                mc.params.forEach { p ->
                                    textField("", p.name) {
                                        value(paramMap[p.name]!!)
                                    }
                                }
                                primaryButton {
                                    +"Save Params"
                                    clicks handledBy {
                                        val newValues = paramMap.map { (name, valueStore) ->
                                            MetricParam(name, valueStore.current.let { s ->
                                                when {
                                                    s.toIntOrNull() != null -> {
                                                        s.toIntOrNull()!!.primitive
                                                    }

                                                    s.lowercase() in listOf("true", "false") -> {
                                                        s.toBoolean().primitive
                                                    }

                                                    else -> {
                                                        s.primitive
                                                    }
                                                }
                                            })
                                        }
                                        metricConfigurationsStore.update(
                                            metricConfigurationsStore.current.map {
                                                if (it.name == mc.name) {
                                                    it.copy(params = newValues)
                                                } else {
                                                    it
                                                }
                                            }
                                        )
                                        editMetricStore.update(null)
                                        showMetricsEditor.update(false)

                                    }
                                }
                            }
                        }
                    }

                    showMetricsPickerStore.data.render { showMetricsPicker ->
                        if (showMetricsPicker) {
                            newMetricTypeStore.data.render { selectedMetric ->

                                if (selectedMetric == null) {
                                    para {
                                        +"What metric type do you want to create?"
                                    }
                                    div("flex flex-row gap-3") {
                                        Metric.entries.forEach { metric ->
                                            a {
                                                +metric.name
                                                clicks.map { metric } handledBy newMetricTypeStore.update
                                            }
                                        }
                                    }
                                } else {
                                    h2 { +"Create new $selectedMetric metric" }
                                    val metricNameStore = storeOf(selectedMetric.name)
                                    textField("", "Metric Name", "Pick a unique name") {
                                        value(metricNameStore)
                                    }
                                    div("flex flex-row") {
                                        secondaryButton {
                                            +"Cancel"
                                            clicks handledBy {
                                                showMetricsPickerStore.update(false)
                                                newMetricTypeStore.update(null)
                                            }
                                        }

                                        primaryButton {
                                            +"OK"
                                            disabled(mcs.map { it.name }
                                                .contains(metricNameStore.current) || metricNameStore.current.isBlank())
                                            clicks handledBy {
                                                showMetricsPickerStore.update(false)
                                                newMetricTypeStore.update(null)
//                                                showMetricsEditor.update(false)
                                                val newConfig = MetricConfiguration(
                                                    metric = selectedMetric,
                                                    name = metricNameStore.current,
                                                    params = selectedMetric.supportedParams
                                                )
                                                editMetricStore.update(newConfig)
                                                metricConfigurationsStore.update(
                                                    mcs + newConfig
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            editMetricStore.data.render { currentMetric ->
                                if (currentMetric == null) {
                                    div("flex flex-row") {
                                        secondaryButton {
                                            +"Cancel"
                                            clicks handledBy {
                                                showMetricsPickerStore.update(false)
                                                newMetricTypeStore.update(null)
                                                showMetricsEditor.update(false)
                                            }
                                        }
                                        primaryButton {
                                            +"Add new Metric"
                                            clicks.map { true } handledBy showMetricsPickerStore.update
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            div("flex flex-row") {
                secondaryButton {
                    +"Cancel"
                    clicks.map { false } handledBy showMetricsEditor.update
                }
                primaryButton {
                    +"Save"
                    clicks handledBy {
                        showMetricsEditor.update(false)
                        newMetricTypeStore.update(null)
                    }
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
            val defaultValueStore = when (field) {
                is SearchContextField.BoolField -> storeOf(field.defaultValue.toString())
                is SearchContextField.IntField -> storeOf(field.defaultValue.toString())
                is SearchContextField.StringField -> storeOf(field.defaultValue.toString())
            }
            val placeHolderStore = when (field) {
                is SearchContextField.BoolField -> storeOf("")
                is SearchContextField.IntField -> storeOf(field.placeHolder)
                is SearchContextField.StringField -> storeOf(field.placeHolder)
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


