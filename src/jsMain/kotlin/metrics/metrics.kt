package metrics

import com.jilesvangurp.rankquest.core.DEFAULT_JSON
import com.jilesvangurp.rankquest.core.MetricResults
import com.jilesvangurp.rankquest.core.RatedSearch
import com.jilesvangurp.rankquest.core.SearchResultRating
import com.jilesvangurp.rankquest.core.pluginconfiguration.MetricConfiguration
import com.jilesvangurp.rankquest.core.pluginconfiguration.MetricsOutput
import com.jilesvangurp.rankquest.core.plugins.PluginFactoryRegistry
import components.*
import dev.fritz2.core.*
import koin
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import pageLink
import ratedsearches.RatedSearchesStore
import search.ActiveSearchPluginConfigurationStore

val metricsModule = module {
    singleOf(::MetricsOutputStore)
}

class MetricsOutputStore : RootStore<List<MetricsOutput>?>(null) {
    val ratedSearchesStore = koin.get<RatedSearchesStore>()
    val pluginFactoryRegistry = koin.get<PluginFactoryRegistry>()
    val activeSearchPluginConfigurationStore = koin.get<ActiveSearchPluginConfigurationStore>()

    val measure = handle {
        runWithBusy({
            ratedSearchesStore.current?.let { ratedSearches ->
                activeSearchPluginConfigurationStore.current?.let { config ->
                    pluginFactoryRegistry.get(config.pluginType)?.let { pf ->
                        val plugin = pf.create(config)
                        config.metrics.map { metricConfiguration ->
                            MetricsOutput(
                                config.name,metricConfiguration, metricConfiguration.metric.run(
                                    plugin,
                                    ratedSearches,
                                    metricConfiguration.params
                                )
                            )
                        }
                    }
                }
            } ?: listOf()
        }) {
            update(it)
        }
        it
    }
}

fun RenderContext.metrics() {
    val activeSearchPluginConfigurationStore = koin.get<ActiveSearchPluginConfigurationStore>()
    val ratedSearchesStore = koin.get<RatedSearchesStore>()
    val metricsOutputStore = koin.get<MetricsOutputStore>()

    val expandedState = storeOf(mapOf<String, Boolean>())
    activeSearchPluginConfigurationStore.data.render { searchPluginConfiguration ->
        if (searchPluginConfiguration == null) {
            para {
                +"Configure a search plugin first. "
                pageLink(Page.Conf)
            }
        } else {
            ratedSearchesStore.data.render { ratedSearches ->
                if (ratedSearches == null) {
                    p {
                        +"Rate some searches first. "
                        pageLink(Page.RatedSearches)
                    }

                }
                div("flex flex-row") {
                    primaryButton {
                        +"Run Metrics"
                        clicks handledBy metricsOutputStore.measure
                    }
                    jsonDownloadButton(
                        metricsOutputStore,
                        "${searchPluginConfiguration.name} metrics ${Clock.System.now()}.json",
                        ListSerializer(MetricsOutput.serializer())
                    )
                    val textStore = storeOf("")
                    textStore.data.render { text ->
                        primaryButton {
                            +"Import"
                            disabled(text.isBlank())
                            clicks handledBy {
                                val decoded = DEFAULT_JSON.decodeFromString<List<MetricsOutput>>(text)
                                console.log(decoded)
                                metricsOutputStore.update(decoded)
                            }
                        }
                    }
                    textFileInput(
                        fileType = ".json",
                        textStore = textStore
                    )
                }

                metricsOutputStore.data.render { metrics ->
                    div("w-full") {
                        metrics?.forEach { (_, metric, metricResult) ->
                            metricResult(expandedState, metric, metricResult)
                        }
                    }
                }
            }
        }
    }
}

