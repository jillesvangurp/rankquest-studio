package searchpluginconfig

import com.jilesvangurp.rankquest.core.DEFAULT_JSON
import com.jilesvangurp.rankquest.core.DEFAULT_PRETTY_JSON
import com.jilesvangurp.rankquest.core.SearchResults
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
import metrics.explanation
import metrics.title
import kotlin.time.Duration.Companion.milliseconds

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
    centeredMainPanel {

        val editConfigurationStore = storeOf<SearchPluginConfiguration?>(null)
        createOrEditPlugin(editConfigurationStore)
        activeSearchPluginConfigurationStore.data.render { activePluginConfig ->
            if (activePluginConfig != null) {
                para {
                    +"Current configuration: "
                    strong {
                        +activePluginConfig.name
                    }
                }
            }

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
                        leftRightRow {
                            div {
                                +pluginConfig.name
                            }
                            div("place-items-end") {
                                row {

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
                            metricsEditor(showMetricsEditor, metricConfigurationsStore)

                        }
                    }
                    div("w-full place-items-end mt-10") {
                        leftRightRow {
                            if (showDemoContent) {
                                a {
                                    +"Hide Demo Content"
                                    clicks.map { false } handledBy showDemoContentStore.update
                                }
                                row {
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

private fun RenderContext.help() {
    infoPopup(
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
            
            You also need to do this if you want to use [rankquest-cli](https://github.com/jillesvangurp/rankquest-cli).
            
            ## Writing your own plugins
            
            Writing your own plugins is possible of course. However, this will require modifying some source code. 
            You can either modify this project or add it as a built in plugin in 
            [rankquest-core](https://github.com/jillesvangurp/rankquest-core). If you do, also modify this project
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

        leftRightRow {
            row {
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
                                    row {
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
                                    row {
                                        secondaryButton {
                                            +"Cancel"
                                            clicks handledBy {
                                                newMetricTypeStore.update(null)
                                                showMetricsPickerStore.update(false)
                                            }
                                        }

                                        primaryButton {
                                            +"OK"
                                            disabled(mcs.map { it.name }
                                                .contains(metricNameStore.current) || metricNameStore.current.isBlank())
                                            clicks handledBy {
                                                showMetricsPickerStore.update(false)
                                                newMetricTypeStore.update(null)
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
                                            +"Close"
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
                    }
                }
            }
        }
    }
}





