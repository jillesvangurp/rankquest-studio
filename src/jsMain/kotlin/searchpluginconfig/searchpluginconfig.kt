package searchpluginconfig

import com.jilesvangurp.rankquest.core.DEFAULT_JSON
import com.jilesvangurp.rankquest.core.DEFAULT_PRETTY_JSON
import com.jilesvangurp.rankquest.core.pluginconfiguration.*
import com.jilesvangurp.rankquest.core.plugins.BuiltinPlugins
import components.*
import dev.fritz2.core.*
import dev.fritz2.remote.http
import examples.quotesearch.MovieQuotesStore
import examples.quotesearch.movieQuotesNgramsSearchPluginConfig
import examples.quotesearch.movieQuotesSearchPluginConfig
import koin
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import kotlinx.coroutines.Job
import kotlinx.datetime.Clock
import kotlinx.serialization.builtins.ListSerializer
import metrics.explanation
import org.w3c.dom.HTMLDivElement

val configurationModule = module {
    singleOf(::PluginConfigurationsStore)
    singleOf(::ActiveSearchPluginConfigurationStore)
    single<Store<Boolean>>(named("showDemo")) {
        storeOf(false, Job())
    }
}

fun RenderContext.pluginConfiguration() {
    val pluginConfigurationStore = koin.get<PluginConfigurationsStore>()
    val activeSearchPluginConfigurationStore = koin.get<ActiveSearchPluginConfigurationStore>()
    val showDemoContentStore = koin.get<Store<Boolean>>(named("showDemo")) as Store<Boolean>
    val movieQuotesStore = koin.get<MovieQuotesStore>()
    val editMetricStore: Store<MetricConfiguration?> = storeOf(null)
    centeredMainPanel {

        val editConfigurationStore = storeOf<SearchPluginConfiguration?>(null)
        createOrEditPlugin(editConfigurationStore)
        activeSearchPluginConfigurationStore.data.render { activePluginConfig ->


            showDemoContentStore.data.filterNotNull().render { showDemoContent ->
                pluginConfigurationStore.data.filterNotNull().render { configurations ->

                    configurations.also {
                        if (it.isEmpty()) {
                            markdownDiv(
                                """
                                    You have no search plugin configurations yet. 
                                    
                                    Plugin configurations are needed to talk to the search APIs for which 
                                    you want to calculate search relevance metrics.
                                    
                                    Click the help button to learn more about this screen.                                                                   
                                """.trimIndent()
                            )
                        } else {
                            if (activePluginConfig != null) {
                                para {
                                    +"Current configuration: "
                                    strong {
                                        +activePluginConfig.name
                                    }
                                }
                            } else {
                                para {
                                    +"You have no active configuration. Click the 'Use' button on one of your configurations."
                                }
                            }
                        }
                    }.forEach { pluginConfig ->
                        val showMetricsEditor = storeOf(false)
                        val metricConfigurationsStore = storeOf(pluginConfig.metrics)
                        metricConfigurationsStore.data handledBy { newMetrics ->
                            if (pluginConfig.metrics != newMetrics) {
                                console.log("updating metrics for ${pluginConfig.name}")
                                pluginConfigurationStore.addOrReplace(
                                    pluginConfig.copy(
                                        metrics = newMetrics
                                    )
                                )
                            }
                        }
                        // can't edit the demo plugins
                        div("flex flex-row w-full place-items-center justify-between my-1", content = fun HtmlTag<HTMLDivElement>.() {
                            div {
                                +pluginConfig.name
                            }
                            div("place-items-end") {
                                flexRow {

                                    val inUse = activePluginConfig?.id == pluginConfig.id
                                    primaryButton(text = if (inUse) "Current" else "Use") {
                                        disabled(inUse)
                                        clicks.map { pluginConfig } handledBy activeSearchPluginConfigurationStore.update
                                    }
                                    secondaryButton(text = "Edit", iconSource = SvgIconSource.Pencil) {
                                        // can't edit the demo plugins
                                        disabled(pluginConfig.pluginType !in (BuiltinPlugins.entries.map { it.name } + "javascript"))
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
                                }
                            }
                            metricsEditor(showMetricsEditor, metricConfigurationsStore, editMetricStore)
                        })
                    }
                    div("w-full place-items-end mt-10") {
                        settings()
                        leftRightRow {
                            if (showDemoContent) {
                                a {
                                    +"Hide Demo Content"
                                    clicks.map { false } handledBy showDemoContentStore.update
                                }
                                flexRow {
                                    secondaryButton {
                                        +"Add Movie Quotes Search"
                                        clicks handledBy {
                                            val c = movieQuotesSearchPluginConfig
                                            if (pluginConfigurationStore.current?.map { it.id }
                                                    ?.contains(c.id) != true) {
                                                pluginConfigurationStore.update((pluginConfigurationStore.current.orEmpty()) + c)
                                            }
                                        }
                                    }
                                    secondaryButton {
                                        +"Add Movie Quotes Search with n-grams"
                                        clicks handledBy {
                                            val c = movieQuotesNgramsSearchPluginConfig
                                            if (pluginConfigurationStore.current?.map { it.id }
                                                    ?.contains(c.id) != true) {
                                                pluginConfigurationStore.update((pluginConfigurationStore.current.orEmpty()) + c)
                                            }
                                        }
                                    }
                                    secondaryButton {
                                        +"ES Based moviesearch"
                                        clicks handledBy {
                                            val config = http("es-movie-quotes-config.json").get().body().let {
                                                DEFAULT_JSON.decodeFromString<SearchPluginConfiguration>(it)
                                            }
                                            pluginConfigurationStore.addOrReplace(config)
                                        }

                                    }
                                    secondaryButton {
                                        +"Index movies into ES"
                                        clicks handledBy movieQuotesStore.indexEs
                                    }
                                    secondaryButton {
                                        +"Clean ES"
                                        clicks handledBy movieQuotesStore.delRecipesES
                                    }
                                    infoPopupFile("demo.md")
                                }
                            } else {
                                a {
                                    +"Show Demo Content"
                                    clicks.map { true } handledBy showDemoContentStore.update
                                }
                            }
                        }
                    }
                }
            }

            if (activePluginConfig != null) {
                val showStore = storeOf(false)
                showStore.data.render { show ->
                    if (show) {
                        a {
                            +"Hide configuration json"
                            clicks.map { showStore.current.not() } handledBy showStore.update
                        }
                        pre("overflow-auto w-full") {
                            console.log(DEFAULT_PRETTY_JSON.encodeToString(activePluginConfig))
                            +DEFAULT_PRETTY_JSON.encodeToString(activePluginConfig)
                        }
                    } else {
                        a {
                            +"Show configuration json for ${activePluginConfig.name}"
                            clicks.map { showStore.current.not() } handledBy showStore.update
                        }

                    }
                }
            }
        }
    }
}

val configurationScreenHelp = """
    The configuration screen is where you can configure your search plugins and metrics. A search plugin
     allows you to extract search results from your search API. The metrics configuration is then used 
     to configure the metrics for your plugin. Each metric has some parameters and you can add 
     different configurations for each metric. Additionally, each metric has a threshold that controls
      what an acceptable value is (green) and what is not acceptable (red).
    
    ## Demo content
    
    The best way to get familiar with Rankquest Studio is to use the demo content. There are three
    plugin configurations that you can use that each implement a movie quote search solution. 
    
    Two of the plugins use an in memory search library that I wrote called 
    [querylight](https://github.com/jillesvangurp/querylight). The third configuration uses 
    Elasticsearch (you need to have that running of course).
    
    Additionally, you can load demo ratings in the ratings screen (only if you have show demo content enabled). 
    You can use these ratings compare the search relevance metrics of the three demo search configurations. 
    
    ## Supported Plugins
    
    Currently there are several built in plugins that you may use to extract results from your search API:
    
    - **Json GET Api Plugin** - use this to configure search services that you cal call with an HTTP GET that returns results in json format.
    - **Json POST Api Plugin** - use this to configure search services that you call with an HTTP POST that returns results in json format.
    - **Elasticsearch** - you can use this to run metrics for elasticsearch queries. You could also do this with the JsonPOSTApiPlugin, of course. 
    However, this instead uses my kt-search library.
    - **Js Plugin** - With this plugin, you can add a custom javascript function to implement your own logic; 
    for example using the fetch API. Note. this of course **only works in the browser and cannot be used with rankquest-cli** currently.
    
    These plugins should cover most common APIs. But if the above is not enough, you can of course implement 
    your own plugins in rankquest core. You will also need to then modify rankquest studio to support it. This
    is not that hard and if you do, please consider contributing your plugin.  
    
    ## Using a proxy
    
    An alternative to developing a new plugin is using a proxy. You might create a simple proxy for your service 
    that maps your API to a simple Json API that can be used with the GET or POST json plugin. This is also a good way
    to deal with more complicated topics like authentication.
                
    ## Switching between plugins
    
    You can configure multiple plugins and then switch between them with the use button. 
    The active configuration has a greyed out current button. 
    
    The test cases are of course dependent on the search context but otherwise will work with any search configuration.
    This makes it really easy to evaluate different search implementations with the same test cases.
               
    ## Configuring metrics
    
    For each plugin configuration, you can define which metrics you want to evaluate and how to 
    configure those metrics. For example, you might want to add a few variations of precision@k with different
    k values.

    ## Import and export
    
    You can export your plugin configurations and re-import them. 
    You should keep your configurations in a safe place. It is good practice to keep them in a git repository
    together with your test cases.
    
    You also need to do this if you want to use [rankquest-cli](https://github.com/jillesvangurp/rankquest-cli).

""".trimIndent()

private fun RenderContext.help() {
    infoPopup(
        title = "Configuration Screen",
        markdown = configurationScreenHelp
    )
}

fun RenderContext.createOrEditPlugin(editConfigurationStore: Store<SearchPluginConfiguration?>) {
    val pluginConfigurationStore = koin.get<PluginConfigurationsStore>()

    editConfigurationStore.data.render { existing ->
        val selectedPluginTypeStore = storeOf(existing?.pluginType ?: "")

        leftRightRow {
            flexRow {
                BuiltinPlugins.entries.forEach { p ->
                    primaryButton {
                        +"New ${p.name}"
                        clicks.map { p.name } handledBy selectedPluginTypeStore.update
                    }
                }
                primaryButton {
                    +"New JS Plugin"
                    clicks.map { "javascript" } handledBy selectedPluginTypeStore.update
                }
                jsonFileImport(SearchPluginConfiguration.serializer()) { decoded ->
                    pluginConfigurationStore.addOrReplace(decoded)
                }
            }
            help()
        }

        selectedPluginTypeStore.data.render { selectedPlugin ->
            if (selectedPlugin.isNotBlank()) {
                overlayLarge {
                    editConfigurationStore.data.render { existing ->
                        val configNameStore = storeOf(existing?.name ?: selectedPlugin)

                        div("flex flex-col items-left gap-y-2 w-5/6 items-center m-auto") {
                            if (existing == null) {
                                h1 { +"New search configuration for $selectedPlugin" }
                            } else {
                                h1 { +"Edit ${existing.name}" }
                            }
                            textField(
                                placeHolder = selectedPlugin, "Name", "A descriptive name for your configuration"
                            ) {
                                value(configNameStore)
                            }
                            // plugin settings
                            if (selectedPlugin == "javascript") {
                                jsPluginEditor(
                                    selectedPluginStore = selectedPluginTypeStore,
                                    configNameStore = configNameStore,
                                    editConfigurationStore = editConfigurationStore
                                )
                            } else {
                                BuiltinPlugins.entries.firstOrNull { it.name == selectedPlugin }?.let { plugin ->
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
        }
    }
}

fun RenderContext.metricsEditor(
    showMetricsEditor: Store<Boolean>, metricConfigurationsStore: Store<List<MetricConfiguration>>,editMetricStore: Store<MetricConfiguration?>
) {
    val showMetricsPickerStore = storeOf(false)
    val newMetricTypeStore = storeOf<Metric?>(null)
    showMetricsEditor.data.render { show ->
        if (show) {
            overlayLarge {

                metricConfigurationsStore.data.render { mcs ->
                    h2 { +"Metric Configuration" }
                    mcs.forEach { mc ->
                        flexRow {
                            flexCol {
                                div {
                                    +"${mc.name} (${mc.metric})"
                                }
                                div {
                                    +mc.params.joinToString(", ") { "${it.name} = ${it.value}" }
                                }
                            }
                            flexRow {
                                secondaryButton {
                                    +"Delete"
                                    clicks handledBy {
                                        confirm(job = job) {
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
                                val expectedStore = storeOf(mc.expected?.toString() ?: "0.75")
                                div {
                                    domNode.innerHTML = mc.metric.explanation
                                }
                                textField("", "Name") {
                                    value(nameStore)
                                }

                                textField("", "Failure Threshold Score") {
                                    value(expectedStore)
                                }

                                mc.params.forEach { p ->
                                    textField("", p.name) {
                                        value(paramMap[p.name]!!)
                                    }
                                }
                                flexRow {
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
                                                    mc.copy(
                                                        name = nameStore.current,
                                                        expected = expectedStore.current.toDoubleOrNull(),
                                                        params = newValues
                                                    )

                                                } else {
                                                    it
                                                }
                                            })
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
                                    h3("text-xl") {
                                        +"What metric type do you want to add?"
                                    }
                                    flexRow {
                                        ul("list-disc") {
                                            Metric.entries.forEach { metric ->
                                                li {
                                                    a {
                                                        +metric.name
                                                        clicks.map { metric } handledBy newMetricTypeStore.update
                                                    }
                                                    div {
                                                        domNode.innerHTML = metric.explanation
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    primaryButton {
                                        +"Cancel"
                                        clicks handledBy {
                                            showMetricsPickerStore.update(false)
                                        }
                                    }
                                } else {
                                    h2 { +"Create new $selectedMetric metric" }
                                    val metricNameStore = storeOf(selectedMetric.name)
                                    textField("", "Metric Name", "Pick a unique name") {
                                        value(metricNameStore)
                                    }
                                    flexRow {
                                        secondaryButton {
                                            +"Cancel"
                                            clicks handledBy {
                                                newMetricTypeStore.update(null)
                                                showMetricsPickerStore.update(false)
                                            }
                                        }

                                        metricNameStore.data.render { metricName ->

                                            primaryButton {
                                                +"OK"
                                                disabled(mcs.map { it.name }
                                                    .contains(metricName) || metricName.isBlank())
                                                clicks handledBy {
                                                    newMetricTypeStore.update(null)
                                                    val newConfig = MetricConfiguration(
                                                        metric = selectedMetric,
                                                        name = metricName,
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
                            }
                        } else {
                            editMetricStore.data.render { currentMetric ->
                                if (currentMetric == null) {
                                    flexRow {
                                        secondaryButton {
                                            +"Close"
                                            clicks handledBy {
                                                showMetricsPickerStore.update(false)
                                                newMetricTypeStore.update(null)
                                                showMetricsEditor.update(false)
                                            }
                                        }
                                        jsonDownloadButton(
                                            mcs, "metric-configuration-${Clock.System.now()}.json", ListSerializer(MetricConfiguration.serializer()), buttonText = "Export Metrics Configuration")

                                        jsonFileImport(ListSerializer(MetricConfiguration.serializer()),"Import") { imported ->
                                            metricConfigurationsStore.update(imported)
                                        }
                                        secondaryButton {
                                            +"Reset"
                                            clicks handledBy {
                                                confirm("Are you sure","Replaces your current metrics with some sensible defaults",job=job)  {
                                                    metricConfigurationsStore.update(StandardConfigurations.defaults)
                                                }
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
            flexRow {
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

object StandardConfigurations {
    val defaults = listOf(
        MetricConfiguration(
            name = "Precision for top 3",
            metric = Metric.PrecisionAtK,
            params = listOf(
                MetricParam("k", 3.primitive),
                MetricParam("relevantRatingThreshold", 1.primitive),
            ),
            expected = 0.75
        ),
        MetricConfiguration(
            name = "Recall for top 3",
            metric = Metric.RecallAtK,
            params = listOf(
                MetricParam("k", 3.primitive),
                MetricParam("relevantRatingThreshold", 1.primitive),
            ),
            expected = 0.75
        ),
        MetricConfiguration(
            name = "Precision for top 10",
            metric = Metric.PrecisionAtK,
            params = listOf(
                MetricParam("k", 3.primitive),
                MetricParam("relevantRatingThreshold", 1.primitive),
            ),
            expected = 0.75
        ),
        MetricConfiguration(
            name = "Precision for top 10",
            metric = Metric.RecallAtK,
            params = listOf(
                MetricParam("k", 10.primitive),
                MetricParam("relevantRatingThreshold", 1.primitive),
            ),
            expected = 0.75
        ),
        MetricConfiguration(
            name = "Mean Reciprocal Rank",
            metric = Metric.MeanReciprocalRank,
            params = listOf(
                MetricParam("k", 10.primitive),
                MetricParam("relevantRatingThreshold", 1.primitive),
            ),
            expected = 0.75
        ),
        MetricConfiguration(
            name = "Expected Reciprocal Rank",
            metric = Metric.ExpectedReciprocalRank,
            params = listOf(
                MetricParam("k", 10.primitive),
                MetricParam("relevantRatingThreshold", 1.primitive),
            ),
            expected = 0.75
        ),
        MetricConfiguration(
            name = "Normalized Discounted Cumulative Gain",
            metric = Metric.NormalizedDiscountedCumulativeGain,
            params = listOf(
                MetricParam("k", 10.primitive),
                MetricParam("relevantRatingThreshold", 1.primitive),
            ),
            expected = 0.75
        ),
    )
}





