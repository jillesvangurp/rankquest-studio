package searchpluginconfig

import com.jilesvangurp.rankquest.core.DEFAULT_PRETTY_JSON
import com.jilesvangurp.rankquest.core.pluginconfiguration.*
import com.jilesvangurp.rankquest.core.plugins.BuiltinPlugins
import components.*
import dev.fritz2.core.*
import examples.quotesearch.movieQuotesNgramsSearchPluginConfig
import examples.quotesearch.movieQuotesSearchPluginConfig
import koin
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.w3c.dom.HTMLDivElement

val configurationModule = module {
    singleOf(::PluginConfigurationsStore)
    singleOf(::ActiveSearchPluginConfigurationStore)
}

fun RenderContext.pluginConfiguration() {
    centeredMainPanel {
        val pluginConfigurationStore = koin.get<PluginConfigurationsStore>()
        val activeSearchPluginConfigurationStore = koin.get<ActiveSearchPluginConfigurationStore>()
        activeSearchPluginConfigurationStore.data.render { activePluginConfig ->
            val showDemoContentStore = storeOf(false)
            if (activePluginConfig != null) {
                para {
                    +"Current configuration: "
                    strong {
                        +activePluginConfig.name
                    }
                }
            } else {
                para { +"No active search plugin comfiguration" }
            }
            val editConfigurationStore = storeOf<SearchPluginConfiguration?>(null)

            showDemoContentStore.data.filterNotNull().render { showDemoContent ->
                pluginConfigurationStore.data.filterNotNull().render { configurations ->
                    configurations.also {
                        if (it.isEmpty()) {
                            para {
                                +"""You have no search plugin configurations yet. Add a 
                                    |configuration or use one of the demo configurations.""".trimMargin()
                            }
                        }
                    }.forEach { pluginConfig ->
                        val metricConfigurationsStore = storeOf(pluginConfig.metrics)
                        metricConfigurationsStore.data handledBy { newMetrics ->
                            pluginConfigurationStore.addOrReplace(
                                pluginConfig.copy(
                                    metrics = newMetrics
                                )
                            )
                        }
                        val showMetricsEditor = storeOf(false)
                        div("flex flex-row w-full items-center") {
                            div("mr-5 w-2/6 text-right") {
                                +pluginConfig.name
                            }
                            div("w-4/6 place-items-center") {
                                row {

                                    secondaryButton(text = "Edit", iconSource = SvgIconSource.Pencil) {
                                        // can't edit the demo plugins
                                        disabled(pluginConfig.pluginType !in BuiltinPlugins.entries.map { it.name })
                                        clicks.map { pluginConfig } handledBy editConfigurationStore.update
                                    }
                                    secondaryButton(text = "Metrics", iconSource = SvgIconSource.Equalizer) {
                                        clicks.map { true } handledBy showMetricsEditor.update
                                    }

                                    secondaryButton(text = "Delete", iconSource = SvgIconSource.Cross) {
                                        clicks.map { pluginConfig.id } handledBy pluginConfigurationStore.remove
                                    }
                                    jsonDownloadButton(
                                        pluginConfig,
                                        "${pluginConfig.name}.json",
                                        SearchPluginConfiguration.serializer()
                                    )
                                    val inUse = activePluginConfig?.id == pluginConfig.id
                                    primaryButton(text = if (inUse) "Current" else "Use") {
                                        disabled(inUse)
                                        clicks.map { pluginConfig } handledBy activeSearchPluginConfigurationStore.update
                                    }
                                    help()
                                }
                            }
                            metricsEditor(showMetricsEditor, metricConfigurationsStore)

                        }
                    }
                    listOf(movieQuotesSearchPluginConfig, movieQuotesNgramsSearchPluginConfig)

                    switchField("Show Demo Plugins") {
                        value(showDemoContentStore)
                    }
                    if (showDemoContent) {
                        secondaryButton {
                            +"Add Movie Quotes Search"
                            clicks handledBy {
                                val c = movieQuotesSearchPluginConfig
                                if (pluginConfigurationStore.current?.map { it.id }?.contains(c.id) != true) {
                                    pluginConfigurationStore.update((pluginConfigurationStore.current.orEmpty()) + c)
                                }
                            }
                        }
                        secondaryButton {
                            +"Add Movie Quotes Search with n-grams"
                            clicks handledBy {
                                val c = movieQuotesNgramsSearchPluginConfig
                                if (pluginConfigurationStore.current?.map { it.id }?.contains(c.id) != true) {
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

private fun HtmlTag<HTMLDivElement>.help() {
    infoModal(
        title = "Configuration Screen",
        markdown = """
            The configuration screen is where you can configure your search plugins and metrics. 
            
            ## Demo plugins
            
            If you want to play around a little bit, there are two demo plugins that you can add that implement
            a simple movie search using an in memory search library that I wrote called 
            [querylight](https://github.com/jillesvangurp/querylight). 
            
            While simple, it can be nice for simple search use cases when you don't want to use a server. 
            It uses tf/idf for ranking and can perform well
            on small data sets. Which makes it great for trying out Rankquest Studio.
            
            There are two plugins with slightly different analyzers. One of them uses ngrams and the other one uses a 
            traditional analyzer. The ngrams based implementation of course performs a lot worse and this should show 
            when you run the metrics.
            
            ## Plugin types
            
            Currently there are three built in plugins:
            
            - Elasticsearch - you can use this to run metrics for elasticsearch plugins.
            - JsonGETApiPlugin - use this to configure search services that you cal call with an HTTP GET that return json
            - JsonPOSTApiPlugin - use this to configure search services that you call with an HTTP POST that return json
                        
            ## Switching between plugins
            
            You can configure multiple plugins and then switch between them with the use button. 
            The active configuration has a greyed out current button. 
                       
            ## Configuring metrics
            
            For each plugin configuration, you can define which metrics you want to evaluate and how to 
            configure those metrics. For example, you might want to add a few variations of precision@k with different
            k values.
       
            ## Import and export
            
            You can export your plugin configurations and re-import them. 
            You should keep your configurations in a safe place. It is good practice to keep them in a git repository
            together with your test cases.
            
            ## Writing your own plugins
            
            Writing your own plugins is possible of course. However, this will require modifying some source code. 
            You can either modify this project or add it as a built in plugin in 
            (rankquest-core)[https://github.com/jillesvangurp/rankquest-core]. If you do, also modify this project
            to add a configuration UI for your plugin.
                        
            A future version of rank quest may add some features to allow you to more easily add your own plugin 
            implementations via e.g. javascript.        
        """.trimIndent()
    )
}

fun RenderContext.createOrEditPlugin(editConfigurationStore: Store<SearchPluginConfiguration?>) {
    val pluginConfigurationStore = koin.get<PluginConfigurationsStore>()

    editConfigurationStore.data.render { existing ->

        val selectedPluginTypeStore = storeOf(existing?.pluginType ?: "")

        row {
            BuiltinPlugins.entries.forEach { p ->
                primaryButton {
                    +"New ${p.name}"
                    clicks.map { p.name } handledBy selectedPluginTypeStore.update
                }
            }
            jsonFileImport(SearchPluginConfiguration.serializer()) { decoded ->
                pluginConfigurationStore.addOrReplace(decoded)
            }
        }

        selectedPluginTypeStore.data.render { selectedPlugin ->
            BuiltinPlugins.entries.firstOrNull { it.name == selectedPlugin }?.let { plugin ->
                overlayLarge {
                    val configNameStore = storeOf(plugin.name)

                    div("flex flex-col items-left space-y-1 w-5/6 items-center m-auto") {
                        h1 { +"New search configuration for $selectedPlugin" }
                        textField(
                            placeHolder = selectedPlugin, "Name", "A descriptive name for your configuration"
                        ) {
                            value(configNameStore)
                        }
                        // plugin settings
                        when (plugin) {
                            BuiltinPlugins.ElasticSearch -> elasticsearchEditor(
                                selectedPluginStore = selectedPluginTypeStore,
                                configNameStore = configNameStore,
                                editConfigurationStore = editConfigurationStore
                            )

                            BuiltinPlugins.JsonGetAPIPlugin -> {
                                httpGetPluginEditor(
                                    selectedPluginStore = selectedPluginTypeStore,
                                    configNameStore = configNameStore,
                                    editConfigurationStore = editConfigurationStore
                                )
                            }

                            BuiltinPlugins.JsonPostAPIPlugin -> httpPostPluginEditor(
                                selectedPluginStore = selectedPluginTypeStore,
                                configNameStore = configNameStore,
                                editConfigurationStore = editConfigurationStore
                            )
                        }
                    }
                }
            }
        }
    }
}

fun RenderContext.metricsEditor(
    showMetricsEditor: Store<Boolean>, metricConfigurationsStore: Store<List<MetricConfiguration>>
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
                        div("flex flex-row align-middle justify-between") {
                            div("flex flex-col") {
                                div {
                                    +"${mc.name} (${mc.metric})"
                                }
                                div {
                                    +mc.params.joinToString(", ") { "${it.name} = ${it.value}" }
                                }
                            }
                            row {
                                secondaryButton {
                                    +"Delete"
                                    clicks handledBy {
                                        confirm {
                                            metricConfigurationsStore.update(metricConfigurationsStore.current.filter { it.name != mc.name })
                                        }
                                    }
                                }
                                primaryButton {
                                    +"Edit"
                                    clicks.map { mc } handledBy editMetricStore.update
                                }
                            }
                        }
                        editMetricStore.data.render { editMetricConfiguration ->
                            val paramMap = mc.params.associate { it.name to storeOf(it.value.content) }
                            if (editMetricConfiguration?.name == mc.name) {
                                val nameStore = storeOf(mc.name)
                                textField("", "name") {
                                    value(nameStore)
                                }
                                mc.params.forEach { p ->
                                    textField("", p.name) {
                                        value(paramMap[p.name]!!)
                                    }
                                }
                                row {
                                    secondaryButton {
                                        +"Cancel"
                                        clicks handledBy {
                                            editMetricStore.update(null)
                                            showMetricsEditor.update(false)
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
                                            metricConfigurationsStore.update(metricConfigurationsStore.current.map {
                                                if (it.name == mc.name) {
                                                    it.copy(
                                                        name = nameStore.current, params = newValues
                                                    )
                                                } else {
                                                    it
                                                }
                                            })

                                            editMetricStore.update(null)
                                            showMetricsEditor.update(false)

                                        }
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
                                    row {
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
                                    row {
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
                                    row {
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
            row {
                secondaryButton {
                    +"Cancel"
                    clicks.map { false } handledBy showMetricsEditor.update
                }
                primaryButton {
                    +"Save"
                    clicks handledBy {
                        showMetricsEditor.update(false)
                        newMetricTypeStore.update(null)
                        // FIXME does not save anything
                    }
                }
            }
        }
    }
}