private fun RenderContext.metricResult(
    expandedState: Store<Map<String, Boolean>>,
    metricConfiguration: MetricConfiguration,
    metricResult: MetricResults
) {
    val ratedSearchesStore = koin.get<RatedSearchesStore>()

    expandedState.data.render { em ->
        val expanded = em[metricConfiguration.name] == true

        ratedSearchesStore.data.render { ratedsearches ->

            val rss = ratedsearches?.associateBy { it.id }.orEmpty()

            div("flex flex-col mx-10 my-3 hover:bg-blueBright-50 p-3 rounded-lg border-2 border-blueBright-400") {
                h2 {
                    +metricConfiguration.name
                }
                div { +"Metric: ${+metricResult.metric}" }
                para {+"SearchConfiguration: ${metricConfiguration.name}"}
                div("flex flex-row w-full") {
                    iconButton(
                        svg = if (expanded) SvgIconSource.Minus else SvgIconSource.Plus,
                        title = if (expanded) "Collapse details" else "Expand details"
                    ) {
                        if (expanded) {
                            clicks.map {
                                val m = em.toMutableMap()
                                m.remove(metricConfiguration.name)
                                m
                            } handledBy expandedState.update
                        } else {
                            clicks.map {
                                val m = em.toMutableMap()
                                m[metricConfiguration.name] = true
                                m
                            } handledBy expandedState.update
                        }
                    }

                    div("mx-3 w-full") {
                        +"SearchContext: (${
                            metricConfiguration.params.map { "${it.name} = ${it.value}" }.joinToString(", ")
                        })"
                    }
                }
                if (expanded) {
                    metricResult.details.forEach { metricResult ->
                        div("w-full") {
                            +metricResult.id
                            +": "
                            +rss[metricResult.id]!!.searchContext.toString()
                        }
                        div {
                            +"${metricResult.metric}"
                        }
                        div("flex flex-row w-full hover:bg-blueBright-200") {
                            div("w-full flex flax-col") {
                                div("w-full") {
                                    h2 { +"Rated results" }
                                    div("ml-5 flex flex-row w-full bg-blueBright-200") {
                                        div("w-1/6") {
                                            +"Doc ID"
                                        }
                                        div("w-4/6") {
                                            +"Rating"
                                        }
                                        div("w-4/6") {
                                            +"Label"
                                        }
                                        div("w-1/6") {
                                            +"Metric"
                                        }
                                    }
                                    metricResult.hits.forEach { (doc, score) ->
                                        div("ml-5 flex flex-row w-full") {
                                            div("w-1/6") {
                                                +doc.docId
                                            }
                                            div("w-1/6") {
                                                +(rss[metricResult.id]?.ratings?.firstOrNull { it.documentId == doc.docId }?.rating?.toString() ?: "1")
                                            }

                                            div("w-4/6") {
                                                +(doc.label ?: "-")
                                            }
                                            div("w-1/6") {
                                                +score.toString()
                                            }
                                        }
                                    }
                                    if (metricResult.unRated.isNotEmpty()) {
                                        h2 { +"Unrated results" }
                                        metricResult.unRated.forEach { doc ->
                                            div("ml-5 flex flex-row w-full") {
                                                div("w-1/6") {
                                                    +doc.docId
                                                }
                                                div("w-4/6") {
                                                    +(doc.label ?: "-")
                                                }

                                                rss[metricResult.id]?.let { ratedSearch ->
                                                    if (ratedSearch.ratings.firstOrNull { it.documentId == doc.docId } == null) {
                                                        iconButton(SvgIconSource.Plus) {
                                                            clicks.mapNotNull {
                                                                ratedSearch.copy(
                                                                    ratings = ratedSearch.ratings + SearchResultRating(
                                                                        documentId = doc.docId,
                                                                        rating = 1,
                                                                        label = doc.label
                                                                    )
                                                                )

                                                            } handledBy ratedSearchesStore.addOrReplace
                                                        }
                                                    } else {
                                                        iconButton(SvgIconSource.Minus) {
                                                            clicks.mapNotNull {
                                                                ratedSearch.copy(
                                                                    ratings = ratedSearch.ratings.filter { it.documentId != doc.docId }
                                                                )

                                                            } handledBy ratedSearchesStore.addOrReplace
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
                }
            }
        }
    }
}

